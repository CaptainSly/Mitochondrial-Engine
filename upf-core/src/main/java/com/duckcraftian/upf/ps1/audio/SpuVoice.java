package com.duckcraftian.upf.ps1.audio;

import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;
import static com.duckcraftian.upf.system.DataTypeHelper.u8;

public class SpuVoice {

    public static final int OFF = 0;
    public static final int ATTACK = 1;
    public static final int DECAY = 2;
    public static final int SUSTAIN = 3;
    public static final int RELEASE = 4;

    public static final int[] GAUSS = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 3, 3,
            3, 4, 4, 5, 5, 6, 7, 7, 8, 9, 9, 10, 11, 12, 13, 14,
            15, 16, 17, 18, 19, 21, 22, 24, 25, 27, 28, 30, 32, 33, 35, 37,
            39, 41, 44, 46, 48, 51, 53, 56, 58, 61, 64, 67, 70, 73, 77, 80,
            84, 87, 91, 95, 99, 103, 107, 111, 116, 120, 125, 130, 135, 140, 145, 150,
            156, 161, 167, 173, 179, 186, 192, 199, 205, 212, 219, 227, 234, 242, 250, 257,
            266, 274, 283, 291, 300, 309, 319, 328, 338, 348, 358, 369, 379, 390, 401, 412,
            424, 436, 448, 460, 473, 485, 498, 512, 525, 539, 553, 567, 582, 597, 612, 627,
            643, 659, 675, 692, 708, 726, 743, 761, 779, 797, 816, 835, 854, 874, 894, 914,
            935, 956, 977, 999, 1020, 1043, 1066, 1089, 1112, 1136, 1160, 1184, 1209, 1234, 1260, 1286,
            1312, 1339, 1366, 1394, 1422, 1450, 1479, 1508, 1537, 1567, 1598, 1628, 1660, 1691, 1723, 1756,
            1789, 1822, 1856, 1890, 1924, 1959, 1995, 2031, 2067, 2104, 2141, 2179, 2217, 2256, 2295, 2334,
            2374, 2415, 2456, 2497, 2539, 2582, 2624, 2668, 2712, 2756, 2801, 2846, 2892, 2938, 2985, 3032,
            3079, 3128, 3176, 3225, 3275, 3325, 3376, 3427, 3479, 3531, 3584, 3637, 3691, 3745, 3799, 3855,
            3910, 3967, 4023, 4081, 4138, 4197, 4255, 4315, 4374, 4435, 4495, 4557, 4619, 4681, 4744, 4807,
            4871, 4935, 5000, 5065, 5131, 5197, 5264, 5332, 5399, 5468, 5536, 5606, 5676, 5746, 5817, 5888,
            5959, 6032, 6104, 6177, 6251, 6325, 6400, 6475, 6550, 6626, 6702, 6779, 6856, 6934, 7012, 7091,
            7170, 7249, 7329, 7409, 7490, 7571, 7653, 7735, 7817, 7900, 7983, 8066, 8150, 8234, 8319, 8404,
            8489, 8575, 8661, 8748, 8834, 8922, 9009, 9097, 9185, 9273, 9362, 9451, 9541, 9630, 9720, 9811,
            9901, 9992, 10083, 10174, 10266, 10358, 10450, 10542, 10635, 10727, 10820, 10913, 11007, 11100, 11194, 11288,
            11382, 11476, 11571, 11665, 11760, 11855, 11950, 12045, 12140, 12236, 12331, 12427, 12522, 12618, 12714, 12809,
            12905, 13001, 13097, 13193, 13289, 13385, 13481, 13577, 13673, 13769, 13865, 13961, 14056, 14152, 14248, 14343,
            14439, 14534, 14630, 14725, 14820, 14915, 15010, 15104, 15199, 15293, 15387, 15481, 15575, 15669, 15762, 15855,
            15948, 16041, 16133, 16226, 16317, 16409, 16500, 16592, 16682, 16773, 16863, 16953, 17042, 17131, 17220, 17308,
            17396, 17484, 17571, 17658, 17744, 17830, 17916, 18001, 18086, 18170, 18254, 18337, 18420, 18502, 18584, 18665,
            18746, 18826, 18905, 18985, 19063, 19141, 19219, 19295, 19372, 19447, 19522, 19597, 19671, 19744, 19816, 19888,
            19959, 20030, 20100, 20169, 20238, 20306, 20373, 20439, 20505, 20570, 20634, 20698, 20760, 20822, 20884, 20944,
            21004, 21063, 21121, 21178, 21235, 21290, 21345, 21399, 21452, 21505, 21556, 21607, 21657, 21706, 21754, 21801,
            21848, 21893, 21938, 21982, 22025, 22066, 22107, 22148, 22187, 22225, 22262, 22299, 22334, 22369, 22402, 22435,
            22467, 22498, 22527, 22556, 22584, 22611, 22637, 22662, 22686, 22709, 22731, 22752, 22772, 22791, 22809, 22826,
            22842, 22857, 22872, 22885, 22897, 22908, 22918, 22927, 22935, 22942, 22948, 22953, 22957, 22960, 22962, 22963
    };

    private int[] pcm;
    private int loopStart;
    private boolean loops;
    private int adsr1, adsr2, pitch;
    private int h1, h2, h3;
    private int pos, frac;
    private int level, envCounter;
    private int phase;
    private float volL, volR;
    private int[] outX;
    private boolean fm;
    private boolean reverb;

    public SpuVoice(int[] pcm, int loopStart, boolean loops, int adsr1, int adsr2, int pitch) {
        this.pcm = pcm;
        this.loopStart = loopStart;
        this.loops = loops;
        this.adsr1 = adsr1;
        this.adsr2 = adsr2;
        this.pitch = pitch;
        volL = 1.0f;
        volR = 1.0f;

        fm = false;
        outX = new int[256];

        reverb = false;
    }

    public void keyOn() {
        pos = 0;
        frac = 0;
        level = 0;
        envCounter = 0;
        phase = ATTACK;
        h1 = 0;
        h2 = 0;
        h3 = 0;
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

    public void render(int frames, float[] outL, float[] outR, int[] fmIn) {
        for (int i = 0; i < frames; i++) {
            if (phase == OFF) {
                outL[i] = 0;
                outR[i] = 0;
                outX[i] = 0;
            } else {
                int idx = (frac >> 4) & 0xFF;
                int mixed = ((GAUSS[0xFF - idx] * h3) >> 15)
                        + ((GAUSS[0x1FF - idx] * h2) >> 15)
                        + ((GAUSS[0x100 + idx] * h1) >> 15)
                        + ((GAUSS[idx] * pcm[pos]) >> 15);

                float s = mixed / 32768.0f * level / 0x7FFF;
                outL[i] = s * volL;
                outR[i] = s * volR;
                outX[i] = (mixed * level) >> 15;
                envTick();
                int step = pitch;
                if (fm && fmIn != null)
                    step = ((step * (fmIn[i] + 0x8000)) >> 15) & 0xFFFF;

                frac = frac + Math.min(step, 0x4000);
                int steps = frac >> 12;
                frac = frac & 0xFFF;
                for (int k = 1; k <= steps; k++) {
                    h3 = h2;
                    h2 = h1;
                    h1 = pcm[pos];
                    pos = pos + 1;
                    if (pos >= pcm.length)
                        if (loops) {
                            pos = loopStart;
                        } else {
                            phase = OFF;
                            level = 0;
                            break;
                        }

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

    public void setReverb(boolean reverb) {
        this.reverb = reverb;
    }

    public void setFm(boolean fm) {
        this.fm = fm;
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

    public boolean hasFm() {
        return fm;
    }

    public boolean hasReverb() {
        return reverb;
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

    public int[] getOutX() {
        return outX;
    }
}