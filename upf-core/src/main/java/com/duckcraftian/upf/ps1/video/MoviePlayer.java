package com.duckcraftian.upf.ps1.video;

import com.duckcraftian.upf.engine.gfx.TexturedQuad;
import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.pe.audio.AKAOAudioSource;
import com.duckcraftian.upf.pe.gfx.PERenderer;
import org.lwjgl.system.MemoryUtil;
import org.tinylog.Logger;

import java.nio.ByteBuffer;
import java.util.List;

public class MoviePlayer implements IDisposable {

    public static final int FMV_RGBA_SIZE = 320 * 240 * 4;

    private List<byte[]> frames;
    private ByteBuffer rgba;
    private AKAOAudioSource source;
    private TexturedQuad quad;

    private int shown;
    public boolean done;
    public boolean skippable;
    public boolean blocking = false;

    private byte[] movie;
    private short[][] pcm;
    private int idx;

    public void start(byte[] movie, AKAOAudioSource src, boolean canSkip) {
        this.movie = movie;
        source = src;
        frames = StrDemux.demux(movie);
        pcm = MovieAudio.decode(movie);
        rgba = MemoryUtil.memCalloc(FMV_RGBA_SIZE);
        quad = new TexturedQuad(320, 240, rgba);
        skippable = canSkip;
        shown = -1;
        done = false;
        source.startMovie(pcm[0], pcm[1]);
        step();
    }

    public void step() {
        idx = (int) (source.movieClock() * 15 / 44100);
        if (idx >= frames.size())
            done = true;
        else if (idx != shown) {
            int[] pixels = Mdec.decodeImage(BSDecoder.decodeFrame(frames.get(idx), 320, 240), 320, 240);
            Mdec.toRGBA(pixels, 320, 240, rgba);
            quad.update(rgba);
            shown = idx;
            Logger.debug("FMV Show: " + idx + ", clock: " + source.movieClock());
        }

    }

    public void stop() {
        source.stopMovie();
        if (rgba != null) MemoryUtil.memFree(rgba);

        quad.onDispose();
    }

    public int elapsed() {
        return shown + 1;
    }

    public void draw(PERenderer renderer) {
        renderer.drawMovie(quad);
    }

    @Override
    public void onDispose() {
        stop();
    }
}