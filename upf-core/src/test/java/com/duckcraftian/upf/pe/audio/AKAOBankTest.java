package com.duckcraftian.upf.pe.audio;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.engine.audio.AudioOutput;
import com.duckcraftian.upf.pe.disc.PERipper;
import com.duckcraftian.upf.ps1.audio.SpuVoice;
import com.duckcraftian.upf.ps1.audio.XaAdpcm;
import com.duckcraftian.upf.ps1.disc.DiscFileSystem;
import com.duckcraftian.upf.serialization.UPDHSerializer;
import com.duckcraftian.upf.system.Utils;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import static com.duckcraftian.upf.pe.audio.InstrumentTranslation.adsrWords;
import static com.duckcraftian.upf.pe.audio.InstrumentTranslation.pitchFor;
import static com.duckcraftian.upf.system.DataTypeHelper.u8;

class AKAOBankTest {

    public static Path path = Path.of("/mnt/secondary/DevEnv/ParasiteEve/disc_dump/pe/resident/unmapped_02224-08945.bin");
    private static Path jsonPath = Path.of("/mnt/secondary/DevEnv/ParasiteEve/testdata/pe/sound_archive.json");

    static {
        UPF.GSON = new GsonBuilder().setPrettyPrinting().create();
    }


    public class Row {
        public int sector_offset, sectors;
    }

    public class Archive {
        public Row[] items;
    }

    @Test
    void item0Test() throws IOException {
        byte[] all = Files.readAllBytes(path);
        byte[] item = Arrays.copyOfRange(all, 0, 22 * 2048);

        AKAOBank bank = new AKAOBank();
        List<List<Integer>> samples = bank.loadBank(item, true);
        Logger.debug("Num of Samples: " + samples.size());

    }

    @Test
    void allBanksTest() throws IOException {
        byte[] all = Files.readAllBytes(path);
        String text = Files.readString(jsonPath);
        Archive archive = UPF.GSON.fromJson(text, Archive.class);

        AKAOBank bank = new AKAOBank();
        int banks = 0, samplesTotal = 0;

        for (Row item : archive.items) {
            if (item.sectors == 0) continue;
            int from = item.sector_offset * 2048;
            int to = from + item.sectors * 2048;

            byte[] bytes = Arrays.copyOfRange(all, from, to);
            List<List<Integer>> samples = bank.loadBank(bytes, true);
            banks++;
            samplesTotal += samples.size();

        }

        Logger.debug("Banks: " + banks + ", samplesTotal: " + samplesTotal);
    }

    @Test
    void sequencerTest() throws IOException {
        Path imagesFiles = UPF.getEngineFolder("data").resolve("pe.udh");
        byte[] container = UPDHSerializer.getStreamedData(imagesFiles, "rooms/m010/container");
        byte[] lengths = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/lengths");
        byte[] noteLengths = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/noteLengths");
        byte[] panTable = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/panTable");
        byte[] lfo = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/lfo");

        byte[] bank0 = UPDHSerializer.getStreamedData(imagesFiles, "sound/bank/s000");
        byte[] bank25 = UPDHSerializer.getStreamedData(imagesFiles, "sound/bank/s025");

        AKAOSequence seq = new AKAOSequence(lengths, noteLengths, panTable, lfo);
        seq.loadInstruments(bank0);
        seq.loadInstruments(bank25);

        Logger.debug("Lengths size=" + lengths.length);

        int[] starts = seq.channelStarts(container, 0);
        seq.start(starts);

        int seconds = 8;
        int total = 44100 * seconds;
        float[] wavL = new float[total];
        float[] wavR = new float[total];

        float[] chunkL = new float[256];
        float[] chunkR = new float[256];

        int frame = 0;

        for (int t = 0; t < (240 * seconds); t++) {
            seq.tick();
            int due = ((t + 1) * 44100) / 240 - (t * 44100) / 240;
            seq.render(due, chunkL, chunkR);
            for (int k = 0; k < due; k++) {
                wavL[frame + k] = chunkL[k];
                wavR[frame + k] = chunkR[k];
            }

            frame = frame + due;
        }

        for (int i = 0; i < starts.length; i++) {
            Logger.debug("Sequencer: " + i + " events: " + seq.channels[i].events + ", pc=" + seq.channels[i].pc);
        }

        Logger.debug(seq.musicalTick + ", tempo: " + ((seq.tempo >> 16) & 0xFFFF));

        byte[] pcm = new byte[total * 4];

        for (int i = 0; i < total; i++) {
            int l = (int) (Math.clamp(wavL[i], -1.0, 1.0) * 32767);
            int r = (int) (Math.clamp(wavR[i], -1.0, 1.0) * 32767);
            pcm[4 * i] = (byte) (l & 0xFF);
            pcm[4 * i + 1] = (byte) ((l >> 8) & 0xFF);
            pcm[4 * i + 2] = (byte) (r & 0xFF);
            pcm[4 * i + 3] = (byte) ((r >> 8) & 0xFF);
        }

        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        ByteArrayInputStream bis = new ByteArrayInputStream(pcm);
        AudioInputStream ais = new AudioInputStream(bis, format, frame);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, Path.of("/mnt/secondary/DevEnv/ParasiteEve/.tmp/seq_m010_t0.wav").toFile());
        ais.close();
        bis.close();

        int stop = 0;
        for (int i = 0; i < starts.length; i++) {
            if (i < starts.length - 1) stop = starts[i + 1];
            else stop = seq.getPayload().length;

            seq.walkChannel(starts[i], stop);
        }
    }

    @Test
    void playTest() throws InterruptedException, IOException {
        Path imagesFiles = UPF.getEngineFolder("data").resolve("pe.udh");
        byte[] container = UPDHSerializer.getStreamedData(imagesFiles, "rooms/m010/container");
        byte[] lengths = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/lengths");
        byte[] noteLengths = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/noteLengths");
        byte[] panTable = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/panTable");
        byte[] lfo = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/lfo");

        byte[] bank0 = UPDHSerializer.getStreamedData(imagesFiles, "sound/bank/s000");
        byte[] bank25 = UPDHSerializer.getStreamedData(imagesFiles, "sound/bank/s025");

        AKAOSequence seq = new AKAOSequence(lengths, noteLengths, panTable, lfo);
        seq.loadInstruments(bank0);
        seq.loadInstruments(bank25);

        int[] starts = seq.channelStarts(container, 0);
        seq.start(starts);


        AKAOAudioSource source = new AKAOAudioSource(seq);
        AudioOutput output = new AudioOutput(source);
        output.start(480);
        output.waitUntilDone();

        Logger.debug("Musical Tick: " + seq.musicalTick + ", tempo: " + ((seq.tempo >> 16) & 0xFFFF) + " noteOnCount=" + seq.noteOnCount + ", keyOffCount=" + seq.keyOffCount);
    }

    @Test
    void musicCommandTest() throws IOException, InterruptedException {
        Path imagesFiles = UPF.getEngineFolder("data").resolve("pe.udh");
        byte[] container = UPDHSerializer.getStreamedData(imagesFiles, "rooms/m010/container");
        byte[] lengths = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/lengths");
        byte[] noteLengths = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/noteLengths");
        byte[] panTable = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/panTable");
        byte[] lfo = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/lfo");

        byte[] bank0 = UPDHSerializer.getStreamedData(imagesFiles, "sound/bank/s000");
        byte[] bank25 = UPDHSerializer.getStreamedData(imagesFiles, "sound/bank/s025");

        AKAOSequence seq = new AKAOSequence(lengths, noteLengths, panTable, lfo);
        seq.loadInstruments(bank0);
        seq.loadInstruments(bank25);

        int[] starts = seq.channelStarts(container, 0);
        seq.start(starts);

        AKAOAudioSource source = new AKAOAudioSource();
        AudioOutput output = new AudioOutput(source);
        output.start(1200);
        Thread.sleep(1000);
        source.play(5, seq);
        Thread.sleep(3000);
        source.setVolume(5, 50);
        Thread.sleep(3000);
        source.setVolume(5, 86);
        Thread.sleep(3000);
        source.stop(5);
        output.waitUntilDone();
    }

    @Test
    void drumTest() throws IOException {
        Path imagesFiles = UPF.getEngineFolder("data").resolve("pe.udh");
        byte[] container = UPDHSerializer.getStreamedData(imagesFiles, "rooms/m005/container");
        byte[] lengths = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/lengths");
        byte[] noteLengths = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/noteLengths");
        byte[] panTable = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/panTable");
        byte[] lfo = UPDHSerializer.getStreamedData(imagesFiles, "sound/akao/lfo");

        byte[] bank0 = UPDHSerializer.getStreamedData(imagesFiles, "sound/bank/s000");
        byte[] bank23 = UPDHSerializer.getStreamedData(imagesFiles, "sound/bank/s023");

        AKAOSequence seq = new AKAOSequence(lengths, noteLengths, panTable, lfo);
        seq.loadInstruments(bank0);
        seq.loadInstruments(bank23);

        int track = AKAOSequence.findTrack(container, 40);

        int[] starts = seq.channelStarts(container, track);
        seq.start(starts);

        float[] chunkL = new float[256];
        float[] chunkR = new float[256];

        for (int t = 0; t < 240 * 30; t++) {
            seq.tick();
            int due = ((t + 1) * 44100) / 240 - (t * 44100) / 240;
            seq.render(due, chunkL, chunkR);
        }
    }

}