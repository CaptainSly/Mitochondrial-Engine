package com.duckcraftian.upf.ps1.audio;

import java.util.ArrayList;
import java.util.List;

import static com.duckcraftian.upf.system.DataTypeHelper.u8;

public class SpuAdpcm {

    public static int[] F0 = {0, 60, 115, 98, 122};
    public static int[] F1 = {0, 0, -52, -55, -60};

    public static int[] decodeBlock(byte[] src, int off, int old, int older, List<Integer> out) {
        int hdr = u8(src, off);
        int filter = hdr >> 4;
        int shift = hdr & 0x0F;

        for (int i = 0; i < 28; i++) {
            int b = u8(src, off + 2 + i / 2);
            int nib = (i % 2) == 0 ? b & 0x0F : b >> 4;

            if (nib >= 8) nib -= 16;
            int s = nib * (1 << (12 - shift)) + (old * F0[filter] + older * F1[filter]) >> 6;

            if (s > 32767) s = 32767;
            if (s < -32768) s = -32768;
            out.add(s);
            older = old;
            old = s;
        }

        int[] result = {old, older};
        return result;
    }

    public static List<Integer> decodeSample(byte[] src, int off) {
        List<Integer> out = new ArrayList<>();
        int old = 0, older = 0;
        while (true) {
            int[] oldVal = decodeBlock(src, off, old, older, out);
            old = oldVal[0];
            older = oldVal[1];

            if ((u8(src, off + 1) & 1) != 0) break;

            off += 16;
        }

        return out;
    }

}