package com.duckcraftian.upf.ps1.gfx;

import com.duckcraftian.upf.ps1.PSX;

public class GTE {

    public static final int DATA_COUNT = 32;
    public static final int CONTROL_COUNT = 32;

    private int[] data;
    private int[] control;

    private int flag;
    private int sf, lm;
    private long mac1, mac2, mac3;

    public static final int[] UNR = new int[0x101];

    static {
        for (int i = 0; i < 0x101; i++) {
            UNR[i] = Math.max(0, (0x40000 / (i + 0x100) + 1) / 2 - 0x101);
        }
    }

    public GTE() {
        data = new int[DATA_COUNT];
        control = new int[CONTROL_COUNT];
    }

    public void execute(int command) {
        flag = 0;
        int opcode = command & 0x3F;

        sf = (command >> 19) & 1;
        lm = (command >> 10) & 1;

        switch (opcode) {
            case PSX.OP_NCLIP:
                nclip();
                break;
            case PSX.OP_AVSZ3:
                avsz3();
                break;
            case PSX.OP_AVSZ4:
                avsz4();
                break;
            case PSX.OP_RTPS:
                rtp(0);
                break;
            case PSX.OP_RTPT:
                rtp(0);
                rtp(1);
                rtp(2);
                break;
        }

        if ((flag & 0x7F87E000) != 0)
            flag = flag | 0x80000000;

        control[31] = flag;
    }

    public void loadRotation(int[] r) {
        writeControl(0, (r[1] << 16) | (r[0] & 0xFFFF));
        writeControl(1, (r[3] << 16) | (r[2] & 0xFFFF));
        writeControl(2, (r[5] << 16) | (r[4] & 0xFFFF));
        writeControl(3, (r[7] << 16) | (r[6] & 0xFFFF));
        writeControl(4, r[8]);
    }

    public void loadTranslation(int[] t) {
        writeControl(5, t[0]);
        writeControl(6, t[1]);
        writeControl(7, t[2]);
    }

    public int readData(int slot) {
        if (slot == 31)
            return leadingBitCount(data[30]);

        if (slot == 28)
            return packColor();

        return data[slot];
    }

    public void writeData(int slot, int value) {
        if (slot == 1 || slot == 3 || slot == 5 || slot == 8 || slot == 9 || slot == 10 || slot == 11)
            data[slot] = (short) value;
        else
            data[slot] = value;
    }

    public int readControl(int slot) {
        if (slot == 4 || slot == 12 || slot == 20 || slot == 27 || slot == 29 || slot == 30)
            return (short) control[slot];

        return control[slot];
    }

    public void writeControl(int slot, int value) {
        control[slot] = value;
    }

    public void setFlag(int bit) {
        flag = flag | (1 << bit);
    }

    private void avsz3() {
        long zsf3 = (short) control[29];
        long sz1 = data[17] & 0xFFFF;
        long sz2 = data[18] & 0xFFFF;
        long sz3 = data[19] & 0xFFFF;
        averageZ(zsf3 * (sz1 + sz2 + sz3));
    }

    private void avsz4() {
        long zsf4 = (short) control[30];
        long sz0 = data[16] & 0xFFFF;
        long sz1 = data[17] & 0xFFFF;
        long sz2 = data[18] & 0xFFFF;
        long sz3 = data[19] & 0xFFFF;
        averageZ(zsf4 * (sz0 + sz1 + sz2 + sz3));
    }

    private void rtp(int index) {
        long vx = (short) data[2 * index];
        long vy = (short) (data[2 * index] >> 16);
        long vz = (short) data[2 * index + 1];

        long rt11 = (short) control[0];
        long rt12 = (short) (control[0] >> 16);
        long rt13 = (short) control[1];
        long rt21 = (short) (control[1] >> 16);
        long rt22 = (short) control[2];
        long rt23 = (short) (control[2] >> 16);
        long rt31 = (short) control[3];
        long rt32 = (short) (control[3] >> 16);
        long rt33 = (short) control[4];

        long trx = control[5];
        long _try = control[6];
        long trz = control[7];

        mac1 = macSum(1, trx, rt11 * vx, rt12 * vy, rt13 * vz);
        mac2 = macSum(2, _try, rt21 * vx, rt22 * vy, rt23 * vz);
        mac3 = macSum(3, trz, rt31 * vx, rt32 * vy, rt33 * vz);

        long low = (lm == 1) ? 0 : -0x8000;
        long ir1 = Math.clamp(mac1, low, 0x7FFF);
        if (ir1 != mac1)
            setFlag(24);

        long ir2 = Math.clamp(mac2, low, 0x7FFF);
        if (ir2 != mac2)
            setFlag(23);

        long check = (sf == 1) ? mac3 : (mac3 >> 12);
        if (check < -0x8000 || check > 0x7FFF)
            setFlag(22);

        long ir3 = Math.clamp(mac3, low, 0x7FFF);

        data[25] = (int) mac1;
        data[26] = (int) mac2;
        data[27] = (int) mac3;
        data[9] = (int) ir1;
        data[10] = (int) ir2;
        data[11] = (int) ir3;

        long sz3 = mac3 >> ((1 - sf) * 12);
        long sz3Clamped = Math.clamp(sz3, 0, 0xFFFF);
        if (sz3Clamped != sz3)
            setFlag(18);

        data[16] = data[17] & 0xFFFF;
        data[17] = data[18] & 0xFFFF;
        data[18] = data[19] & 0xFFFF;
        data[19] = (int) sz3Clamped;

        long h = control[26] & 0xFFFF;
        long n = divide(h, sz3Clamped);
        long ofx = control[24];
        long ofy = control[25];

        long macX = n * ir1 + ofx;
        checkMac0Overflow(macX);
        long sx = Math.clamp(macX >> 16, -0x400, 0x3FF);
        if (sx != (macX >> 16))
            setFlag(14);

        long macY = n * ir2 + ofy;
        checkMac0Overflow(macY);
        long sy = Math.clamp(macY >> 16, -0x400, 0x3FF);
        if (sy != (macY >> 16))
            setFlag(13);

        data[12] = data[13];
        data[13] = data[14];
        data[14] = (int) (((sy & 0xFFFF) << 16) | (sx & 0xFFFF));

        long dqa = (short) control[27];
        long dqb = control[28];
        long mac0 = n * dqa + dqb;
        setMac0(mac0);
        long ir0 = Math.clamp(mac0 >> 12, 0, 0x1000);
        if (ir0 != (mac0 >> 12))
            setFlag(12);

        data[8] = (int) ir0;
    }

    private long macSum(int which, long tr, long a, long b, long c) {
        long acc = tr << 12;
        acc += a;
        checkMacOverflow(acc, which);
        acc += b;
        checkMacOverflow(acc, which);
        acc += c;
        checkMacOverflow(acc, which);
        acc = (acc << 20) >> 20;
        return acc >> (sf * 12);
    }

    private void checkMacOverflow(long acc, int which) {
        if (acc > (1L << 43) - 1)
            setFlag(31 - which);

        if (acc < -(1L << 43))
            setFlag(28 - which);
    }

    private void setMac0(long value) {
        checkMac0Overflow(value);
        data[24] = (int) value;
    }

    private void checkMac0Overflow(long value) {
        if (value > 0x7FFFFFFF)
            setFlag(16);

        if (value < -0x80000000)
            setFlag(15);
    }

    private void averageZ(long mac0) {
        setMac0(mac0);
        long otz = mac0 >> 12;
        if (otz < 0) {
            otz = 0;
            setFlag(18);
        }

        if (otz > 0xFFFF) {
            otz = 0xFFFF;
            setFlag(18);
        }

        data[7] = (int) otz;
    }

    private void nclip() {
        long sx0 = (short) data[12];
        long sy0 = (short) (data[12] >> 16);
        long sx1 = (short) data[13];
        long sy1 = (short) (data[13] >> 16);
        long sx2 = (short) data[14];
        long sy2 = (short) (data[14] >> 16);

        long mac0 = sx0 * sy1 + sx1 * sy2 + sx2 * sy0 - sx0 * sy2 - sx1 * sy0 - sx2 * sy1;

        setMac0(mac0);
    }

    private int leadingBitCount(int x) {
        if (x < 0)
            x = ~x;

        return Integer.numberOfLeadingZeros(x);
    }

    private int packColor() {
        int r = Math.clamp(data[9] >> 7, 0, 31);
        int g = Math.clamp(data[10] >> 7, 0, 31);
        int b = Math.clamp(data[11] >> 7, 0, 31);

        return r | (g << 5) | (b << 10);
    }

    private long divide(long h, long sz3) {
        if (h >= sz3 * 2) {
            setFlag(17);
            return 0x1FFFF;
        }

        int z = Integer.numberOfLeadingZeros((int) sz3) - 16;
        long n = h << z;
        long d = sz3 << z;
        long u = UNR[(int) ((d - 0x7FC0) >> 7)] + 0x101;
        d = (0x2000080 - d * u) >> 8;
        d = (0x0000080 + d * u) >> 8;

        return Math.min(0x1FFFF, (n * d + 0x8000) >> 16);
    }

}