package com.duckcraftian.upf.pe.audio;

import com.duckcraftian.upf.assets.AudioSource;
import com.duckcraftian.upf.ps1.audio.SpuReverb;
import org.tinylog.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AKAOAudioSource implements AudioSource {

    private AKAOSequence sequence;
    private AKAOSequence sfx;
    private SpuReverb reverb;
    private long tickCounter;
    private float[] scratchL, scratchR;
    private float[] wetL, wetR, silence;
    private float[] sfxL, sfxR, sendL, sendR;

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
        sendL = new float[256];
        sendR = new float[256];
        sfxL = new float[256];
        sfxR = new float[256];
        wetL = new float[256];
        wetR = new float[256];
        silence = new float[256];
        queue = new ConcurrentLinkedQueue<>();
        reverb = new SpuReverb();
    }

    public void play(int key, AKAOSequence seq) {
        queue.add(() -> {
            sequence = seq;
            seq.reverb = reverb;
            reverb.setMode(seq.reverbMode);
            Logger.debug("Reverb Mode: " + seq.reverbMode);
            currentKey = key;
        });
    }

    public void setSfx(AKAOSequence sfx) {
        queue.add(() -> {
            this.sfx = sfx;
            sfx.reverb = reverb;
        });
    }

    public void startSnippet(int first, int second, int pan, int volume, int tag, int handle) {
        queue.add(() -> {
            if (sfx != null)
                sfx.startSnippet(first, second, pan, volume, tag, handle);
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

            for (int k = 0; k < due; k++) {
                scratchL[k] = 0.0f;
                scratchR[k] = 0.0f;
                sendL[k] = 0.0f;
                sendR[k] = 0.0f;
            }

            if (sequence != null) {
                sequence.tick();
                sequence.render(due, scratchL, scratchR);
                for (int k = 0; k < due; k++) {
                    sendL[k] = sendL[k] + sequence.sendL[k];
                    sendR[k] = sendR[k] + sequence.sendR[k];
                }
            }

            if (sfx != null) {
                sfx.tick();
                sfx.render(due, sfxL, sfxR);
                for (int k = 0; k < due; k++) {
                    scratchL[k] = scratchL[k] + sfxL[k];
                    scratchR[k] = scratchR[k] + sfxR[k];
                    sendL[k] = sendL[k] + sfx.sendL[k];
                    sendR[k] = sendR[k] + sfx.sendR[k];
                }
            }

            reverb.process(due, sendL, sendR, wetL, wetR);

            for (int k = 0; k < due; k++) {
                outL[frame + k] = scratchL[k] + wetL[k];
                outR[frame + k] = scratchR[k] + wetR[k];
            }

            drain();
            frame = frame + due;
            tickCounter = tickCounter + 1;
        }


        if (sequence != null)
            sequence.stepBankSlide();

    }

}