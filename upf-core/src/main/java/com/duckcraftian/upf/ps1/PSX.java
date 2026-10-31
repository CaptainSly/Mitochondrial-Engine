package com.duckcraftian.upf.ps1;

import com.duckcraftian.upf.ps1.gfx.GTE;

public class PSX {

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

    public static String glyph(int b) {
        if (b <= 0x09) return String.valueOf(b);
        if (b == 0x0F) return " ";
        if (b >= 0x10 && b <= 0x29) return String.valueOf((char) ('A' + (b - 0x10)));
        if (b >= 0x30 && b <= 0x49) return String.valueOf((char) ('a' + (b - 0x30)));
        if (b == 0x2A) return "&";
        if (b == 0x2B) return "!";
        if (b == 0x2C) return "?";
        if (b == 0x2D) return "\"";
        if (b == 0x2E) return "'";
        if (b == 0x2F) return ".";
        if (b == 0x4A) return ":";
        if (b == 0x4B) return ",";
        return "";
    }

    public static int code(String s) {
        if (s == null || s.length() != 1) return -1;
        char c = s.charAt(0);

        if (c >= '0' && c <= '9') return c - '0';          // 0x00-0x09
        if (c >= 'A' && c <= 'Z') return 0x10 + (c - 'A'); // 0x10-0x29
        if (c >= 'a' && c <= 'z') return 0x30 + (c - 'a'); // 0x30-0x49

        switch (c) {
            case ' ':
                return 0x0F;
            case '&':
                return 0x2A;
            case '!':
                return 0x2B;
            case '?':
                return 0x2C;
            case '"':
                return 0x2D;
            case '\'':
                return 0x2E;
            case '.':
                return 0x2F;
            case ':':
                return 0x4A;
            case ',':
                return 0x4B;
            default:
                return -1; // no mapping
        }
    }
}