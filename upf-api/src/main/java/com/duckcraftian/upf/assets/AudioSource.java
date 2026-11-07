package com.duckcraftian.upf.assets;

public interface AudioSource {

    int sampleRate();

    int chunkFrames();

    void render(float[] outL, float[] outR);

}