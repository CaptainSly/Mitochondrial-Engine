package com.duckcraftian.upf.pe.gfx;


import com.duckcraftian.upf.pe.script.ScriptVM;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.duckcraftian.upf.pe.ParasiteEve.AYA_STATS;
import static com.duckcraftian.upf.pe.script.ScriptVM.gameStateFlags;
import static com.duckcraftian.upf.pe.script.ScriptVM.menuUnlockMask;

public class PEHud {

    public static final int HUD_PAGE = 0x34;
    public static final int HUD_CLUT = 32275;

    public static final int ATB_LEFT_N = packRGB(0x00, 0x46, 0x82);
    public static final int ATB_RIGHT_N = packRGB(0x9F, 0xFF, 0xF9);
    public static final int ATB_MID = packRGB(0x50, 0xA3, 0xBE);

    public static final int PE_LEFT_N = packRGB(0x00, 0x82, 0x36);
    public static final int PE_RIGHT_N = packRGB(0x4A, 0xFF, 0x3B);
    public static final int PE_MID = packRGB(0x25, 0xC1, 0x39);

    public static final int PINK_LEFT_N = packRGB(0xFF, 0x3D, 0x81);
    public static final int PINK_RIGHT_N = packRGB(0x83, 0x13, 0x01);
    public static final int PINK_MID = packRGB(0xC1, 0x28, 0x41);

    public static List<Integer> opaque = new ArrayList<>();
    public static List<Integer> semi = new ArrayList<>();

    public static int[] windowBounds = new int[8];
    public static int hudMode, hudX, hudY;
    public static int atbLeft, atbRight, peLeft, peRight, pinkLeft, pinkRight;

    public static void buildPlayerPanel() {
        boolean battle = (gameStateFlags & 2) != 0;
        boolean hasPE = (menuUnlockMask & 2) != 0;

        if (AYA_STATS.atbGauge >= 9001) AYA_STATS.atbGauge = 9000;

        if (AYA_STATS.peAcc > AYA_STATS.maxAtk) AYA_STATS.peAcc = AYA_STATS.maxAtk;
        else if (AYA_STATS.peAcc < 0) AYA_STATS.peAcc = 0;

        if (AYA_STATS.maxHp < AYA_STATS.curHp) AYA_STATS.curHp = AYA_STATS.maxHp;


        int atbWidth = ((AYA_STATS.atbGauge * 0x38)) / 9000;
        int peWidth = ((AYA_STATS.peAcc >> 16) * 0x38) / (AYA_STATS.maxAtk >> 16);

        if (hasPE) {
            if (battle) addFlatQuad(semi, hudX, hudY, 0x54, 0x20, 0x303030, 7);
            else addFlatQuad(semi, hudX, hudY + 7, 0x54, 0x19, 0x303030, 7);
        } else {
            if (battle) addFlatQuad(semi, hudX, hudY, 0x54, 0x19, 0x303030, 7);
            else addFlatQuad(semi, hudX, hudY + 7, 0x54, 0x12, 0x303030, 7);
        }

        if (battle) {
            int ax = hudX + 8;
            int ay = hudY + 4;
            addFlatQuad(opaque, ax, ay, 0x38, 3, packRGB(0x1D, 0x3E, 0x32), 6);
            addGradientQuad(opaque, ax, ay, atbLeft, ax + atbWidth, ay, atbRight, ax, ay + 3, atbLeft, ax + atbWidth, ay + 3, atbRight, 5);
            addSprite(opaque, ax + atbWidth, hudY + 2, 0xC8, 0xE0, 4, 8, HUD_PAGE, HUD_CLUT, atbRight, 4);
            addSprite(opaque, hudX + 0x44, hudY + 3, 0x50, 0xF4, 8, 4, HUD_PAGE, HUD_CLUT, 0x808080, 4);
        }

        if (hasPE) {
            int px = hudX + 8;
            int py = hudY + 0x19;
            addGradientQuad(opaque, px, py, peLeft, px + peWidth, py, peRight, px, py + 3, peLeft, px + peWidth, py + 3, peRight, 5);
            addGradientQuad(opaque, px + peWidth, py, pinkLeft, px + 0x38, py, pinkRight, px + peWidth, py + 3, pinkLeft, px + 0x38, py + 3, pinkRight, 5);
            addSprite(opaque, hudX + 0x44, hudY + 0x18, 0x60, 0xF4, 8, 4, HUD_PAGE, HUD_CLUT, 0x808080, 4);
        }

        addHpNumbers(AYA_STATS.maxHp, AYA_STATS.curHp);
    }

    public static void resetGaugeColors() {
        atbLeft = ATB_LEFT_N;
        atbRight = ATB_RIGHT_N;
        peLeft = PE_LEFT_N;
        peRight = PE_RIGHT_N;
        pinkLeft = PINK_LEFT_N;
        pinkRight = PINK_RIGHT_N;
    }

    public static void updateGaugeColors() {
        int phase = ScriptVM.battleFrame & 3;
        if ((gameStateFlags & 0x100) == 0) {
            if (AYA_STATS.peReadyFlash != 0) {
                peLeft = leftOf(phase, PE_LEFT_N, PE_RIGHT_N, PE_MID);
                peRight = rightOf(phase, PE_LEFT_N, PE_RIGHT_N, PE_MID);
                if (AYA_STATS.peAcc < AYA_STATS.maxAtk) AYA_STATS.peReadyFlash = 0;
            }

            if (AYA_STATS.peReadyFlash == 0) {
                peLeft = PE_LEFT_N;
                peRight = PE_RIGHT_N;
            }
        }

        if ((AYA_STATS.stateFlags & 0x2000) != 0) {
            pinkLeft = leftOf(phase, PINK_LEFT_N, PINK_RIGHT_N, PINK_MID);
            pinkRight = rightOf(phase, PINK_LEFT_N, PINK_RIGHT_N, PINK_MID);
        } else {
            pinkLeft = PINK_LEFT_N;
            pinkRight = PINK_RIGHT_N;
        }

        if (AYA_STATS.curHp <= 0) resetGaugeColors();
    }

    private static int leftOf(int phase, int a, int b, int mid) {
        if (phase == 0) return a;
        if (phase == 2) return b;
        return mid;
    }

    private static int rightOf(int phase, int a, int b, int mid) {
        if (phase == 0) return b;
        if (phase == 2) return a;
        return mid;
    }

    public static void setATBFlash(int phase) {
        atbLeft = leftOf(phase, ATB_LEFT_N, ATB_RIGHT_N, ATB_MID);
        atbRight = rightOf(phase, ATB_LEFT_N, ATB_RIGHT_N, ATB_MID);
    }


    private static void addHpNumbers(int max, int cur) {
        int y = hudY + 11;
        int[] digits = new int[8];

        int n = 0;
        int value = max;
        while (true) {
            int q = value / 10;
            digits[n] = value - q * 10;
            value = q;
            if (value == 0) break;
            n += 1;
        }

        n += 1;
        digits[n] = 10;
        int last = n;
        int x = hudX - n * 6 + 56;
        for (int k = n; k >= 0; k--) {
            addSprite(opaque, x, y, digits[k] * 8, 0xE8, 6, 10, HUD_PAGE, HUD_CLUT, 0x808080, 5);
            x += 6;
        }

        x = hudX - (last * 2 + 1) * 6 + 62;
        n = 0;
        value = cur;
        while (true) {
            int q = value / 10;
            digits[n] = value - q * 10;
            value = q;
            if (value == 0) break;
            n += 1;
        }
        int missing = last - n - 1;
        if (missing > 0) x += missing * 6;
        for (int k = n; k >= 0; k--) {
            addSprite(opaque, x, y, digits[k] * 8, 0xE8, 6, 10, HUD_PAGE, HUD_CLUT, 0x808080, 5);
            x += 6;
        }

        addSprite(opaque, hudX + 0x44, hudY + 0xE, 0x58, 0xF4, 8, 4, HUD_PAGE, HUD_CLUT, 0x808080, 4);
    }

    public static void setWindowMode(int mode) {
        hudMode = mode;
        hudX = windowBounds[mode * 2];
        hudY = windowBounds[mode * 2 + 1];
    }

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