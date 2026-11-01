package com.duckcraftian.upf.pe.gfx;

import com.duckcraftian.upf.pe.script.ScriptVM;
import com.duckcraftian.upf.system.DataTypeHelper;
import com.duckcraftian.upf.system.Utils;

import java.util.ArrayList;
import java.util.List;

import static com.duckcraftian.upf.system.DataTypeHelper.u8;

public class PEText {

    public static final int FONT_PAGE = 5;
    public static final int FONT_CLUT = 16148;
    public static final int ICON_PAGE = 20;
    public static final int ICON_CLUT = 30611;
    public static final int POINTER_CLUT = 30547;

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

    public static List<int[]> layoutMessage(byte[] container, int bank, int id, List<Integer> delays, List<int[]> choices) {
        List<int[]> sections = new ArrayList<>();
        List<Integer> out = new ArrayList<>();
        int[] choiceNow = {0, 0, 0};

        int x = 20;
        int y = 174;

        int pos = bank;
        while (!(u8(container, pos) == 0xFE && u8(container, pos + 1) == id)) {
            pos += 1;
            if (pos >= container.length - 1) {
                sections.add(new int[0]);
                delays.add(0);
                choices.add(new int[]{0, 0, 0});
                return sections;
            }
        }

        pos += 2;

        while (u8(container, pos) != 0xFF && u8(container, pos) != 0xF9) {
            int b = u8(container, pos);
            if (b == 0xF7) {
                x = 20;
                y += 12;
            } else if (b == 0xF8) {
                sections.add(Utils.intListToArray(out));
                delays.add(0);
                choices.add(choiceNow);
                choiceNow = new int[]{0, 0, 0};
                out = new ArrayList<>();
                x = 20;
                y = 174;
            } else if (b == 0xFB) {
                pos += 1;
                int sub = u8(container, pos);
                if (sub == 0x09) {
                    pos += 1;
                    choiceNow = new int[]{DataTypeHelper.u8(container, pos), x, y};
                } else if (sub == 0x07) {
                    pos += 1;
                    sections.add(Utils.intListToArray(out));
                    choices.add(choiceNow);
                    choiceNow = new int[]{0, 0, 0};
                    delays.add(u8(container, pos));
                } else if (sub <= 0x03) {
                    x += addIcon(out, sub, x, y);
                }
            } else if (b == 0xFA) {
                for (int g : ScriptVM.playerName)
                    x += addGlyph(out, g, x, y);
            } else if (b < 0xF7)
                x += addGlyph(out, b, x, y);

            pos += 1;
        }

        sections.add(Utils.intListToArray(out));
        choices.add(choiceNow);
        if ((u8(container, pos) == 0xF9))
            delays.add(1);
        else
            delays.add(0);

        return sections;
    }

    public static void addPointer(List<Integer> out, int x, int y) {
        int u = 112;
        int v = 212;

        int x0 = x, x1 = x + 24;
        int y0 = y + 1, y1 = y + 13;
        int u0 = u, u1 = u + 24;
        int v0 = v, v1 = v + 12;

        addVertex(out, x0, y0, u0, v0, ICON_PAGE, POINTER_CLUT);
        addVertex(out, x1, y0, u1, v0, ICON_PAGE, POINTER_CLUT);
        addVertex(out, x0, y1, u0, v1, ICON_PAGE, POINTER_CLUT);

        addVertex(out, x1, y0, u1, v0, ICON_PAGE, POINTER_CLUT);
        addVertex(out, x1, y1, u1, v1, ICON_PAGE, POINTER_CLUT);
        addVertex(out, x0, y1, u0, v1, ICON_PAGE, POINTER_CLUT);
    }

    private static int addIcon(List<Integer> out, int n, int x, int y) {
        int u = 0x40 + 12 * n;
        int v = 212;

        int x0 = x, x1 = x + 12;
        int y0 = y + 1, y1 = y + 13;
        int u0 = u, u1 = u + 12;
        int v0 = v, v1 = v + 12;

        addVertex(out, x0, y0, u0, v0, ICON_PAGE, ICON_CLUT);
        addVertex(out, x1, y0, u1, v0, ICON_PAGE, ICON_CLUT);
        addVertex(out, x0, y1, u0, v1, ICON_PAGE, ICON_CLUT);

        addVertex(out, x1, y0, u1, v0, ICON_PAGE, ICON_CLUT);
        addVertex(out, x1, y1, u1, v1, ICON_PAGE, ICON_CLUT);
        addVertex(out, x0, y1, u0, v1, ICON_PAGE, ICON_CLUT);

        return 12;
    }

    private static void addVertex(List<Integer> out, int x, int y, int u, int v) {
        addVertex(out, x, y, u, v, FONT_PAGE, FONT_CLUT);
    }

    private static void addVertex(List<Integer> out, int x, int y, int u, int v, int page, int clut) {
        out.add(x);
        out.add(y);
        out.add(u);
        out.add(v);
        out.add(page);
        out.add(clut);
        out.add(0);
    }

}