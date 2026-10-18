package com.duckcraftian.upf.engine.gfx;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.assets.Color;
import com.duckcraftian.upf.impl.IDisposable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class TexturedQuad implements IDisposable {

    private Texture texture;
    private Mesh quad;

    private Color tint;

    public TexturedQuad(String texturePath) {
        this.texture = new Texture(texturePath);
        this.tint = UPF.WHITE;

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

    public void draw() {
        glActiveTexture(GL_TEXTURE0);
        texture.bind();
        quad.draw();
        texture.unbind();
    }

    public Color getTint() {
        return tint;
    }

    @Override
    public void onDispose() {
        texture.onDispose();
        quad.onDispose();
    }
}