package com.duckcraftian.upf.ps1.video;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class Mdec {

    public static final int[] IQ = {
            2, 16, 19, 22, 26, 27, 29, 34,
            16, 16, 22, 24, 27, 29, 34, 37,
            19, 22, 26, 27, 29, 34, 34, 38,
            22, 22, 26, 27, 29, 34, 37, 40,
            22, 26, 27, 29, 32, 35, 40, 48,
            26, 27, 29, 32, 35, 40, 48, 58,
            26, 27, 29, 34, 38, 46, 56, 69,
            27, 29, 35, 38, 46, 56, 69, 83
    };

    public static final int[] ZIGZAG = {
            0, 1, 5, 6, 14, 15, 27, 28,
            2, 4, 7, 13, 16, 26, 29, 42,
            3, 8, 12, 17, 25, 30, 41, 43,
            9, 11, 18, 24, 31, 40, 44, 53,
            10, 19, 23, 32, 39, 45, 52, 54,
            20, 22, 33, 38, 46, 51, 55, 60,
            21, 34, 37, 47, 50, 56, 59, 61,
            35, 36, 48, 49, 57, 58, 62, 63
    };

    public static final int[] SCALE = {
            0x5A82, 0x5A82, 0x5A82, 0x5A82, 0x5A82, 0x5A82, 0x5A82, 0x5A82,
            0x7D8A, 0x6A6D, 0x471C, 0x18F8, 0xE707, 0xB8E3, 0x9592, 0x8275,
            0x7641, 0x30FB, 0xCF04, 0x89BE, 0x89BE, 0xCF04, 0x30FB, 0x7641,
            0x6A6D, 0xE707, 0x8275, 0xB8E3, 0x471C, 0x7D8A, 0x18F8, 0x9592,
            0x5A82, 0xA57D, 0xA57D, 0x5A82, 0x5A82, 0xA57D, 0xA57D, 0x5A82,
            0x471C, 0x8275, 0x18F8, 0x6A6D, 0x9592, 0xE707, 0x7D8A, 0xB8E3,
            0x30FB, 0x89BE, 0x7641, 0xCF04, 0xCF04, 0x7641, 0x89BE, 0x30FB,
            0x18F8, 0xB8E3, 0x6A6D, 0x8275, 0x7D8A, 0x9592, 0x471C, 0xE707
    };

    public static final int[] XOFF = {0, 8, 0, 8};
    public static final int[] YOFF = {0, 0, 8, 8};

    public static final int[] ZAGZIG = new int[ZIGZAG.length];

    static {
        for (int i = 0; i < 64; i++) ZAGZIG[ZIGZAG[i]] = i;
        for (int i = 0; i < 64; i++) if (SCALE[i] >= 0x8000) SCALE[i] = SCALE[i] - 0x10000;
    }

    public static int[] rlDecode(int[] hw) {
        int[] coef = new int[64];
        int n = hw[0];
        int qscale = (n >> 10) & 0x3F;
        int k = 0;
        int val = s10(n) * IQ[0];
        int i = 1;

        while (true) {
            if (qscale == 0) val = s10(n) * 2;
            val = Math.clamp(val, -0x400, 0x3FF);
            if (qscale > 0) coef[ZAGZIG[k]] = val;
            else coef[k] = val;
            n = hw[i];
            i = i + 1;
            if (n == 0xFE00) break;
            k = k + ((n >> 10) & 0x3F) + 1;
            if (k > 63) break;
            val = (s10(n) * IQ[ZAGZIG[k]] * qscale + 4) >> 3;
        }

        return coef;
    }

    public static int[] idct(int[] coef) {
        int[] src = Arrays.copyOf(coef, coef.length);
        int[] dst = new int[64];

        for (int pass = 0; pass < 2; pass++) {
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    int sum = 0;
                    for (int z = 0; z < 8; z++) {
                        sum = sum + src[z * 8 + y] * SCALE[z * 8 + x];
                    }
                    dst[y * 8 + x] = (sum + 0x7FF8) >> 16;
                }
            }
            int[] temp = Arrays.copyOf(src, src.length);
            src = Arrays.copyOf(dst, dst.length);
            dst = Arrays.copyOf(temp, temp.length);
        }

        return src;
    }

    public static int[] decodeImage(List<int[]> blocks, int width, int height) {
        int[] pixels = new int[width * height];
        int rows = height / 16;
        int cols = width / 16;

        for (int m = 0; m < cols * rows; m++) {
            int col = m / rows;
            int row = m % rows;
            int[] cr = idct(rlDecode(blocks.get(m * 6)));
            int[] cb = idct(rlDecode(blocks.get(m * 6 + 1)));
            for (int j = 0; j < 4; j++) {
                int[] yb = idct(rlDecode(blocks.get(m * 6 + 2 + j)));
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        int ci = ((y + YOFF[j]) / 2) * 8 + (x + XOFF[j]) / 2;
                        double r = cr[ci] * 1.402;
                        double g = -0.3437 * cb[ci] - 0.7143 * cr[ci];
                        double b = cb[ci] * 1.772;
                        int rr = (int) (Math.clamp(yb[y * 8 + x] + r, -128, 127) + 128);
                        int gg = (int) (Math.clamp(yb[y * 8 + x] + g, -128, 127) + 128);
                        int bb = (int) (Math.clamp(yb[y * 8 + x] + b, -128, 127) + 128);
                        int px = col * 16 + XOFF[j] + x;
                        int py = row * 16 + YOFF[j] + y;
                        pixels[py * width + px] = (rr << 16) | (gg << 8) | bb;
                    }
                }
            }
        }

        return pixels;
    }

    public static void toRGBA(int[] pixels, int width, int height, ByteBuffer out) {
        for (int y = 0; y < height; y++) {
            int dstRow = height - 1 - y;
            for (int x = 0; x < width; x++) {
                int v = pixels[y * width + x];
                int at = (dstRow * width + x) * 4;
                out.put(at, (byte) ((v >> 16) & 0xFF));
                out.put(at + 1, (byte) ((v >> 8) & 0xFF));
                out.put(at + 2, (byte) (v & 0xFF));
                out.put(at + 3, (byte) 0xFF);
            }
        }
    }

    private static int s10(int value) {
        int v = value & 0x3FF;
        if (v >= 512)
            v = v - 1024;

        return v;
    }


}