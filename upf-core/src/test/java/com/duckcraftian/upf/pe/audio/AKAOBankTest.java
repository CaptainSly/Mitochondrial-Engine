package com.duckcraftian.upf.pe.audio;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.ps1.audio.SpuVoice;
import com.duckcraftian.upf.ps1.audio.XaAdpcm;
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
    void bank0WavTest() throws IOException {
        byte[] all = Files.readAllBytes(path);
        byte[] item = Arrays.copyOfRange(all, 0, 22 * 2048);

        AKAOBank bank = new AKAOBank();
        List<List<Integer>> samples = bank.loadBank(item, false);

        int rate = 44100;

        int k = 0;
        int r = 0x40 + 0x40 * k;
        int[] words = adsrWords(item, r);
        int pitch = pitchFor(item, r, 72, 0);
        SpuVoice v = new SpuVoice(Utils.intListToArray(samples.get(k)), bank.getLoopStart().get(k), bank.getLoops().get(k), words[0], words[1], pitch);

        int total = 22050;
        byte[] wav = new byte[total * 2];
        float[] bigL = new float[total];
        float[] bigR = new float[total];
        float[] oneL = new float[1];
        float[] oneR = new float[1];

        v.keyOn();

        for (int i = 0; i < 22050; i++) {
            if (i == 11025) v.keyOff();

            v.render(1, oneL, oneR);
            bigL[i] = oneL[0];
            bigR[i] = oneR[0];

            if (i == 0 || i == 2 || i == 40 || i == 11024 || i == 11100 || i == 11200)
                Logger.debug("I=" + i + " phase=" + v.getPhase() + " v.level=" + v.getLevel());
        }


        for (int i = 0; i < total; i++) {
            int s = (int) (bigL[i] * 32767);
            wav[2 * i] = (byte) (s & 0xFF);
            wav[2 * i + 1] = (byte) ((s >> 8) & 0xFF);
        }

        AudioFormat format = new AudioFormat(rate, 16, 1, true, false);
        ByteArrayInputStream bis = new ByteArrayInputStream(wav);
        AudioInputStream ais = new AudioInputStream(bis, format, total);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, Path.of("/mnt/secondary/DevEnv/ParasiteEve/.tmp/bank0test.wav").toFile());

        ais.close();
        bis.close();
    }

    @Test
    void xaTest() throws IOException {
        byte[] all = Files.readAllBytes(Path.of("/mnt/secondary/DevEnv/ParasiteEve/disc_dump/disc1/FMV1/FMV001.STR"));

        XaAdpcm xa = new XaAdpcm();
        List<Integer> left = new ArrayList<>();
        List<Integer> right = new ArrayList<>();

        int audioSectors = 0;
        for (int s = 0; s < all.length / 2352; s++) {
            int off = s * 2352;
            if ((u8(all, off + 18) & 0x04) != 0) {
                xa.decodeSector(all, off, left, right);
                audioSectors++;
            }
        }

        Logger.debug("Audio Sectors: " + audioSectors + ", Left Size=" + left.size() + ", Right Size=" + right.size());

        int frames = left.size();
        byte[] wav = new byte[frames * 4];

        for (int i = 0; i < frames; i++) {
            int l = left.get(i);
            int r = right.get(i);
            wav[4 * i] = (byte) (l & 0xFF);
            wav[4 * i + 1] = (byte) ((l >> 8) & 0xFF);
            wav[4 * i + 2] = (byte) (r & 0xFF);
            wav[4 * i + 3] = (byte) ((r >> 8) & 0xFF);
        }

        AudioFormat format = new AudioFormat(37800, 16, 2, true, false);
        ByteArrayInputStream bis = new ByteArrayInputStream(wav);
        AudioInputStream ais = new AudioInputStream(bis, format, frames);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, Path.of("/mnt/secondary/DevEnv/ParasiteEve/.tmp/jingletest1.wav").toFile());
        ais.close();
        bis.close();

    }


}