package com.duckcraftian.upf.ps1;

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

    public static int fixedMul(int a, int b) {
        return (int) (((long) a * b) >> 16);
    }

    public static int fixedDiv(int a, int b) {
        return (a / (b >> 8)) << 8;
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