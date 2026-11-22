package com.duckcraftian.upf.ps1;

import com.duckcraftian.upf.ps1.gfx.GTE;

public class PSX {

    private static int randSeed;

    public static void srand(int seed) {
        randSeed = seed;
    }

    public static int rand() {
        randSeed = randSeed * 0x41C64E6D + 0x3039;
        return (randSeed >> 16) & 0x7FFF;
    }

    // Opcodes
    public static final int OP_RTPS = 0x01;
    public static final int OP_NCLIP = 0x06;
    public static final int OP_AVSZ3 = 0x2D;
    public static final int OP_AVSZ4 = 0x2E;
    public static final int OP_RTPT = 0x30;
    public static final int OP_MVMVA = 0x12;

    public static final int CMD_RTPS = 0x0180001;
    public static final int CMD_RTPT = 0x0280030;
    public static final int CMD_NCLIP = 0x1400006;
    public static final int CMD_AVSZ3 = 0x158002D;
    public static final int CMD_AVSZ4 = 0x168002E;
    public static final int CMD_MVMVA = 0x4A480012;

    public static final GTE GTE = new GTE();

    public static int getTPage(int tp, int abr, int x, int y) {
        return ((tp & 3) << 7) | ((abr & 3) << 5) | ((y & 0x100) >> 4) | ((x & 0x3FF) >> 6) | ((y & 0x200) << 2);
    }

    public static int getClut(int x, int y) {
        return ((y << 6) | ((x >> 4) & 0x3F)) & 0xFFFF;
    }

    public static int packRGBColor(int r, int g, int b) {
        return (r & 0xFF) | ((g & 0xFF) << 8) | ((b & 0xFF) << 16);
    }

}