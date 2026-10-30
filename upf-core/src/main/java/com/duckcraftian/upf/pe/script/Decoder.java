package com.duckcraftian.upf.pe.script;

import org.tinylog.Logger;

import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;

public class Decoder {

    public static final boolean[] valid = new boolean[241];

    static {
        for (int i = 0; i < valid.length; i++)
            valid[i] = true;

        int[] invalidCodeDeltas = new int[]{
                1, 8, 1, 17, 18, 1, 4, 2, 1, 1, 1, 25, 1, 1, 70, 1, 1, 1, 1, 1, 1
        };

        int invalidOpCode = 6;
        for (int i = 0; i < invalidCodeDeltas.length; i++) {
            valid[invalidOpCode] = false;
            invalidOpCode += invalidCodeDeltas[i];
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

            StringBuilder sb = new StringBuilder("Pos " + pos + " op " + op);


            if (op > 240 || !valid[op])
                break;

            if (op == 0 && argc == 0)
                break;

            if (pos + 8 + 4 * argc > end)
                break;

            int modes = header >> 17;
            for (int i = 0; i < argc; i++) {
                if (i == 5) modes = tail;
                int mode = modes & 7;
                int value = (int) u32LE(container, pos + 8 + 4 * i);
                sb.append("  " + mode + ":" + value);
                modes = modes >> 3;
            }

            Logger.debug(sb.toString());
            pos += 8 + 4 * argc;
            words += 2 + argc;
        }

        return words;
    }

}