package com.duckcraftian.upf.utils;

@FunctionalInterface
public interface ProgressListener {

    void onProgress(int fileNumber, int fileCount, String fileName);

}