package com.duckcraftian.upf.pe.disc.rooms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.duckcraftian.upf.system.DataTypeHelper.*;

public class RoomParser {

    public static byte[] readBackgroundBlock(byte[] roomContainer) {
        int directoryOffset = (int) u32LE(roomContainer, 4);
        long field = u32LE(roomContainer, directoryOffset + 0x1C);
        int tableOffset = (int) field & 0x3FFFFF;

        long word = u32LE(roomContainer, tableOffset + 4);
        int blockOffset = (int) (word & 0xFFFFFF);
        int blockSize = u16LE(roomContainer, blockOffset);

        byte[] backgroundBlock = new byte[blockSize];
        System.arraycopy(roomContainer, blockOffset, backgroundBlock, 0, blockSize);

        return backgroundBlock;
    }

    public static List<BackgroundTile> parseBackgroundBlock(byte[] block, int group) {
        List<BackgroundTile> tiles = new ArrayList<>();

        int field26 = u16LE(block, 0x26);
        int layerCount = u16LE(block, 0x06);
        int entryOffset = s32LE(block, 0x14);

        int outX = s16LE(block, 0x38);
        int outY = s16LE(block, 0x3A);

        for (int l = 0; l < layerCount; l++) {
            int entry = entryOffset + l * 0x38;
            int layerGroup = u8(block, entry + 0x24);

            if (layerGroup != group)
                continue;

            long first = u32LE(block, entry);
            int layerDepth = (int) ((first >> 8) & 0xFFF);
            int tileCount = u16LE(block, entry + 0x26);
            int posStart = entry + s32LE(block, entry + 0x28);
            int uvStart = entry + s32LE(block, entry + 0x2C);

            int scrX = u16LE(block, entry + 0x0C);
            int scrY = u16LE(block, entry + 0x0E);

            for (int t = 0; t < tileCount; t++) {
                long posWord = u32LE(block, posStart + t * 4);

                int x = (int) (posWord >> 22) + scrX + outX;
                int y = (int) ((posWord >> 12) & 0x3FF) + scrY + outY;
                int tileDepth = (int) (posWord & 0xFFF);

                long w0 = u32LE(block, uvStart + t * 8);
                long w1 = u32LE(block, uvStart + t * 8 + 4);

                int clutY = (int) (w0 & 511);
                int clutX = (int) ((w0 >> 5) & 0x3F0);
                int pageX = (int) ((w0 >> 10) & 0x3C0);
                int pageY = (int) ((w0 >> 7) & 256);
                int u = (int) (w1 & 0xFF);
                int v = (int) ((w0 >> 24) & 0xFF);
                int orderIndex = field26 + layerDepth + tileDepth;

                tiles.add(new BackgroundTile(x, y, clutX, clutY, pageX, pageY, u, v, orderIndex));
            }

        }

        tiles.sort(Comparator.comparingInt(BackgroundTile::orderIndex).reversed());
        return tiles;
    }

    public static List<PSXViewport> parseViewports(byte[] block) {
        int viewportOffset = s32LE(block, 0x1C);
        int count = (block.length - viewportOffset) / 52;

        List<PSXViewport> viewports = new ArrayList<>();
        for (int v = 0; v < count; v++) {
            int at = viewportOffset + v * 52;

            int projectionDistance = u16LE(block, at);
            int[] rotation = new int[9];
            for (int i = 0; i < rotation.length; i++)
                rotation[i] = s16LE(block, at + 0x02 + i * 2);

            int[] translation = new int[3];
            for (int i = 0; i < translation.length; i++)
                translation[i] = s32LE(block, at + 0x14 + i * 4);

            int width = u16LE(block, at + 0x28);
            int height = u16LE(block, at + 0x2A);
            int minX = s16LE(block, at + 0x2C);
            int maxX = s16LE(block, at + 0x2E);
            int minY = s16LE(block, at + 0x30);
            int maxY = s16LE(block, at + 0x32);

            viewports.add(new PSXViewport(projectionDistance, rotation, translation, width, height, minX, maxX, minY, maxY));
        }

        return viewports;
    }

}