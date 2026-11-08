package com.duckcraftian.upf.ps1.audio;

import org.tinylog.Logger;

public class SpuReverb {


    public static SpuRecord[] PRESETS = new SpuRecord[10];
    public static final int[] FIR = {-1, 0, 2, 0, -10, 0, 35, 0, -103, 0, 266, 0, -616, 0, 1332, 0, -2960, 0, 10246, 16384, 10246, 0, -2960, 0, 1332, 0, -616, 0, 266, 0, -103, 0, 35, 0, -10, 0, 2, 0, -1};

    static {
        PRESETS[4] = new SpuRecord(14320, new int[]{
                (short) 0x00E3, (short) 0x00A9, (short) 0x6F60, (short) 0x4FA8, (short) 0xBCE0, (short) 0x4510, (short) 0xBEF0, (short) 0xA680,
                (short) 0x5680, (short) 0x52C0, (short) 0x0DFB, (short) 0x0B58, (short) 0x0D09, (short) 0x0A3C, (short) 0x0BD9, (short) 0x0973,
                (short) 0x0B59, (short) 0x08DA, (short) 0x08D9, (short) 0x05E9, (short) 0x07EC, (short) 0x04B0, (short) 0x06EF, (short) 0x03D2,
                (short) 0x05EA, (short) 0x031D, (short) 0x031C, (short) 0x0238, (short) 0x0154, (short) 0x00AA, (short) 0x8000, (short) 0x8000
        });

        PRESETS[5] = new SpuRecord(22256, new int[]{
                (short) 0x01A5, (short) 0x0139, (short) 0x6000, (short) 0x5000, (short) 0x4C00, (short) 0xB800, (short) 0xBC00, (short) 0xC000,
                (short) 0x6000, (short) 0x5C00, (short) 0x15BA, (short) 0x11BB, (short) 0x14C2, (short) 0x10BD, (short) 0x11BC, (short) 0x0DC1,
                (short) 0x11C0, (short) 0x0DC3, (short) 0x0DC0, (short) 0x09C1, (short) 0x0BC4, (short) 0x07C1, (short) 0x0A00, (short) 0x06CD,
                (short) 0x09C2, (short) 0x05C1, (short) 0x05C0, (short) 0x041A, (short) 0x0274, (short) 0x013A, (short) 0x8000, (short) 0x8000
        });

    }

    public static final int dAPF1 = 0;
    public static final int dAPF2 = 1;
    public static final int vIIR = 2;
    public static final int vCOMB1 = 3;
    public static final int vCOMB2 = 4;
    public static final int vCOMB3 = 5;
    public static final int vCOMB4 = 6;
    public static final int vWALL = 7;
    public static final int vAPF1 = 8;
    public static final int vAPF2 = 9;
    public static final int mLSAME = 10;
    public static final int mRSAME = 11;
    public static final int mLCOMB1 = 12;
    public static final int mRCOMB1 = 13;
    public static final int mLCOMB2 = 14;
    public static final int mRCOMB2 = 15;
    public static final int dLSAME = 16;
    public static final int dRSAME = 17;
    public static final int mLDIFF = 18;
    public static final int mRDIFF = 19;
    public static final int mLCOMB3 = 20;
    public static final int mRCOMB3 = 21;
    public static final int mLCOMB4 = 22;
    public static final int mRCOMB4 = 23;
    public static final int dLDIFF = 24;
    public static final int dRDIFF = 25;
    public static final int mLAPF1 = 26;
    public static final int mRAPF1 = 27;
    public static final int mLAPF2 = 28;
    public static final int mRAPF2 = 29;
    public static final int vLIN = 30;
    public static final int vRIN = 31;

    private int[] regs;
    private int mode;
    private int[] ram;
    private int size;
    private int cur;
    private int depthL;
    private int depthR;
    private int tick;
    private int pos;
    private int[] downL, downR, upL, upR;
    private int stepL, stepR;

    public SpuReverb() {
        this.mode = 0;
        this.tick = 0;
        this.pos = 0;
        this.downL = new int[64];
        this.downR = new int[64];
        this.upL = new int[64];
        this.upR = new int[64];

        this.stepL = 0;
        this.stepR = 0;
        this.setMode(4);
    }

    public void setMode(int mode) {
        if (mode < 0 || mode > 9) {
            Logger.debug("Unknown Reverb Mode");
            return;
        }

        SpuRecord preset = PRESETS[mode];
        if (preset == null) {
            Logger.debug("Unknown Reverb mode");
            return;
        }

        if (this.mode == mode) return;
        this.mode = mode;
        this.size = preset.size();
        this.regs = preset.regs();
        this.ram = new int[size];
        cur = 0;
        depthL = 0;
        depthR = 0;
    }

    public void setDepth(int left, int right) {
        depthL = left;
        depthR = right;
    }

    public void process(int frames, float[] inL, float[] inR, float[] outL, float[] outR) {
        for (int i = 0; i < frames; i++) {
            pos = (pos + 1) & 63;
            downL[pos] = toS16(inL[i]);
            downR[pos] = toS16(inR[i]);
            if (tick % 2 == 1) {
                int lin = mulv(sat16(firSum(downL, pos) >> 15), regs[vLIN]);
                int rin = mulv(sat16(firSum(downR, pos) >> 15), regs[vRIN]);
                step(lin, rin);
                upL[pos] = stepL;
                upR[pos] = stepR;
            } else {
                upL[pos] = 0;
                upR[pos] = 0;
            }

            tick = tick + 1;
            outL[i] = toFloat(sat16(firSum(upL, pos) >> 14));
            outR[i] = toFloat(sat16(firSum(upR, pos) >> 14));
        }
    }

    public void step(int lin, int rin) {
        int[] r = regs;

        int pl = ram[at(r[mLSAME], -1)];
        ram[at(r[mLSAME], 0)] = sat16(mulv(lin + mulv(ram[at(r[dLSAME], 0)], r[vWALL]) - pl, r[vIIR]) + pl);

        int pr = ram[at(r[mRSAME], -1)];
        ram[at(r[mRSAME], 0)] = sat16(mulv(rin + mulv(ram[at(r[dRSAME], 0)], r[vWALL]) - pr, r[vIIR]) + pr);

        int pdl = ram[at(r[mLDIFF], -1)];
        ram[at(r[mLDIFF], 0)] = sat16(mulv(lin + mulv(ram[at(r[dRDIFF], 0)], r[vWALL]) - pdl, r[vIIR]) + pdl);


        int pdr = ram[at(r[mRDIFF], -1)];
        ram[at(r[mRDIFF], 0)] = sat16(mulv(rin + mulv(ram[at(r[dLDIFF], 0)], r[vWALL]) - pdr, r[vIIR]) + pdr);

        long sumL = (long) r[vCOMB1] * ram[at(r[mLCOMB1], 0)]
                + (long) r[vCOMB2] * ram[at(r[mLCOMB2], 0)]
                + (long) r[vCOMB3] * ram[at(r[mLCOMB3], 0)]
                + (long) r[vCOMB4] * ram[at(r[mLCOMB4], 0)];

        int lout = sat16((int) (sumL >> 15));
        int a = ram[at(r[mLAPF1] - r[dAPF1], 0)];
        lout = sat16(lout - mulv(a, r[vAPF1]));
        ram[at(r[mLAPF1], 0)] = lout;

        lout = sat16(mulv(lout, r[vAPF1]) + a);
        a = ram[at(r[mLAPF2] - r[dAPF2], 0)];
        lout = sat16(lout - mulv(a, r[vAPF2]));
        ram[at(r[mLAPF2], 0)] = lout;
        lout = sat16(mulv(lout, r[vAPF2]) + a);

        long sumR = (long) r[vCOMB1] * ram[at(r[mRCOMB1], 0)]
                + (long) r[vCOMB2] * ram[at(r[mRCOMB2], 0)]
                + (long) r[vCOMB3] * ram[at(r[mRCOMB3], 0)]
                + (long) r[vCOMB4] * ram[at(r[mRCOMB4], 0)];

        int rout = sat16((int) (sumR >> 15));
        a = ram[at(r[mRAPF1] - r[dAPF1], 0)];
        rout = sat16(rout - mulv(a, r[vAPF1]));
        ram[at(r[mRAPF1], 0)] = rout;

        rout = sat16(mulv(rout, r[vAPF1]) + a);
        a = ram[at(r[mRAPF2] - r[dAPF2], 0)];
        rout = sat16(rout - mulv(a, r[vAPF2]));
        ram[at(r[mRAPF2], 0)] = rout;
        rout = sat16(mulv(rout, r[vAPF2]) + a);

        stepL = sat16(mulv(lout, depthL));
        stepR = sat16(mulv(rout, depthR));
        cur = (cur + 1) % size;
    }


    public int sat16(int x) {
        return Math.clamp(x, -32768, 32767);
    }

    public int mulv(int a, int v) {
        return (a * v) >> 15;
    }

    public int toS16(float x) {
        return sat16((int) (x * 32768));
    }

    public float toFloat(int v) {
        return v / 32768.0f;
    }


    public int at(int reg, int off) {
        return Math.floorMod(reg * 4 + cur + off, size);
    }

    public int firSum(int[] ring, int pos) {
        int sum = 0;
        for (int i = 0; i < 39; i++) {
            sum += FIR[i] * ring[(pos - i) & 63];
        }

        return sum;
    }

}