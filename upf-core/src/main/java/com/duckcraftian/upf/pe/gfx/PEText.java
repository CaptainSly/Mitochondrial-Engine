package com.duckcraftian.upf.pe.gfx;

import java.util.List;

public class PEText {

    public static final int FONT_PAGE = 5;
    public static final int FONT_CLUT = 16148;

    public static byte[] metrics;

    public static int addGlyph(List<Integer> out, int code, int x, int y) {
        int g = code;

        if (g >= 0x101)
            g -= 0x13;

        int metric = metrics[g] & 0xFF;
        int width = metric >> 4;
        int nibble = metric & 15;
        int u = (g % 21) * 12 + nibble;
        int v = (g / 21) * 12;

        int x0 = x, x1 = x + width;
        int y0 = y + 1, y1 = y + 13;
        int u0 = u, u1 = u + width;
        int v0 = v, v1 = v + 12;

        addVertex(out, x0, y0, u0, v0);
        addVertex(out, x1, y0, u1, v0);
        addVertex(out, x0, y1, u0, v1);

        addVertex(out, x1, y0, u1, v0);
        addVertex(out, x1, y1, u1, v1);
        addVertex(out, x0, y1, u0, v1);

        if (g < 10 || g == 15)
            return width + 2;

        return width + 1;
    }

    private static void addVertex(List<Integer> out, int x, int y, int u, int v) {
        out.add(x);
        out.add(y);
        out.add(u);
        out.add(v);
        out.add(FONT_PAGE);
        out.add(FONT_CLUT);
        out.add(0);
    }

}