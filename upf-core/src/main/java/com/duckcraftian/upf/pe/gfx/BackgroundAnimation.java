package com.duckcraftian.upf.pe.gfx;

import static com.duckcraftian.upf.system.DataTypeHelper.*;

public class BackgroundAnimation {

    public static int[] ctrlFlags, ctrlPos, ctrlStep, ctrlElapsed, ctrlLength;
    public static int[][] slotEntry, slotDuration;
    public static int[] entryFlags;

    public static void initBackground(byte[] block) {
        int controlCount = u16LE(block, 0x04);
        int layerCount = u16LE(block, 0x06);
        int controlOffset = s32LE(block, 0x10);
        int entryOffset = s32LE(block, 0x14);

        entryFlags = new int[layerCount];
        for (int l = 0; l < layerCount; l++)
            entryFlags[l] = u8(block, entryOffset + l * 0x38);

        ctrlFlags = new int[controlCount];
        ctrlPos = new int[controlCount];
        ctrlStep = new int[controlCount];
        ctrlElapsed = new int[controlCount];
        ctrlLength = new int[controlCount];
        slotEntry = new int[controlCount][];
        slotDuration = new int[controlCount][];

        for (int c = 0; c < controlCount; c++) {
            int ca = controlOffset + c * 16;
            int head = (int) u32LE(block, ca);
            ctrlFlags[c] = head & 0xFF;
            ctrlLength[c] = head >>> 8;
            ctrlPos[c] = (int) u32LE(block, ca + 4);
            ctrlStep[c] = s16LE(block, ca + 8);
            ctrlElapsed[c] = u16LE(block, ca + 10);

            int slots = ca + s32LE(block, ca + 12);
            slotEntry[c] = new int[ctrlLength[c]];
            slotDuration[c] = new int[ctrlLength[c]];

            for (int j = 0; j < ctrlLength[c]; j++) {
                slotEntry[c][j] = u8(block, slots + 2 * j);
                slotDuration[c][j] = (byte) u8(block, slots + 2 * j + 1);
            }

            entryFlags[slotEntry[c][0]] |= 2;
        }

    }

}