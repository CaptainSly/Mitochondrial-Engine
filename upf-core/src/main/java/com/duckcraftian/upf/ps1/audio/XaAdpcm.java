package com.duckcraftian.upf.ps1.audio;

import java.util.List;

import static com.duckcraftian.upf.system.DataTypeHelper.u8;

public class XaAdpcm {

    public static final int[] F0 = {0, 60, 115, 98};
    public static final int[] F1 = {0, 0, -52, -55};

    public int[] old = {0, 0};
    public int[] older = {0, 0};

    public void decodeSector(byte[] all, int off, List<Integer> left, List<Integer> right) {
        int base = off + 24;

        for (int group = 0; group < 18; group++) {
            int g = base + group * 128;
            for (int block = 0; block < 4; block++) {
                decodeUnit(all, g, block, 0, left);
                decodeUnit(all, g, block, 1, right);
            }
        }
    }

    public void decodeUnit(byte[] all, int group, int block, int mode, List<Integer> output) {
        int hdr = u8(all, group + 4 + block * 2 + mode);
        int shift = 12 - (hdr & 0x0F);

        int filter = (hdr & 0x30) >> 4;
        for (int j = 0; j < 28; j++) {
            int b = u8(all, group + 16 + block + j * 4);
            int nib = (b >> (mode * 4)) & 0x0F;
            if (nib >= 8)
                nib -= 16;

            int s = nib * (1 << shift) + ((old[mode] * F0[filter] + older[mode] * F1[filter] + 32) >> 6);
            s = Math.clamp(s, -32768, 32767);
            output.add(s);
            older[mode] = old[mode];
            old[mode] = s;
        }
    }

}