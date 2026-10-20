package com.duckcraftian.upf.utils;

public class DataTypeHelper {

    public static String ascii(byte[] buffer, int offset, int strLen) {
        char[] chars = new char[strLen];
        for (int i = 0; i < strLen; i++)
            chars[i] = ((char) u8(buffer, offset + i));

        return String.valueOf(chars);
    }

    public static int u8(byte[] buffer, int offset) {
        return buffer[offset] & 0xFF;
    }

    public static int s8(byte[] buffer, int offset) {
        return buffer[offset];
    }

    public static int u16LE(byte[] buffer, int offset) {
        int b0 = buffer[offset] & 0xFF;
        int b1 = buffer[offset + 1] & 0xFF;

        return b0 | (b1 << 8);
    }

    public static int s16LE(byte[] buffer, int offset) {
        int b0 = buffer[offset] & 0xFF;
        int b1 = buffer[offset + 1] & 0xFF;

        return (short) (b0 | (b1 << 8));
    }

    public static long u32LE(byte[] buffer, int offset) {
        long b0 = buffer[offset] & 0xFF;
        long b1 = buffer[offset + 1] & 0xFF;
        long b2 = buffer[offset + 2] & 0xFF;
        long b3 = buffer[offset + 3] & 0xFF;

        long result = b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
        return result;
    }

    public static int s32LE(byte[] buffer, int offset) {
        int b0 = buffer[offset] & 0xFF;
        int b1 = buffer[offset + 1] & 0xFF;
        int b2 = buffer[offset + 2] & 0xFF;
        int b3 = buffer[offset + 3] & 0xFF;

        int result = b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
        return result;
    }

}