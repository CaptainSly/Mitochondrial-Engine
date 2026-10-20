package com.duckcraftian.upf.ps1.disc;

public record RawDiscEntry(String path, long lba, long iso_size, long sectors, String mode, long bytes, String sha1) {
}