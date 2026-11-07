package com.duckcraftian.upf.pe.audio;

import com.duckcraftian.upf.assets.AudioSource;

public class AKAOAudioSource implements AudioSource {

    private AKAOSequence sequence;
    private long tickCounter;
    private float[] scratchL, scratchR;

    public AKAOAudioSource(AKAOSequence sequence) {
        this.sequence = sequence;
        tickCounter = 0;
        scratchL = new float[256];
        scratchR = new float[256];
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
            sequence.tick();
            int due = (int) (((tickCounter + 1) * sampleRate()) / 240 - (tickCounter * sampleRate()) / 240);
            sequence.render(due, scratchL, scratchR);
            for (int k = 0; k < due; k++) {
                outL[frame + k] = scratchL[k];
                outR[frame + k] = scratchR[k];
            }

            frame = frame + due;
            tickCounter = tickCounter + 1;
        }
    }
}