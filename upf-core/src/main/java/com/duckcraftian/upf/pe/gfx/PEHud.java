package com.duckcraftian.upf.pe.gfx;

import java.util.ArrayList;
import java.util.List;

public class PEHud {

    public static List<Integer> opaque = new ArrayList<>();
    public static List<Integer> semi = new ArrayList<>();

    public static int x, y, w, h;
    public static int u, v;

    public static int rgb;
    public static int slot;

    public static void addPolyVertex(List<Integer> out, int x, int y, int u, int v, int page, int clut, int slot, int rgb) {
        out.add(x);
        out.add(y);
        out.add(u);
        out.add(v);
        out.add(page);
        out.add(clut);
        out.add(slot);
        out.add(rgb);
    }

    public static void addGradientQuad(List<Integer> out, int x0, int y0, int rgb0, int x1, int y1, int rgb1, int x2, int y2, int rgb2, int x3, int y3, int rgb3, int slot) {
        addPolyVertex(out, x0, y0, 0, 0, -1, 0, slot, rgb0);
        addPolyVertex(out, x1, y1, 0, 0, -1, 0, slot, rgb1);
        addPolyVertex(out, x2, y2, 0, 0, -1, 0, slot, rgb2);
        addPolyVertex(out, x1, y1, 0, 0, -1, 0, slot, rgb1);
        addPolyVertex(out, x3, y3, 0, 0, -1, 0, slot, rgb3);
        addPolyVertex(out, x2, y2, 0, 0, -1, 0, slot, rgb2);
    }

    public static void addFlatQuad(List<Integer> out, int x, int y, int w, int h, int rgb, int slot) {
        addPolyVertex(out, x, y, 0, 0, -1, 0, slot, rgb);
        addPolyVertex(out, x + w, y, 0, 0, -1, 0, slot, rgb);
        addPolyVertex(out, x, y + h, 0, 0, -1, 0, slot, rgb);
        addPolyVertex(out, x + w, y, 0, 0, -1, 0, slot, rgb);
        addPolyVertex(out, x + w, y + h, 0, 0, -1, 0, slot, rgb);
        addPolyVertex(out, x, y + h, 0, 0, -1, 0, slot, rgb);
    }

    public static void addSprite(List<Integer> out, int x, int y, int u, int v, int w, int h, int page, int clut, int rgb, int slot) {
        addPolyVertex(out, x, y, u, v, page, clut, slot, rgb);
        addPolyVertex(out, x + w, y, u + w, v, page, clut, slot, rgb);
        addPolyVertex(out, x, y + h, u, v + h, page, clut, slot, rgb);
        addPolyVertex(out, x + w, y, u + w, v, page, clut, slot, rgb);
        addPolyVertex(out, x + w, y + h, u + w, v + h, page, clut, slot, rgb);
        addPolyVertex(out, x, y + h, u, v + h, page, clut, slot, rgb);
    }

    public static int packRGB(int r, int g, int b) {
        return r | (g << 8) | (b << 16);
    }

}