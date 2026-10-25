package com.duckcraftian.upf.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataTypeTest {

    static byte[] bytes(int... value) {
        byte[] b = new byte[value.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) value[i];
        }

        return b;
    }


    @Test
    void u8Test() {
        byte[] u8Bytes = bytes(
                0x00,
                0x7F,
                0x80,
                0xFF
        );


        assertEquals(0, com.duckcraftian.upf.system.DataTypeHelper.u8(u8Bytes, 0));
        assertEquals(127, com.duckcraftian.upf.system.DataTypeHelper.u8(u8Bytes, 1));
        assertEquals(128, com.duckcraftian.upf.system.DataTypeHelper.u8(u8Bytes, 2));
        assertEquals(255, com.duckcraftian.upf.system.DataTypeHelper.u8(u8Bytes, 3));
    }

    @Test
    void s8Test() {
        byte[] s8Bytes = bytes(
                0x7F,
                0x80,
                0xFF
        );

        assertEquals(127, com.duckcraftian.upf.system.DataTypeHelper.s8(s8Bytes, 0));
        assertEquals(-128, com.duckcraftian.upf.system.DataTypeHelper.s8(s8Bytes, 1));
        assertEquals(-1, com.duckcraftian.upf.system.DataTypeHelper.s8(s8Bytes, 2));
    }

    @Test
    void u16LETest() {
        byte[] u16Bytes = bytes(
                0x34, 0x12,
                0xFF, 0x00,
                0x00, 0xFF,
                0xFF, 0xFF
        );

        assertEquals(0x1234, com.duckcraftian.upf.system.DataTypeHelper.u16LE(u16Bytes, 0));
        assertEquals(255, com.duckcraftian.upf.system.DataTypeHelper.u16LE(u16Bytes, 2));
        assertEquals(65280, com.duckcraftian.upf.system.DataTypeHelper.u16LE(u16Bytes, 4));
        assertEquals(65535, com.duckcraftian.upf.system.DataTypeHelper.u16LE(u16Bytes, 6));
    }

    @Test
    void s16LETest() {
        byte[] s16Bytes = bytes(
                0xFF, 0x7F,
                0x00, 0x80,
                0xFF, 0xFF
        );

        assertEquals(32767, com.duckcraftian.upf.system.DataTypeHelper.s16LE(s16Bytes, 0));
        assertEquals(-32768, com.duckcraftian.upf.system.DataTypeHelper.s16LE(s16Bytes, 2));
        assertEquals(-1, com.duckcraftian.upf.system.DataTypeHelper.s16LE(s16Bytes, 4));
    }

    @Test
    void u32LETest() {
        byte[] u32Bytes = bytes(
                0x78, 0x56, 0x34, 0x12,
                0xFF, 0xFF, 0xFF, 0x7F,
                0x00, 0x00, 0x00, 0x80,
                0xFF, 0xFF, 0xFF, 0xFF
        );

        assertEquals(0x12345678L, com.duckcraftian.upf.system.DataTypeHelper.u32LE(u32Bytes, 0));
        assertEquals(2147483647L, com.duckcraftian.upf.system.DataTypeHelper.u32LE(u32Bytes, 4));
        assertEquals(2147483648L, com.duckcraftian.upf.system.DataTypeHelper.u32LE(u32Bytes, 8));
        assertEquals(4294967295L, com.duckcraftian.upf.system.DataTypeHelper.u32LE(u32Bytes, 12));
    }

    @Test
    void s32LETest() {
        byte[] s32Bytes = bytes(
                0xFF, 0xFF, 0xFF, 0x7F,
                0x00, 0x00, 0x00, 0x80,
                0xFF, 0xFF, 0xFF, 0xFF
        );

        assertEquals(2147483647, com.duckcraftian.upf.system.DataTypeHelper.s32LE(s32Bytes, 0));
        assertEquals(-2147483648, com.duckcraftian.upf.system.DataTypeHelper.s32LE(s32Bytes, 4));
        assertEquals(-1, com.duckcraftian.upf.system.DataTypeHelper.s32LE(s32Bytes, 8));
    }

}