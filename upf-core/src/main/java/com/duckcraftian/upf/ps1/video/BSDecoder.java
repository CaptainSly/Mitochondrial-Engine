package com.duckcraftian.upf.ps1.video;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.duckcraftian.upf.system.DataTypeHelper.u16LE;

public class BSDecoder {

    public static final int[] AC_LEN = {2, 2, 3, 3, 4, 5, 5, 4, 5, 7, 8, 9, 10, 11, 12};
    public static final int[] AC_PREFIX = {2, 3, 3, 2, 3, 5, 4, 1, 1, 1, 1, 1, 1, 1, 1};
    public static final int[] AC_IDX = {0, 0, 0, 1, 1, 0, 3, 2, 2, 3, 4, 4, 4, 4, 4};
    public static final int[][] AC_VALS = {{0xFE00}, {0x0001}, {0x0401},
            {0x0002, 0x0801}, {0x1001, 0x0C01},
            {0x0003},
            {0x3401, 0x0006, 0x3001, 0x2C01, 0x0C02, 0x0403, 0x0005, 0x2801}, {0x1C01, 0x1801, 0x0402, 0x1401},
            {0x0802, 0x2401, 0x0004, 0x2001},
            {0x4001, 0x1402, 0x0007, 0x0803, 0x0404, 0x3C01, 0x3801, 0x1002},
            {0x000B, 0x2002, 0x1003, 0x000A, 0x0804, 0x1C02, 0x5401, 0x5001, 0x0009, 0x4C01, 0x4801, 0x0405, 0x0C03, 0x0008, 0x1802, 0x4401},
            {0x2802, 0x2402, 0x1403, 0x0C04, 0x0805, 0x0407, 0x0406, 0x000F, 0x000E, 0x000D, 0x000C, 0x6801, 0x6401, 0x6001, 0x5C01, 0x5801},
            {0x001F, 0x001E, 0x001D, 0x001C, 0x001B, 0x001A, 0x0019, 0x0018, 0x0017, 0x0016, 0x0015, 0x0014, 0x0013, 0x0012, 0x0011, 0x0010},
            {0x0028, 0x0027, 0x0026, 0x0025, 0x0024, 0x0023, 0x0022, 0x0021, 0x0020, 0x040E, 0x040D, 0x040C, 0x040B, 0x040A, 0x0409, 0x0408},
            {0x0412, 0x0411, 0x0410, 0x040F, 0x1803, 0x4002, 0x3C02, 0x3802, 0x3402, 0x3002, 0x2C02, 0x7C01, 0x7801, 0x7401, 0x7001, 0x6C01},};

    public static int readAc(BitReader br) {
        if (br.peek(6) == 1) {
            br.read(6);
            return br.read(16);
        }
        for (int r = 0; r < 15; r++) {
            if (br.peek(AC_LEN[r]) == AC_PREFIX[r]) {
                br.read(AC_LEN[r]);
                int x = 0;
                if (AC_IDX[r] > 0) x = br.read(AC_IDX[r]);
                int v = AC_VALS[r][x];
                if (r > 0) {
                    int s = br.read(1);
                    if (s == 1) {
                        int level = v & 0x3FF;
                        level = (-level) & 0x3FF;
                        v = (v & 0xFC00) | level;
                    }
                }
                return v;
            }
        }

        throw new IllegalStateException("Bad bit stream: No Row Matched");
    }

    public static int readBlock(BitReader br, int quant, int[] out) {
        int dc = br.read(10);
        out[0] = (quant << 10) | dc;
        int k = 1;
        int v = 0;
        while (v != 0xFE00) {
            v = readAc(br);
            out[k] = v;
            k = k + 1;
        }

        return k;
    }

    public static List<int[]> decodeFrame(byte[] frame) {
        int quant = u16LE(frame, 4);
        BitReader bit = new BitReader();
        bit.newFrame(frame, 8);
        int[] out = new int[65];
        List<int[]> block = new ArrayList<>();

        while (bit.peek(10) != 0x1FF) {
            for (int i = 0; i < 6; i++) {
                int count = readBlock(bit, quant, out);
                block.add(Arrays.copyOfRange(out, 0, count));
            }
        }

        return block;
    }

}