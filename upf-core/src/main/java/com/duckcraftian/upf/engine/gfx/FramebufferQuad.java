package com.duckcraftian.upf.engine.gfx;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.assets.Color;
import com.duckcraftian.upf.impl.IDisposable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class FramebufferQuad implements IDisposable {

    private Framebuffer framebuffer;
    private Mesh quad;

    private Color tint;

    public FramebufferQuad(int width, int height) {
        framebuffer = new Framebuffer(width, height);
        createQuad();
    }

    public FramebufferQuad(Framebuffer frameBuffer) {
        this.framebuffer = frameBuffer;
        createQuad();
    }

    private void createQuad() {
        tint = UPF.WHITE;
        Vector3f[] positions = {
                new Vector3f(-0.5f, 0.5f, 0.0f),
                new Vector3f(-0.5f, -0.5f, 0.0f),
                new Vector3f(0.5f, -0.5f, 0.0f),
                new Vector3f(0.5f, 0.5f, 0.0f)
        };

        Vector3f[] colors = {
                new Vector3f(1.0f, 1.0f, 1.0f),
                new Vector3f(1.0f, 1.0f, 1.0f),
                new Vector3f(1.0f, 1.0f, 1.0f),
                new Vector3f(1.0f, 1.0f, 1.0f)
        };

        int[] indices = new int[]{
                0, 1, 3, 3, 1, 2
        };

        Vector2f[] uvCoords = {
                new Vector2f(0.0f, 1.0f),
                new Vector2f(0.0f, 0.0f),
                new Vector2f(1.0f, 0.0f),
                new Vector2f(1.0f, 1.0f)
        };

        quad = new Mesh(positions, colors, uvCoords, indices);
    }


    public void begin() {
        framebuffer.begin();
    }

    public void end() {
        framebuffer.end();
    }

    public void draw() {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, framebuffer.getFramebufferTexture());
        quad.draw();
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    public void onDispose() {
        framebuffer.onDispose();
        quad.onDispose();
    }

    public Color getTint() {
        return tint;
    }
}