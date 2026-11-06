package com.duckcraftian.upf.pe.audio;

import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;
import static com.duckcraftian.upf.system.DataTypeHelper.u8;

public class InstrumentTranslation {

    public static int[] adsrWords(byte[] bank, int recordOffset) {
        int atk = u8(bank, recordOffset + 8);
        int dec = u8(bank, recordOffset + 9);
        int susL = u8(bank, recordOffset + 10);
        int susR = u8(bank, recordOffset + 11);
        int rel = u8(bank, recordOffset + 12);
        int atkM = u8(bank, recordOffset + 13);
        int susM = u8(bank, recordOffset + 14);
        int relM = u8(bank, recordOffset + 15);

        int adsr1 = ((atkM >> 2) << 15) | (atk << 8) | (dec << 4) | susL;
        int adsr2 = ((susM >> 1) << 14) | (susR << 6) | ((relM >> 2) << 5) | rel;

        return new int[]{adsr1, adsr2};
    }

    public static int pitchFor(byte[] bank, int recordOffset, int note, int detune) {
        int p = (int) u32LE(bank, recordOffset + 0x10 + (note % 12) * 4);
        int oct = note / 12;

        if (detune != 0)
            p += ((p * detune) >>> 7);

        if (oct >= 7) p = p << (oct - 6);
        else if (oct < 6) p = p >> (6 - oct);

        return p & 0xFFFF;
    }

}