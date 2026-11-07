package com.duckcraftian.upf.pe.audio;

import com.duckcraftian.upf.assets.AudioSource;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AKAOAudioSource implements AudioSource {

    private AKAOSequence sequence;
    private long tickCounter;
    private float[] scratchL, scratchR;

    public Queue<Runnable> queue;
    public int currentKey;

    public AKAOAudioSource(AKAOSequence sequence) {
        this();
        this.sequence = sequence;
    }

    public AKAOAudioSource() {
        tickCounter = 0;
        currentKey = -1;
        scratchL = new float[256];
        scratchR = new float[256];
        queue = new ConcurrentLinkedQueue<>();
    }

    public void play(int key, AKAOSequence seq) {
        queue.add(() -> {
            sequence = seq;
            currentKey = key;
        });
    }

    public void stop(int key) {
        queue.add(() -> {
            if (sequence != null && currentKey == key) {
                sequence.stopAll();
                currentKey = -1;
            }
        });
    }

    public void setVolume(int key, int value) {
        queue.add(() -> {
            if (sequence != null && currentKey == key)
                sequence.setBankGain(value);
        });
    }

    public void slide(int key, int duration, int target) {
        queue.add(() -> {
            if (sequence != null && currentKey == key)
                sequence.slideBankGain(target, duration);
        });
    }

    public void slideFromTo(int key, int duration, int start, int target) {
        queue.add(() -> {
            if (sequence != null && currentKey == key)
                sequence.slideBankGainFromTo(start, target, duration);
        });
    }

    public void drain() {
        Runnable cmd = queue.poll();
        while (cmd != null) {
            cmd.run();
            cmd = queue.poll();
        }
    }

    @Override
    public int sampleRate() {
        return 44100;
    }

    @Override
    public int chunkFrames() {
        return 735;
    }

    @Override
    public void render(float[] outL, float[] outR) {
        int frame = 0;
        for (int j = 0; j < 4; j++) {
            int due = (int) (((tickCounter + 1) * sampleRate()) / 240 - (tickCounter * sampleRate()) / 240);

            if (sequence != null) {
                sequence.tick();
                sequence.render(due, scratchL, scratchR);
                for (int k = 0; k < due; k++) {
                    outL[frame + k] = scratchL[k];
                    outR[frame + k] = scratchR[k];
                }
            } else {
                for (int k = 0; k < due; k++) {
                    outL[frame + k] = 0.0f;
                    outR[frame + k] = 0.0f;
                }
            }

            drain();
            frame = frame + due;
            tickCounter = tickCounter + 1;
        }

        if (sequence != null)
            sequence.stepBankSlide();

    }

}