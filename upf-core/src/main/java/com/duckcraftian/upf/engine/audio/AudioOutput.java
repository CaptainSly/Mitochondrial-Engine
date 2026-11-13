package com.duckcraftian.upf.engine.audio;

import com.duckcraftian.upf.assets.AudioSource;
import com.duckcraftian.upf.impl.IDisposable;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.tinylog.Logger;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

public class AudioOutput implements IDisposable {

    private AudioSource source;
    private Thread audioThread;
    private long device;
    private long context;
    private int alSource;
    private int produced;
    private int maxBuffers;
    private short[] pcm;
    private int[] buffers;
    private float[] chunkL;
    private float[] chunkR;

    private volatile boolean stopRequested;
    private ShortBuffer buff;

    public AudioOutput(AudioSource source) {
        this.source = source;
        buffers = new int[4];
        pcm = new short[source.chunkFrames() * 2];
        buff = MemoryUtil.memAllocShort(pcm.length);
        chunkL = new float[source.chunkFrames()];
        chunkR = new float[source.chunkFrames()];
        produced = 0;
    }

    public void start(int limit) {
        maxBuffers = limit;
        audioThread = new Thread(() -> {
            try {
                run();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        audioThread.start();
    }

    public void stop() throws InterruptedException {
        stopRequested = true;
        if (audioThread != null)
            audioThread.join();
    }

    public void waitUntilDone() throws InterruptedException {
        if (audioThread != null)
            audioThread.join();
    }

    public void run() throws InterruptedException {
        device = ALC10.alcOpenDevice((ByteBuffer) null);
        if (device == 0)
            throw new RuntimeException("No AUDIO Device");

        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        long context = alcCreateContext(device, (IntBuffer) null);
        alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCaps);

        alSource = alGenSources();
        for (int i = 0; i < 4; i++)
            buffers[i] = alGenBuffers();

        for (int i = 0; i < 4; i++) {
            fill(buffers[i]);
            alSourceQueueBuffers(alSource, buffers[i]);
        }
        alSourcePlay(alSource);

        Logger.debug("AL ERROR: " + alGetError() + ", AL BUFFERS QUEUED: " + alGetSourcei(alSource, AL_BUFFERS_QUEUED) + ", AL SOURCE STATE: " + alGetSourcei(alSource, AL_SOURCE_STATE));

        while (produced < maxBuffers && !stopRequested) {
            int processed = alGetSourcei(alSource, AL_BUFFERS_PROCESSED);
            while (processed > 0) {
                int b = alSourceUnqueueBuffers(alSource);
                fill(b);
                alSourceQueueBuffers(alSource, b);
                processed = processed - 1;
            }

            if (alGetSourcei(alSource, AL_SOURCE_STATE) != AL_PLAYING)
                alSourcePlay(alSource);

            Thread.sleep(5);

        }

        while (alGetSourcei(alSource, AL_SOURCE_STATE) == AL_PLAYING && !stopRequested)
            Thread.sleep(5);

        alSourceStop(alSource);
        alSourcei(alSource, AL_BUFFER, 0);
        alDeleteSources(alSource);

        for (int i = 0; i < 4; i++)
            alDeleteBuffers(buffers[i]);

        alcMakeContextCurrent(0);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    private void fill(int buffer) {
        buff.clear();
        source.render(chunkL, chunkR);
        for (int k = 0; k < source.chunkFrames(); k++) {
            pcm[2 * k] = (short) (Math.clamp(chunkL[k], -1.0, 1.0) * 32767);
            pcm[2 * k + 1] = (short) (Math.clamp(chunkR[k], -1.0, 1.0) * 32767);
        }
        int peak = 0;
        for (int k = 0; k < pcm.length; k++) {
            if (Math.abs(pcm[k]) > peak) peak = Math.abs(pcm[k]);
        }

        buff.put(pcm).flip();
        alBufferData(buffer, AL_FORMAT_STEREO16, buff, source.sampleRate());
        produced = produced + 1;
    }

    @Override
    public void onDispose() {
        try {
            stop();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        MemoryUtil.memFree(buff);
    }
}