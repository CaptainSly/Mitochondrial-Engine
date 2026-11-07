package com.duckcraftian.upf.pe.audio;

import com.duckcraftian.upf.ps1.audio.SpuVoice;
import com.duckcraftian.upf.system.Utils;
import org.tinylog.Logger;

import java.util.Arrays;
import java.util.List;

import static com.duckcraftian.upf.pe.audio.InstrumentTranslation.adsrWords;
import static com.duckcraftian.upf.pe.audio.InstrumentTranslation.pitchFor;
import static com.duckcraftian.upf.ps1.audio.SpuVoice.OFF;
import static com.duckcraftian.upf.system.DataTypeHelper.*;

public class AKAOSequence {

    private byte[] payload;
    private byte[] lengths;
    private int[] noteLengths;
    AKAOChannel[] channels;
    AKAOInstrument[] instruments;
    SpuVoice[] voices;
    int[] panTable;
    int bankGain;
    float[] mixL;
    float[] mixR;
    int noteOnCount;
    int keyOffCount;
    int bankPitch;
    int bankSlideDuration;
    int bankSlideDelta;
    long tempo;
    private int tempoSlideDuration;
    private int tempoDelta;
    private int accumulator;
    int drumHitCount;
    int musicalTick;

    public AKAOSequence(byte[] lengths, byte[] noteLengths, byte[] panTable) {
        this.lengths = lengths;
        loadPanTable(panTable);
        loadNoteLengths(noteLengths);

        instruments = new AKAOInstrument[256];
        mixL = new float[256];
        mixR = new float[256];
        bankGain = 127;
        bankPitch = 127 << 16;
        bankSlideDuration = 0;
        bankSlideDelta = 0;
    }

    public void start(int[] starts) {
        channels = new AKAOChannel[starts.length];
        for (int i = 0; i < starts.length; i++) {
            channels[i] = new AKAOChannel();
            channels[i].pc = starts[i];
            channels[i].noteLength = 4;
            channels[i].volumeBase = 0x7F00;
            channels[i].gateLength = 2;
            channels[i].pan = 0x8000;
            channels[i].instrument = 0;
            channels[i].volumeBase = 0x7F00;
            channels[i].volume = 0x3FFF0000;
            channels[i].pan = 0x8000;
            channels[i].releaseRate = -1;
            channels[i].voice = -1;
        }

        voices = new SpuVoice[24];
        tempo = 0xFFFF0000L;
        tempoSlideDuration = 0;
        tempoDelta = 0;
        accumulator = 1;
        musicalTick = 0;
        drumHitCount = 0;
        noteOnCount = 0;
        keyOffCount = 0;
    }

    public void tick() {
        int acc = (int) ((tempo >> 16) & 0xFFFF);
        accumulator += acc;
        if ((accumulator & 0xFFFF0000L) != 0) {
            accumulator &= 0xFFFF;
            musicalTick++;
            for (int i = 0; i < channels.length; i++) {
                AKAOChannel ch = channels[i];
                if (!ch.ended) {
                    ch.noteLength--;
                    ch.gateLength--;
                    if (ch.noteLength == 0) step(i);
                    else if (ch.gateLength == 0) keyOff(ch);

                    updateChannel(ch);
                }
            }

            if (tempoSlideDuration != 0) {
                tempoSlideDuration = tempoSlideDuration - 1;
                tempo = (tempo + tempoDelta) & 0xFFFFFFFFL;
            }
        }
    }

    public void updateChannel(AKAOChannel ch) {
        if (ch.expressionDuration != 0) {
            ch.volume = ch.volume + ch.expressionDelta;
            ch.expressionDuration--;
        }

        applyVolume(ch);
    }

    public void setBankGain(int value) {
        bankPitch = (value & 0x7F) << 16;
        bankSlideDuration = 0;
        bankGain = (bankPitch >> 16) & 0x7F;
    }

    public void slideBankGain(int target, int duration) {
        int steps = duration;
        if (steps == 0) steps = 1;
        int goal = (target & 0x7F) << 16;
        bankSlideDelta = (goal - bankPitch) / steps;
        bankSlideDuration = steps;
    }

    public void slideBankGainFromTo(int start, int target, int duration) {
        setBankGain(start);
        slideBankGain(target, duration);
    }

    public void stepBankSlide() {
        if (bankSlideDuration != 0) {
            bankPitch = bankPitch + bankSlideDelta;
            bankSlideDuration = bankSlideDuration - 1;
            bankGain = (bankPitch >> 16) & 0x7F;
        }
    }

    public void applyVolume(AKAOChannel ch) {
        int hi = ch.volume >> 16;
        int channel = (hi * (ch.volumeBase >> 8)) >> 7;
        int base = (channel * bankGain) >> 7;
        int p = (ch.pan >> 8) & 0xFF;
        ch.volumeLeft = (base * panTable[p]) >> 15;
        ch.volumeRight = (base * panTable[p ^ 0xFF]) >> 15;
        if (ch.voice >= 0)
            voices[ch.voice].setVolume(ch.volumeLeft / 16384.0f, ch.volumeRight / 16384.0f);
    }

    public void render(int frames, float[] outL, float[] outR) {
        for (int i = 0; i < frames; i++) {
            outL[i] = 0.0f;
            outR[i] = 0.0f;
        }

        for (int v = 0; v < 24; v++) {
            if (voices[v] != null && voices[v].getPhase() != OFF) {
                voices[v].render(frames, mixL, mixR);
                for (int i = 0; i < frames; i++) {
                    outL[i] = outL[i] + mixL[i];
                    outR[i] = outR[i] + mixR[i];
                }
            }
        }

    }

    public void step(int idx) {
        AKAOChannel channel = channels[idx];
        int op = u8(payload, channel.pc);
        channel.pc++;
        while (op >= 0xA0 && op != 0xA0) {
            command(channel, op);
            op = u8(payload, channel.pc);
            channel.pc++;
        }

        if (op == 0xA0) {
            channel.ended = true;
            return;
        }

        if (channel.noteLength == 0) channel.noteLength = noteLengths[op % 11];
        channel.events++;
        channel.gateLength = channel.noteLength - 2;
        if (nextIsTie(channel)) channel.gateLength = channel.noteLength;
        if (op < 0x84) {
            if (channel.drumMode)
                drumHit(idx, channel.drumTable + 6 * (op / 11));
            else {
                int note = op / 11 + channel.octave * 12;
                if (channel.rangePending) selectByRange(channel, note);
                channel.currentNote = note;
                noteOn(idx, note + channel.transpose);
            }
        } else if (op >= 0x8F)
            keyOff(channel);
    }

    public void drumHit(int idx, int row) {
        AKAOChannel ch = channels[idx];
        int instrument = u8(payload, row);
        int note = u8(payload, row + 1);
        int rowVol = u8(payload, row + 2) | (u8(payload, row + 3) << 8);
        int pan = u8(payload, row + 4);
        if (ch.instrument != instrument) {
            ch.instrument = instrument;
            ch.attackOverride = -1;
            ch.sustainRateOverride = -1;
            ch.sustainModeOverride = -1;
            ch.releaseRate = -1;
        }
        ch.volume = (ch.drumVolume * rowVol) << 2;
        ch.pan = ((pan + 0x40) & 0xFF) << 8;
        keyOff(ch);
        noteOn(idx, note);
        drumHitCount = drumHitCount + 1;
        if (drumHitCount <= 3)
            Logger.debug("Musical Tick: " + musicalTick + ", " + idx + " " + ((row - ch.drumTable) / 6) + " " + instrument + " " + note + " " + ch.volumeLeft + " " + ch.volumeRight);
    }

    private boolean nextIsTie(AKAOChannel channel) {
        int p = channel.pc;
        while (true) {
            int b = u8(payload, p);
            if (b < 0xA0) return b >= 0x84 && b < 0x8F;
            if (b == 0xA0) return false;
            if (b == 0xFC) p = p + 1 + extLength(u8(payload, p + 1));
            else p = p + opLength(b);
        }
    }

    private void noteOn(int idx, int note) {
        AKAOChannel ch = channels[idx];
        AKAOInstrument instrument = instruments[ch.instrument];
        int[] words = adsrWords(instrument.bank(), instrument.recordOffset(), ch.attackOverride, ch.sustainRateOverride, ch.sustainModeOverride);
        int adsr1 = words[0];
        int adsr2 = words[1];
        if (ch.releaseRate >= 0)
            adsr2 = (adsr2 & ~0x1F) | (ch.releaseRate & 0x1F);

        int pitch = pitchFor(instrument.bank(), instrument.recordOffset(), note, 0) & 0x3FFF;
        int v = freeVoice();
        if (v < 0) return;

        voices[v] = new SpuVoice(instrument.pcm(), instrument.loopStart(), instrument.loops(), adsr1, adsr2, pitch);
        ch.voice = v;
        applyVolume(ch);
        voices[v].keyOn();
        noteOnCount++;
        if (idx == 0 || idx == 6)
            Logger.debug("musicalTick: " + musicalTick + " idx: " + idx + " instrument: " + ch.instrument + ", note: " + note + ", pitch: " + pitch + " adsr1: " + adsr1 + ", adsr2: " + adsr2 + ", volumeLeft: " + ch.volumeLeft + ", volumeRight: " + ch.volumeRight);

    }

    public void keyOff(AKAOChannel channel) {
        if (channel.voice >= 0) {
            voices[channel.voice].keyOff();
            channel.voice = -1;
            keyOffCount = keyOffCount + 1;
        }
    }

    public int freeVoice() {
        for (int v = 0; v < 24; v++)
            if (voices[v] == null || voices[v].getPhase() == OFF) return v;

        return -1;
    }

    public void command(AKAOChannel ch, int opCode) {
        if (opCode == 0xFC) {
            int sub = u8(payload, ch.pc);
            ch.pc++;

            if (sub == 0x00) {
                int b0 = u8(payload, ch.pc);
                int b1 = u8(payload, ch.pc + 1);
                tempo = (b0 << 16) | (b1 << 24);
                tempoSlideDuration = 0;
                ch.pc = ch.pc + 2;
            } else if (sub == 0x01) {
                tempoSlideDuration = u8(payload, ch.pc);
                if (tempoSlideDuration == 0) tempoSlideDuration = 256;
                int b0 = u8(payload, ch.pc + 1);
                int b1 = u8(payload, ch.pc + 2);
                long target = (b0 << 16) | (b1 << 24);
                long current = tempo & 0xFFFF0000L;
                tempoDelta = (int) ((target - current) / tempoSlideDuration);
                tempo = current;
                ch.pc = ch.pc + 3;
            } else if (sub == 0x04) {
                ch.drumTable = ch.pc + 2 + s16LE(payload, ch.pc);
                ch.drumMode = true;
                ch.pc = ch.pc + 2;
            } else if (sub == 0x05) {
                ch.drumMode = false;
            } else if (sub == 0x06) {
                ch.pc = ch.pc + 2 + s16LE(payload, ch.pc);
            } else if (sub == 0x14) {
                ch.rangeTable = ch.pc + 2 + s16LE(payload, ch.pc);
                ch.instrument = 0xFF;
                ch.currentNote = 0;
                ch.rangePending = true;
                ch.pc = ch.pc + 2;
            } else {
                ch.pc = ch.pc + extLength(sub) - 1;
            }
        } else if (opCode == 0xA1) {
            ch.instrument = u8(payload, ch.pc);
            ch.releaseRate = -1;
            ch.rangePending = false;
            ch.attackOverride = -1;
            ch.sustainRateOverride = -1;
            ch.sustainModeOverride = -1;
            ch.pc = ch.pc + 1;
        } else if (opCode == 0xA2) {
            ch.noteLength = u8(payload, ch.pc);
            ch.pc = ch.pc + 1;
        } else if (opCode == 0xA3) {
            ch.volumeBase = u8(payload, ch.pc) << 8;
            ch.pc = ch.pc + 1;
        } else if (opCode == 0xA5) {
            ch.octave = u8(payload, ch.pc);
            ch.pc = ch.pc + 1;
        } else if (opCode == 0xA6) ch.octave = (ch.octave + 1) & 0xF;
        else if (opCode == 0xA7) ch.octave = (ch.octave - 1) & 0xF;
        else if (opCode == 0xA8) {
            if (ch.drumMode) ch.drumVolume = u8(payload, ch.pc) << 7;
            else {
                ch.volume = s8(payload, ch.pc) << 23;
                ch.expressionDuration = 0;
            }

            ch.pc = ch.pc + 1;
        } else if (opCode == 0xA9) {
            int d = u8(payload, ch.pc);
            if (d == 0) d = 256;
            int target = s8(payload, ch.pc + 1) << 23;
            int current = ch.volume & 0xFFFF0000;
            ch.volume = current;
            ch.expressionDelta = (target - current) / d;
            ch.expressionDuration = d;
            ch.pc = ch.pc + 2;
        } else if (opCode == 0xAA) {
            ch.pan = ((u8(payload, ch.pc) + 0x40) & 0xFF) << 8;
            ch.pc = ch.pc + 1;
        } else if (opCode == 0xAD) {
            ch.attackOverride = u8(payload, ch.pc);
            ch.pc = ch.pc + 1;
        } else if (opCode == 0xB2) {
            ch.releaseRate = u8(payload, ch.pc);
            ch.pc++;
        } else if (opCode == 0xC8) {
            ch.loopTop = (ch.loopTop + 1) & 3;
            ch.loopPcs[ch.loopTop] = ch.pc;
            ch.loopCounts[ch.loopTop] = 0;
        } else if (opCode == 0xC9) {
            int n = u8(payload, ch.pc);
            ch.pc = ch.pc + 1;
            if (n == 0) n = 256;
            ch.loopCounts[ch.loopTop] = ch.loopCounts[ch.loopTop] + 1;
            if (ch.loopCounts[ch.loopTop] != n) ch.pc = ch.loopPcs[ch.loopTop];
            else ch.loopTop = (ch.loopTop - 1) & 3;
        } else
            ch.pc = ch.pc + opLength(opCode) - 1;

    }

    public void selectByRange(AKAOChannel ch, int note) {
        int t = ch.rangeTable;
        int count = 1;
        while (u8(payload, t + 8 * count) < 0x80)
            count = count + 1;

        int r = -1;
        int want = -1;
        boolean switchNow = false;

        if (ch.currentNote < note) {
            r = count - 1;
            for (int i = count - 1; i >= 0; i--)
                if (u8(payload, t + 8 * i + 2) >= note) r = i;

            want = u8(payload, t + 8 * r);
            int nextNote = u8(payload, t + 8 * (r + 1));
            switchNow = ch.instrument != want && ch.instrument != nextNote;
        } else if (note < ch.currentNote) {
            r = 0;
            for (int i = 0; i < count; i++)
                if (u8(payload, t + 8 * i + 1) <= note) r = i;

            want = u8(payload, t + 8 * r);
            int prevNote = -1;
            if (r > 0) prevNote = u8(payload, t + 8 * (r - 1));
            switchNow = ch.instrument != want && ch.instrument != prevNote;
        }

        if (switchNow) {
            int rec = t + 8 * r;
            ch.instrument = want;
            ch.attackOverride = u8(payload, rec + 3);
            ch.sustainRateOverride = u8(payload, rec + 4);
            ch.sustainModeOverride = u8(payload, rec + 5);
            ch.releaseRate = u8(payload, rec + 6);
        }
    }

    public int[] channelStarts(byte[] container, int track) {
        long dirOffset = u32LE(container, 4);
        long tracksWord = u32LE(container, (int) (dirOffset + 0x30));
        long count = tracksWord >> 22;
        long tableOff = tracksWord & 0x3FFFFF;

        long record = tableOff + 12 * track;
        long w0 = u32LE(container, (int) record);
        long w1 = u32LE(container, (int) record + 4);
        long w2 = u32LE(container, (int) record + 8);
        long size = w0 & 0xFFFFFF;

        long payloadOff = w1 & 0xFFFFFF;
        long key = w2 >> 16;
        long bank = w2 & 0xFFFF;

        payload = cut(container, (int) payloadOff, (int) size);

        long mask = u32LE(payload, 0x10) & 0xFFFFFF;
        int channels = countOneBits(mask);

        int[] starts = new int[channels];
        for (int i = 0; i < channels; i++) {
            int offset = u16LE(payload, 0x20 + 2 * i);
            starts[i] = 0x22 + 2 * i + offset;
        }

        Logger.debug("Size=" + size + ", payloadOffset=" + payloadOff + ", key=" + key + ", bank=" + bank + ", channels=" + channels + ", starts[0]=" + starts[0]);
        return starts;
    }

    public int walkChannel(int start, int stop) {
        int pc = start;
        int count = 0;

        while (pc < stop) {
            int step = 0;

            int op = u8(payload, pc);
            if (op < 0xA0)
                step = 1;
            else if (op == 0xFC) {
                int sub = u8(payload, pc + 1);
                step = 1 + extLength(sub);
            } else step = opLength(op);

            pc += step;
            count++;
        }

        Logger.debug("start=" + start + ", stop=" + stop + ", count=" + count + ", pc=" + pc);
        return pc;
    }

    public void loadNoteLengths(byte[] raw) {
        noteLengths = new int[11];
        for (int i = 0; i < noteLengths.length; i++)
            noteLengths[i] = u16LE(raw, 2 * i);
    }

    public void loadPanTable(byte[] table) {
        panTable = new int[256];
        for (int i = 0; i < panTable.length; i++) {
            panTable[i] = s16LE(table, 2 * i);
        }
    }

    public void loadInstruments(byte[] bankBytes) {
        long first = u32LE(bankBytes, 0x18);
        long last = u32LE(bankBytes, 0x1C);

        if (last == 0) last = 0x100;
        AKAOBank bank = new AKAOBank();
        List<List<Integer>> samples = bank.loadBank(bankBytes, false);

        for (int k = 0; k < (last - first); k++) {
            int recordOffset = 0x40 + 0x40 * k;
            int[] pcm = Utils.intListToArray(samples.get(k));
            int loopStart = bank.getLoopStart().get(k);
            Boolean loops = bank.getLoops().get(k);

            AKAOInstrument instrument = new AKAOInstrument(bankBytes, recordOffset, pcm, loopStart, loops);
            instruments[(int) (first + k)] = instrument;
        }
    }

    public void stopAll() {
        for (int i = 0; i < channels.length; i++) {
            channels[i].ended = true;
            keyOff(channels[i]);
        }
    }

    public static int findTrack(byte[] container, int key) {
        long dirOffset = u32LE(container, 4);
        long tracksWord = u32LE(container, (int) (dirOffset + 0x30));
        long count = tracksWord >> 22;
        long tableOffset = tracksWord & 0x3FFFFF;

        for (int i = 0; i < count; i++) {
            long w2 = u32LE(container, (int) (tableOffset + 12 * i + 8));
            if ((w2 >> 16) == key) return i;
        }

        return -1;
    }

    public static int trackBank(byte[] container, int track) {
        long dirOffset = u32LE(container, 4);
        long tracksWord = u32LE(container, (int) (dirOffset + 0x30));
        long tableOffset = tracksWord & 0x3FFFFF;
        long w2 = u32LE(container, (int) (tableOffset + 12 * track + 8));
        return (int) (w2 & 0xFFFF);
    }

    private int opLength(int op) {
        int n = u8(lengths, op - 0xA0);
        if (n != 0) return n;
        if (op == 0xC9) return 2;
        return 1;
    }


    private int extLength(int sub) {
        int n = u8(lengths, 96 + sub);
        if (n != 0) return n;
        if (sub == 0x06) return 3;
        if (sub == 0x07 || sub == 0x08 || sub == 0x09) return 4;
        if (sub == 0x12) return 3;
        if (sub == 0x17) return 2;
        if (sub == 0x18) return 3;
        return 1;
    }

    private byte[] cut(byte[] container, int payloadOffset, int size) {
        return Arrays.copyOfRange(container, payloadOffset, payloadOffset + size);
    }

    private int countOneBits(long value) {
        return Long.bitCount(value);
    }

    private int countOneBits(int value) {
        return Integer.bitCount(value);
    }

    public byte[] getPayload() {
        return payload;
    }

}