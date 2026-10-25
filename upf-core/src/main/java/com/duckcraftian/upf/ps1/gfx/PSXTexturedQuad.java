package com.duckcraftian.upf.ps1.gfx;

import com.duckcraftian.upf.engine.gfx.Mesh;

import static com.duckcraftian.upf.UPF.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class PSXTexturedQuad {

    private PSXTexture texture;
    private Mesh quad;

    public PSXTexturedQuad() {
        this.texture = new PSXTexture();
        quad = new Mesh(RECT_POSITIONS, RECT_COLORS, RECT_UV_COORDS, RECT_INDICES);
    }

    public void draw() {
        glActiveTexture(GL_TEXTURE0);
        texture.bind();
        quad.draw();
        texture.unbind();
    }

    public PSXTexture getTexture() {
        return texture;
    }

    public void onDispose() {
        texture.onDispose();
        quad.onDispose();
    }
}