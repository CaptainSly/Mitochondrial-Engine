package com.duckcraftian.upf.pe.audio;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.engine.audio.AudioOutput;
import com.duckcraftian.upf.pe.gfx.PECamera;
import com.duckcraftian.upf.serialization.UPDHSerializer;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.Path;

import static com.duckcraftian.upf.system.DataTypeHelper.*;

public class PEAudio {

    public static final int[] STEP_ANIM = {22, 22, 23, 23};
    public static final int[] STEP_FRAME = {0, 19, 0, 9};

    public Path imagesFile;
    public byte[] lengths, noteLengths, panTable;
    public byte[] bank0, lfo;

    public int[][] soundEvents;
    public byte[] roomContainer;

    public int dynCount, stepSlot;
    public PECamera camera;

    public AKAOAudioSource source;

    private int sfxCounter;

    public void init(Path file) throws IOException {
        imagesFile = file;
        lengths = UPDHSerializer.getStreamedData(imagesFile, "sound/akao/lengths");
        noteLengths = UPDHSerializer.getStreamedData(imagesFile, "sound/akao/noteLengths");
        panTable = UPDHSerializer.getStreamedData(imagesFile, "sound/akao/panTable");
        bank0 = UPDHSerializer.getStreamedData(imagesFile, "sound/bank/s000");
        lfo = UPDHSerializer.getStreamedData(imagesFile, "sound/akao/lfo");

        source = new AKAOAudioSource();
        UPF.AUDIO = new AudioOutput(source);
        UPF.AUDIO.start(Integer.MAX_VALUE);

        sfxCounter = 0;
    }

    public void loadRoomSfx(byte[] container) throws IOException {
        roomContainer = container;
        soundEvents = new int[20][6];
        dynCount = 0;

        for (int e = 0; e < 4; e++) {
            soundEvents[e][2] = STEP_ANIM[e];
            soundEvents[e][3] = STEP_FRAME[e];
        }

        AKAOSequence newSfx = new AKAOSequence(lengths, noteLengths, panTable, lfo);
        newSfx.initNested(container);
        newSfx.loadInstruments(bank0);

        long dirOffset = u32LE(container, 4);
        long streamsWord = u32LE(container, (int) (dirOffset + 0x24));
        long count = streamsWord >> 22;
        long tableOff = streamsWord & 0x3FFFFF;
        long samplesWord = u32LE(container, (int) (dirOffset + 0x2C));
        long sampleCount = samplesWord >> 22;
        long sampleTable = samplesWord & 0x3FFFFF;
        for (int i = 0; i < count; i++) {
            long rec = tableOff + 8 * i;
            int flags = u8(container, (int) (rec + 3));
            int bank = u16LE(container, (int) (rec + 4));
            int channel = u16LE(container, (int) (rec + 6));

            if ((flags & 0x20) != 0 && (flags & 0x10) == 0 && channel < 2) {
                byte[] bankBytes = UPDHSerializer.getStreamedData(imagesFile, bankFileName(bank));
                newSfx.loadInstruments(bankBytes, 128 + 16 * channel);
            } else if ((flags & 0x20) == 0 && (flags & 0x10) == 0) {

                byte[] bankBytes = UPDHSerializer.getStreamedData(imagesFile, bankFileName(bank));
                newSfx.loadInstruments(bankBytes);
            }
        }

        for (int i = 0; i < sampleCount; i++) {
            long record = sampleTable + 12 * i;
            long flags = u8(container, (int) (record + 3));
            long key = u16LE(container, (int) (record + 10));
            long slot = u32LE(container, (int) (record + 4)) >> 24;

            if ((flags & 0x20) != 0 && slot < 2) {
                if ((key & 1) == 1) {
                    soundEvents[1][(int) (4 + slot)] = (int) key;
                    soundEvents[3][(int) (4 + slot)] = (int) key;
                } else {
                    soundEvents[0][(int) (4 + slot)] = (int) key;
                    soundEvents[2][(int) (4 + slot)] = (int) key;
                }

            }

        }

        source.setSfx(newSfx);
    }

    public void addActorSound(int type, int sub, int anim, int frame, int id0, int id1) {
        if (dynCount >= 16) return;
        int[] ev = soundEvents[4 + dynCount];
        ev[0] = type & 0xFF;
        ev[1] = sub & 0xFF;
        ev[2] = anim & 0xFF;
        ev[3] = frame & 0xFF;
        ev[4] = id0 & 0xFFFF;
        ev[5] = id1 & 0xFFFF;
        dynCount = dynCount + 1;
    }

    public void setStepSlot() {
        stepSlot = 1;
    }

    public void newFrame() {
        stepSlot = 0;
    }

    public void actorSounds(boolean isPlayer, int type, int sub, int anim, int prev, int cur, int x, int y, int z) {
        for (int e = 0; e < 4 + dynCount; e++) {
            int[] ev = soundEvents[e];
            boolean match = false;
            if (e < 4)
                match = isPlayer && ev[2] == anim;
            else
                match = ev[0] == type && ev[1] == sub && ev[2] == anim;

            if (match && prev <= ev[3] && ev[3] < cur) {
                int id = ev[4 + stepSlot];
                if (id != 0) playSfxAt(id, x, y, z);
            }
        }
    }

    public int playSfxAt(int key, int x, int y, int z) {
        int[] pv = camera.soundPosition(x, y, z);
        return playSfx(roomContainer, key, 0, pv[0], pv[1]);
    }

    public int playSfx(byte[] container, int key, int tag, int pan, int volume) {
        long dirOffset = u32LE(container, 4);
        long word = u32LE(container, (int) (dirOffset + 0x2C));
        long count = word >> 22;
        long tableOff = word & 0x3FFFFF;
        long snippet = -1;
        for (int i = 0; i < count; i++) {
            long record = tableOff + 12 * i;
            if (u16LE(container, (int) (record + 10)) == key) {
                snippet = u32LE(container, (int) (record + 4)) & 0xFFFFFF;
                break;
            }
        }

        if (snippet < 0) return -1;
        long first = -1;
        long second = -1;
        int o0 = u16LE(container, (int) (snippet + 4));
        int o1 = u16LE(container, (int) (snippet + 6));
        if (o0 != 0xFFFF) first = snippet + 8 + o0;
        if (o1 != 0xFFFF) second = snippet + 8 + o1;

        int handle = 0x400 + sfxCounter;
        sfxCounter = (sfxCounter + 1) & 0x1FF;
        source.startSnippet((int) first, (int) second, pan, volume, tag & 0xFFFFFF, handle);
        return handle;
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
        AKAOSequence seq = new AKAOSequence(lengths, noteLengths, panTable, lfo);
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

    public void resetMusic() {
        source.stopCurrentMusic();
    }

    private String bankFileName(int bank) {
        String append = "";
        if (bank <= 9)
            append = "s00" + bank;
        else if (bank >= 10 && bank < 100)
            append = "s0" + bank;
        else if (bank >= 100)
            append = "s" + bank;

        return "sound/bank/" + append;
    }

    public void setCDVolume(int i) {
        source.setMovieVolume(i);
    }
}