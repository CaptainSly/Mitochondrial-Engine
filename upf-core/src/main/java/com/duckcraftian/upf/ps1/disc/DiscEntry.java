package com.duckcraftian.upf.ps1.disc;

public record DiscEntry(String name, long lba, long size, boolean isDir) {

    @Override
    public String toString() {
        return "{ name: " + name + ", lba: " + lba + ", size: " + size + ", isDir: " + isDir + "}";
    }
}