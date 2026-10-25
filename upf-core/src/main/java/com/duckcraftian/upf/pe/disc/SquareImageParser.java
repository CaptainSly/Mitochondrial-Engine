package com.duckcraftian.upf.pe.disc;

import com.duckcraftian.upf.ps1.data_types.PSXImage;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            if (source_rect > 0)
                throw new IllegalArgumentException("uwu we're not dealing with this just yet. :P");

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
        }

        return result;
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