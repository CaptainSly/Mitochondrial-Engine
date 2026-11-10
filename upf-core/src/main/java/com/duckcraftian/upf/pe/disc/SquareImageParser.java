package com.duckcraftian.upf.pe.disc;

import com.duckcraftian.upf.ps1.data_types.PSXImage;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.duckcraftian.upf.system.DataTypeHelper.u16LE;
import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;

public class SquareImageParser {

    public static List<PSXImage> readVramUploads(byte[] container) {
        long dirOffset = u32LE(container, 4);
        long timsWord = u32LE(container, (int) (dirOffset + 0x28));
        long count = timsWord >>> 22;
        long tableOff = timsWord & 0x3FFFFF;

        List<PSXImage> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long base = tableOff + 20 * i;
            long src = u32LE(container, (int) (base + 4));
            long rect = u32LE(container, (int) (base + 8));
            long source_rect = u32LE(container, (int) (base + 12));
            long rect2 = u32LE(container, (int) (base + 16));

            long offset = src & 0xFFFFFF;
            int height = (int) (src >>> 24);
            int width = (int) (rect & 0x3FF);
            int x = (int) ((rect >>> 10) & 0x7FF);
            int y = (int) (rect >>> 21);

            int size = width * 2 * height;
            int end = (int) (offset + size);
            if (end > container.length)
                throw new RuntimeException("Record: " + i + " Offset: " + offset + " Size: " + size + " Container Length: " + container.length);

            byte[] pixels = Arrays.copyOfRange(container, (int) offset, end);
            result.add(new PSXImage(x, y, width, height, pixels));
            if (source_rect != 0) {
                long offset2 = end;
                long height2 = source_rect >> 24;
                long width2 = (int) (rect2 & 0x3FF);
                long x2 = (rect2 >> 10) & 0x7FF;
                long y2 = rect2 >> 21;
                long size2 = width2 * 2 * height2;
                long endAt2 = offset2 + size2;
                byte[] pixels2 = Arrays.copyOfRange(container, (int) offset2, (int) endAt2);
                result.add(new PSXImage((int) x2, (int) y2, (int) width2, (int) height2, pixels2));
            }
        }

        return result;
    }

    public static PSXImage readTIMImage(byte[] blob, int index) {
        int tim = (int) u32LE(blob, index * 4);
        int flags = (int) u32LE(blob, tim + 4);
        int block = tim + 8;
        if ((flags & 8) != 0)
            block = (int) (block + u32LE(blob, block));

        int x = u16LE(blob, block + 4);
        int y = u16LE(blob, block + 6);
        int w = u16LE(blob, block + 8);
        int h = u16LE(blob, block + 10);

        byte[] pixels = Arrays.copyOfRange(blob, (block + 12), block + 12 + w * 2 * h);
        return new PSXImage(x, y, w, h, pixels);
    }

    public static byte[] convertImagesToBytes(List<PSXImage> images) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {

            out.writeInt(images.size());

            for (PSXImage image : images) {
                out.writeShort(image.x());
                out.writeShort(image.y());
                out.writeShort(image.width());
                out.writeShort(image.height());
                out.write(image.pixels());
            }
        }

        return bytes.toByteArray();
    }

    public static PSXImage readTIMClut(byte[] blob, int index) {
        int tim = (int) u32LE(blob, index * 4);
        int block = tim + 8;
        int x = u16LE(blob, block + 4);
        int y = u16LE(blob, block + 6);
        int w = u16LE(blob, block + 8);
        int h = u16LE(blob, block + 10);
        byte[] pixels = Arrays.copyOfRange(blob, block + 12, block + 12 + w * 2 * h);
        return new PSXImage(x, y, w, h, pixels);
    }

    public static List<PSXImage> convertBytesToImages(byte[] data) throws IOException {
        List<PSXImage> images = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int imageCount = in.readInt();

            for (int i = 0; i < imageCount; i++) {
                int x = in.readShort();
                int y = in.readShort();
                int width = in.readShort();
                int height = in.readShort();
                byte[] pixels = new byte[width * 2 * height];
                in.readFully(pixels);

                images.add(new PSXImage(x, y, width, height, pixels));
            }
        }

        return images;
    }

}