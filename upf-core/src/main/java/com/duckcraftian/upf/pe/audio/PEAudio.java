package com.duckcraftian.upf.pe.audio;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.engine.audio.AudioOutput;
import com.duckcraftian.upf.serialization.UPDHSerializer;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.Path;

public class PEAudio {

    public Path imagesFile;
    public byte[] lengths, noteLengths, panTable;
    public byte[] bank0;

    public AKAOAudioSource source;

    public void init(Path file) throws IOException {
        imagesFile = file;
        lengths = UPDHSerializer.getStreamedData(imagesFile, "sound/akao/lengths");
        noteLengths = UPDHSerializer.getStreamedData(imagesFile, "sound/akao/noteLengths");
        panTable = UPDHSerializer.getStreamedData(imagesFile, "sound/akao/panTable");
        bank0 = UPDHSerializer.getStreamedData(imagesFile, "sound/bank/s000");

        source = new AKAOAudioSource();
        UPF.AUDIO = new AudioOutput(source);
        UPF.AUDIO.start(Integer.MAX_VALUE);
    }

    public void startMusic(byte[] container, int key) throws IOException {
        int track = AKAOSequence.findTrack(container, key);
        if (track < 0) {
            Logger.error("No Music track for key: " + key);
            return;
        }

        int bankId = AKAOSequence.trackBank(container, track);
        String bankName = "sound/bank/";

        String append = "";
        if (bankId <= 9)
            append = "s00" + bankId;
        else if (bankId >= 10 && bankId < 100)
            append = "s0" + bankId;
        else if (bankId >= 100)
            append = "s" + bankId;

        bankName = bankName + append;
        byte[] bankBytes = UPDHSerializer.getStreamedData(imagesFile, bankName);
        AKAOSequence seq = new AKAOSequence(lengths, noteLengths, panTable);
        seq.loadInstruments(bank0);
        seq.loadInstruments(bankBytes);
        int[] starts = seq.channelStarts(container, track);
        seq.start(starts);
        source.play(key, seq);
    }

    public void slideMusicVolume(int key, int duration, int target) {
        source.slide(key, duration, target);
    }

    public void slideMusicVolumeFromTo(int key, int duration, int start, int target) {
        source.slideFromTo(key, duration, start, target);
    }

    public void stopMusic(int key) {
        source.stop(key);
    }

    public void setMusicVolume(int key, int value) {
        source.setVolume(key, value);
    }

}