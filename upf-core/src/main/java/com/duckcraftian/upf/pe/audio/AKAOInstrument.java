package com.duckcraftian.upf.pe.audio;

public record AKAOInstrument(byte[] bank,
                             int recordOffset,
                             int[] pcm,
                             int loopStart,
                             boolean loops) {

}