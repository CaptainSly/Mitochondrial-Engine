package com.duckcraftian.upf.ps1.video;

import static com.duckcraftian.upf.system.DataTypeHelper.u16LE;

public class BitReader {

    private byte[] data;
    private int base;
    private int pos;

    public void newFrame(byte[] frame, int start) {
        data = frame;
        base = start;
        pos = 0;
    }

    public int peek(int n) {
        int v = 0;
        for (int i = 0; i < n; i++) {
            int p = pos + i;
            int at = base + (p / 16) * 2;
            int bit = 0;
            if (at + 1 >= data.length)
                bit = 0;
            else {
                int hw = u16LE(data, at);
                bit = (hw >> (15 - p % 16)) & 1;
            }

            v = (v << 1) | bit;
        }

        return v;
    }

    public int read(int n) {
        int v = peek(n);
        pos = pos + n;
        return v;
    }

}