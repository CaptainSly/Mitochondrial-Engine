package com.duckcraftian.upf.pe.script;

import java.util.Arrays;

import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;

public class Decoder {

    public static final boolean[] valid = new boolean[241];

    static {
        Arrays.fill(valid, true);

        int[] invalidCodeDeltas = new int[]{
                1, 8, 1, 17, 18, 1, 4, 2, 1, 1, 1, 25, 1, 1, 70, 1, 1, 1, 1, 1, 1
        };

        int invalidOpCode = 6;
        for (int invalidCodeDelta : invalidCodeDeltas) {
            valid[invalidOpCode] = false;
            invalidOpCode += invalidCodeDelta;
        }
    }

    public static int trace(byte[] container, int start, int end) {
        int pos = start;
        int words = 0;

        while (pos + 8 <= end) {
            int header = (int) u32LE(container, pos);
            int tail = (int) u32LE(container, pos + 4);
            int op = header & 0x1FFF;
            int argc = (header >> 13) & 15;

            if (op > 240 || !valid[op])
                break;

            if (op == 0 && argc == 0)
                break;

            if (pos + 8 + 4 * argc > end)
                break;

            int modes = header >> 17;
            for (int i = 0; i < argc; i++) {
                if (i == 5) modes = tail;
                modes = modes >> 3;
            }

            pos += 8 + 4 * argc;
            words += 2 + argc;
        }

        return words;
    }

}