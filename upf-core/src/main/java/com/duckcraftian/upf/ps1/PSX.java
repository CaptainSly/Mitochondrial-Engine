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

    public static final int CMD_RTPS = 0x0180001;
    public static final int CMD_RTPT = 0x0280030;
    public static final int CMD_NCLIP = 0x1400006;
    public static final int CMD_AVSZ3 = 0x158002D;
    public static final int CMD_AVSZ4 = 0x168002E;

    public static final GTE GTE = new GTE();

}