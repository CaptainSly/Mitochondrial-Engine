package com.duckcraftian.upf.serialization;

public record DataEntry(long size, long compressedSize, byte[] compressedData) {
}