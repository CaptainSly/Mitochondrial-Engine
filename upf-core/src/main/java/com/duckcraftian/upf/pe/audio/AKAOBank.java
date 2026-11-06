package com.duckcraftian.upf.pe.audio;

import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.duckcraftian.upf.ps1.audio.SpuAdpcm.decodeSample;
import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;
import static com.duckcraftian.upf.system.DataTypeHelper.u8;

public class AKAOBank {

    private List<Integer> loopStart;
    private List<Boolean> loops;

    public AKAOBank() {
        loopStart = new ArrayList<>();
        loops = new ArrayList<>();
    }

    public List<List<Integer>> loadBank(byte[] bank, boolean printFirst) {
        long spuDest = u32LE(bank, 0x10);
        long adpcmBytes = u32LE(bank, 0x14);
        long first = u32LE(bank, 0x18);
        long last = u32LE(bank, 0x1C);

        if (last == 0)
            last = 0x100;

        long count = last - first;
        long adpcmStart = 0x40 + 0x40 * count;

        List<List<Integer>> samples = new ArrayList<>();

        for (int k = 0; k < count; k++) {
            long record = 0x40 + 0x40 * k;
            long startAddr = u32LE(bank, (int) record);
            long rel = startAddr - spuDest;

            long loopAddr = u32LE(bank, (int) (record + 4));
            int offset = (int) (adpcmStart + rel);

            List<Integer> pcm = decodeSample(bank, offset);
            int size = pcm.size();
            loopStart.add((int) ((loopAddr - startAddr) / 16 * 28));

            int lastBlock = offset + (size / 28 - 1) * 16;
            loops.add((u8(bank, lastBlock + 1) & 2) != 0);

            if (printFirst)
                Logger.debug("k=" + k + ", startAddr=" + startAddr + ", rel=" + rel + "/" + (rel % 16));

            samples.add(pcm);

        }

        return samples;
    }

    public List<Integer> getLoopStart() {
        return loopStart;
    }

    public List<Boolean> getLoops() {
        return loops;
    }
}