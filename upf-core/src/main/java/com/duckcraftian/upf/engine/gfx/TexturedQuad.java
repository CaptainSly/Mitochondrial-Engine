package com.duckcraftian.upf.engine.gfx;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.assets.Color;
import com.duckcraftian.upf.impl.IDisposable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

import static com.duckcraftian.upf.UPF.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class TexturedQuad implements IDisposable {

    private Texture texture;
    private Mesh quad;

    private Color tint;

    public TexturedQuad(String texturePath) {
        this.texture = new Texture(texturePath);
        quad = new Mesh(RECT_POSITIONS, RECT_COLORS, RECT_UV_COORDS, RECT_INDICES);
    }

    public TexturedQuad(int width, int height, ByteBuffer pixels) {
        this.texture = new Texture(width, height, pixels);
        this.texture.setSmooth();
        quad = new Mesh(RECT_POSITIONS, RECT_COLORS, RECT_UV_COORDS, RECT_INDICES);
    }


    public void update(ByteBuffer pixels) {
        texture.update(pixels);
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