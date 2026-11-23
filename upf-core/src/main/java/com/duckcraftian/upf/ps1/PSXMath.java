package com.duckcraftian.upf.ps1;

import static com.duckcraftian.upf.system.DataTypeHelper.toS16;

public class PSXMath {

    public static int[] rotMatrixYXZ(int ax, int ay, int az) {
        int cx = rcos(ax);
        int sx = rsin(ax);
        int cy = rcos(ay);
        int sy = rsin(ay);
        int cz = rcos(az);
        int sz = rsin(az);
        int sysx = (sy * sx) >> 12;
        int cysx = (cy * sx) >> 12;

        int[] matrix = new int[9];
        matrix[0] = ((cy * cz) >> 12) + ((sysx * sz) >> 12);
        matrix[1] = ((sysx * cz) >> 12) - ((cy * sz) >> 12);
        matrix[2] = (sy * cx) >> 12;
        matrix[3] = (cx * sz) >> 12;
        matrix[4] = (cx * cz) >> 12;
        matrix[5] = -sx;
        matrix[6] = ((cysx * sz) >> 12) - ((sy * cz) >> 12);
        matrix[7] = ((sy * sz) >> 12) + ((cysx * cz) >> 12);
        matrix[8] = (cy * cx) >> 12;

        return matrix;
    }

    public static void loadAverageCol(byte[] first, int firstAt, byte[] second, int secondAt, int firstScale, int secondScale, byte[] out, int outAt) {
        for (int i = 0; i < 3; i++)
            out[outAt + i] = (byte) (((first[firstAt + i] & 0xFF) * firstScale + (second[secondAt + i] & 0xFF) * secondScale) >> 12);
    }

    public static int fixedMul(int a, int b) {
        return (int) (((long) a * b) >> 16);
    }

    public static int fixedDiv(int a, int b) {
        int d = b >> 8;
        if (d == 0) return (a >= 0 ? -1 : 1) << 8;

        return (a / d) << 8;
    }

    public static int[] averageShorts(int[] first, int[] second, int firstWeight, int secondWeight) {
        int[] out = {0, 0, 0};
        for (int k = 0; k < 3; k++) {
            out[k] = toS16(((first[k] * firstWeight) >> 12) + ((second[k] * secondWeight) >> 12));
        }

        return out;
    }

    public static int ratan2(int y, int x) {
        return (int) Math.round(Math.atan2(y, x) * 2048 / Math.PI);
    }

    public static int squareRoot0(long n) {
        return (int) Math.floor(Math.sqrt(n));
    }

    public static int[] mulRotMatrix(int[] matrix) {
        for (int col = 0; col < 3; col++) {
            PSX.GTE.writeData(0, ((matrix[3 + col] << 16) | (matrix[col] & 0xFFFF)));
            PSX.GTE.writeData(1, matrix[6 + col]);
            PSX.GTE.execute(0x4A486012);
            matrix[col] = PSX.GTE.readData(9);
            matrix[3 + col] = PSX.GTE.readData(10);
            matrix[6 + col] = PSX.GTE.readData(11);
        }

        return matrix;
    }

    public static int[] scaleMatrix(int[] matrix, int sx, int sy, int sz) {
        int[] scale = {sx, sy, sz};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                matrix[3 * row + col] = (short) ((matrix[3 * row + col] * scale[col]) >> 12);
            }
        }

        return matrix;
    }

    public static int rcos(int angle) {
        double radians = angle * 2 * Math.PI / 4096;
        return (int) Math.round(Math.cos(radians) * 4096);
    }

    public static int rsin(int angle) {
        double radians = angle * 2 * Math.PI / 4096;
        return (int) Math.round(Math.sin(radians) * 4096);
    }

    public static int[] mulMatrix(int[] a, int[] b) {
        int[] matrix = new int[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                matrix[3 * i + j] = (a[3 * i] * b[j] + a[3 * i + 1] * b[3 + j] + a[3 * i + 2] * b[6 + j]) >> 12;
            }
        }

        return matrix;
    }

    public static int[] mulVector(int[] a, int x, int y, int z) {
        int[] v = new int[3];
        for (int i = 0; i < 3; i++) {
            v[i] = (a[3 * i] * x + a[3 * i + 1] * y + a[3 * i + 2] * z) >> 12;
        }

        return v;
    }

}