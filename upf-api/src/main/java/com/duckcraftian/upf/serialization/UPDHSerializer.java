package com.duckcraftian.upf.serialization;

import org.joml.Vector2L;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.zstd.Zstd;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class UPDHSerializer {

    public static final int VERSION = 1;

    public static final byte[] MAGIC_BYTES = {'U', 'P', 'D', 'H'};

    public static LinkedHashMap<String, byte[]> read(Path file) throws IOException {
        LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {

            // Read Header
            byte[] magic = new byte[MAGIC_BYTES.length];
            in.readFully(magic);

            if (!Arrays.equals(MAGIC_BYTES, magic))
                throw new RuntimeException("Not a UPDH File, got the magic bytes: " + MAGIC_BYTES);

            int version = in.readInt();
            if (version != VERSION)
                throw new RuntimeException("Version Mismatch. Looking for Version: " + VERSION + " got Version: " + version + " instead");

            int numCompressedEntries = in.readInt();

            List<String> keys = new ArrayList<>();

            // Vector2L used instead of a new Record/Class
            // X = Uncompressed Size
            // Y = Compressed Size
            List<Vector2L> sizes = new ArrayList<>(); // This is the stupidest fucking thing I've ever done in my life. I fucking love it XD

            // Pass 1: Table of Contents
            for (int i = 0; i < numCompressedEntries; i++) {
                String key = in.readUTF();
                long size = in.readLong();
                long compressedSize = in.readLong();

                keys.add(key);
                sizes.add(new Vector2L(size, compressedSize));
            }

            List<DataEntry> compressedEntries = new ArrayList<>();

            // Pass 2: Compressed Data
            for (int i = 0; i < numCompressedEntries; i++) {
                Vector2L size = sizes.get(i);
                byte[] compressedData = new byte[(int) size.y];
                in.readFully(compressedData);

                compressedEntries.add(new DataEntry(size.x, size.y, compressedData));
            }

            List<byte[]> decompressedEntries = getDecompressedEntries(compressedEntries);

            // Add to Entries
            for (int i = 0; i < decompressedEntries.size(); i++) {
                entries.put(keys.get(i), decompressedEntries.get(i));
            }

        }
        return entries;
    }

    public static void write(Path file, LinkedHashMap<String, byte[]> entries) {
        List<DataEntry> compressedEntries = getCompressedEntries(entries.values().stream().toList());
        List<String> keys = entries.keySet().stream().toList();

        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
            // Write Header
            out.write(MAGIC_BYTES);
            out.writeInt(VERSION);
            out.writeInt(compressedEntries.size());

            // Write out Table of Contents
            for (int i = 0; i < compressedEntries.size(); i++) {
                String key = keys.get(i);
                DataEntry entry = compressedEntries.get(i);

                out.writeUTF(key);
                out.writeLong(entry.size());
                out.writeLong(entry.compressedSize());
            }

            for (DataEntry entry : compressedEntries)
                out.write(entry.compressedData());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static byte[] getStreamedData(Path file, String key) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            byte[] magic = new byte[MAGIC_BYTES.length];
            raf.readFully(magic);

            if (!Arrays.equals(MAGIC_BYTES, magic))
                throw new RuntimeException("Not a UPDH File");

            int version = raf.readInt();
            if (version != VERSION)
                throw new RuntimeException("Version Mismatch! Expected: " + VERSION + " Got: " + version + " instead");

            int entryCount = raf.readInt();

            long runningTotal = 0;
            long wantedStart = -1;
            long wantedSize = 0;
            long wantedCompressed = 0;

            for (int i = 0; i < entryCount; i++) {
                String k = raf.readUTF();
                long size = raf.readLong();
                long compressedSize = raf.readLong();

                if (k.equals(key)) {
                    wantedStart = runningTotal;
                    wantedSize = size;
                    wantedCompressed = compressedSize;
                }

                runningTotal += compressedSize;
            }

            if (wantedStart == -1)
                throw new RuntimeException("Key: " + key + ", doesn't exist in file");

            long dataStart = raf.getFilePointer();
            raf.seek(dataStart + wantedStart);

            byte[] compressed = new byte[(int) wantedCompressed];
            raf.readFully(compressed);
            return decompressData(new DataEntry(wantedSize, wantedCompressed, compressed));
        }
    }


    public static byte[] compressData(byte[] data) {
        ByteBuffer srcBuffer = MemoryUtil.memAlloc(data.length);
        ByteBuffer dstBuffer = null;
        try {
            srcBuffer.put(data).flip();
            long maxCompressedSize = Zstd.ZSTD_compressBound(data.length);
            dstBuffer = MemoryUtil.memAlloc((int) maxCompressedSize);

            long compressedSize = Zstd.ZSTD_compress(dstBuffer, srcBuffer, Zstd.ZSTD_defaultCLevel());
            if (Zstd.ZSTD_isError(compressedSize)) {
                String error = Zstd.ZSTD_getErrorName(compressedSize);
                throw new RuntimeException("Zstd Compression Error: " + error);
            }

            byte[] compressed = new byte[(int) compressedSize];
            dstBuffer.get(compressed);

            return compressed;
        } finally {
            MemoryUtil.memFree(srcBuffer);
            if (dstBuffer != null) MemoryUtil.memFree(dstBuffer);
        }
    }

    public static byte[] decompressData(DataEntry entry) {
        ByteBuffer compressedBuffer = MemoryUtil.memAlloc((int) entry.compressedSize());
        ByteBuffer dstBuffer = null;
        try {
            compressedBuffer.put(entry.compressedData()).flip();
            dstBuffer = MemoryUtil.memAlloc((int) entry.size());

            long decompressedSize = Zstd.ZSTD_decompress(dstBuffer, compressedBuffer);
            if (Zstd.ZSTD_isError(decompressedSize)) {
                String error = Zstd.ZSTD_getErrorName(decompressedSize);
                throw new RuntimeException("Zstd Decompression Error: " + error);
            }

            byte[] decompressed = new byte[(int) decompressedSize];
            dstBuffer.get(decompressed);

            return decompressed;
        } finally {
            MemoryUtil.memFree(compressedBuffer);
            if (dstBuffer != null) MemoryUtil.memFree(dstBuffer);
        }
    }

    private static List<byte[]> getDecompressedEntries(List<DataEntry> entries) {
        List<byte[]> decompressedEntries = new ArrayList<>();

        for (DataEntry entry : entries)
            decompressedEntries.add(decompressData(entry));

        return decompressedEntries;
    }


    private static List<DataEntry> getCompressedEntries(List<byte[]> entries) {
        List<DataEntry> compressedEntries = new ArrayList<>();

        for (byte[] data : entries) {
            byte[] compressed = compressData(data);
            compressedEntries.add(new DataEntry(data.length, compressed.length, compressed));
        }

        return compressedEntries;
    }

}