package com.duckcraftian.upf.ps1;

public class PSXMath {

    public static int[] rotMatrixYXZ(int ax, int ay, int az) {
        int cx = (int) Math.round(Math.cos(ax * 2 * Math.PI / 4096) * 4096);
        int sx = (int) Math.round(Math.sin(ax * 2 * Math.PI / 4096) * 4096);
        int cy = (int) Math.round(Math.cos(ay * 2 * Math.PI / 4096) * 4096);
        int sy = (int) Math.round(Math.sin(ay * 2 * Math.PI / 4096) * 4096);
        int cz = (int) Math.round(Math.cos(az * 2 * Math.PI / 4096) * 4096);
        int sz = (int) Math.round(Math.sin(az * 2 * Math.PI / 4096) * 4096);
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