package com.duckcraftian.upf.ps1.audio;

import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;
import static com.duckcraftian.upf.system.DataTypeHelper.u8;

public class SpuVoice {

    public static final int OFF = 0;
    public static final int ATTACK = 1;
    public static final int DECAY = 2;
    public static final int SUSTAIN = 3;
    public static final int RELEASE = 4;

    private int[] pcm;
    private int loopStart;
    private boolean loops;
    private int adsr1, adsr2, pitch;
    private int pos, frac;
    private int level, envCounter;
    private int phase;
    private float volL, volR;

    public SpuVoice(int[] pcm, int loopStart, boolean loops, int adsr1, int adsr2, int pitch) {
        this.pcm = pcm;
        this.loopStart = loopStart;
        this.loops = loops;
        this.adsr1 = adsr1;
        this.adsr2 = adsr2;
        this.pitch = pitch;
        volL = 1.0f;
        volR = 1.0f;
    }

    public void keyOn() {
        pos = 0;
        frac = 0;
        level = 0;
        envCounter = 0;
        phase = ATTACK;
    }

    public void keyOff() {
        if (phase != OFF)
            phase = RELEASE;

        envCounter = 0;
    }

    public void setVolume(float left, float right) {
        volL = left;
        volR = right;
    }

    public void render(int frames, float[] outL, float[] outR) {
        for (int i = 0; i < frames; i++) {
            if (phase == OFF) {
                outL[i] = 0;
                outR[i] = 0;
            } else {
                float s = pcm[pos] / 32768.0f * level / 0x7FFF;
                outL[i] = s * volL;
                outR[i] = s * volR;
                envTick();
                frac += Math.min(pitch, 0x4000);
                pos += (frac >> 12);
                frac &= 0xFFF;
                if (pos >= pcm.length)
                    if (loops) {
                        pos = loopStart;
                    } else {
                        phase = OFF;
                        level = 0;
                    }
            }
        }
    }

    public void envTick() {
        int shift = 0;
        int step = 0;
        boolean exp = false;
        boolean dec = false;

        if (phase == ATTACK) {
            shift = (adsr1 >> 10) & 0x1F;
            step = (adsr1 >> 8) & 3;
            exp = ((adsr1 >> 15) & 1) != 0;
            dec = false;
        } else if (phase == DECAY) {
            shift = (adsr1 >> 4) & 0xF;
            step = 0;
            exp = true;
            dec = true;
        } else if (phase == SUSTAIN) {
            shift = (adsr2 >> 8) & 0x1F;
            step = (adsr2 >> 6) & 3;
            exp = ((adsr2 >> 15) & 1) != 0;
            dec = ((adsr2 >> 14) & 1) != 0;
        } else {
            shift = adsr2 & 0x1F;
            step = 0;
            exp = ((adsr2 >> 5) & 1) != 0;
            dec = true;
        }

        int s = 7 - step;
        if (dec)
            s = ~s;

        s = s << Math.max(0, 11 - shift);
        int inc = 0x8000 >> Math.max(0, shift - 11);
        if (exp && !dec && level > 0x6000) {
            if (shift < 10) s = s >> 2;
            else if (shift >= 11) inc = inc >> 2;
            else {
                s = s >> 1;
                inc = inc >> 1;
            }
        } else if (exp && dec) s = (s * level) >> 15;

        if (!(shift == 0x1F && step == 3))
            inc = Math.max(inc, 1);

        envCounter += inc;
        if ((envCounter & 0x8000) == 0) return;
        envCounter = 0;

        level += s;
        if (dec) level = Math.max(level, 0);
        else level = Math.min(level, 0x7FFF);

        if (phase == ATTACK && level >= 0x7FFF) phase = DECAY;
        else if (phase == DECAY && level <= ((adsr1 & 0xF) + 1) * 0x800)
            phase = SUSTAIN;
        else if (phase == RELEASE && level == 0) phase = OFF;
    }

    public int[] getPcm() {
        return pcm;
    }

    public int getLoopStart() {
        return loopStart;
    }

    public boolean isLoops() {
        return loops;
    }

    public int getAdsr1() {
        return adsr1;
    }

    public int getAdsr2() {
        return adsr2;
    }

    public int getPitch() {
        return pitch;
    }

    public int getPos() {
        return pos;
    }

    public int getFrac() {
        return frac;
    }

    public int getLevel() {
        return level;
    }

    public int getEnvCounter() {
        return envCounter;
    }

    public int getPhase() {
        return phase;
    }

    public float getVolL() {
        return volL;
    }

    public float getVolR() {
        return volR;
    }
}