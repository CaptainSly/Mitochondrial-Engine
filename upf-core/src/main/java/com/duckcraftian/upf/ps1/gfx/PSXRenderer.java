package com.duckcraftian.upf.ps1.gfx;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.engine.gfx.Framebuffer;
import com.duckcraftian.upf.engine.gfx.FramebufferQuad;
import com.duckcraftian.upf.engine.gfx.ShaderProgram;
import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.pe.disc.rooms.BackgroundTile;
import com.duckcraftian.upf.pe.disc.rooms.PSXViewport;
import com.duckcraftian.upf.ps1.data_types.PSXImage;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class PSXRenderer implements IDisposable {

    private ShaderProgram defaultShader, tileShader, blitShader;
    private PSXTexturedQuad quad;

    private GTE gte;

    private PSXTexture tileTexture;
    private PSXTileBatch tileBatch;
    private Framebuffer psxBuffer;
    private FramebufferQuad screen;

    private int camX, camY;

    private boolean isDebug = false;

    public PSXRenderer(List<BackgroundTile> tiles) {
        // Debug
        quad = new PSXTexturedQuad();
        defaultShader = new ShaderProgram("shaders/ps1/ps1_default");
        defaultShader.createUniform("vram");
        defaultShader.createUniform("pageOrigin");
        defaultShader.createUniform("pageSize");

        // Actual
        gte = new GTE();

        tileShader = new ShaderProgram("shaders/ps1/ps1_tiles");
        tileShader.createUniform("vram");
        tileShader.createUniform("scroll");

        blitShader = new ShaderProgram("shaders/ps1/ps1_blit");
        blitShader.createUniform("screen");

        tileTexture = new PSXTexture();
        tileBatch = new PSXTileBatch(tiles);
        psxBuffer = new Framebuffer(320, 224, GL_NEAREST, GL_NEAREST, GL_RGBA8, false);
        screen = new FramebufferQuad(psxBuffer);
    }

    public void onRender(List<PSXViewport> viewports) {

        // Clamp Camera Position
        PSXViewport viewport = viewports.get(0);
        camX = Math.clamp(camX, viewport.minX(), viewport.maxX());
        camY = Math.clamp(camY, viewport.minY(), viewport.maxY());

        screen.begin();
        if (isDebug) {
            defaultShader.begin();
            glActiveTexture(GL_TEXTURE0);
            defaultShader.set("vram", 0);
            defaultShader.set("pageOrigin", new Vector2f(768, 0));
            defaultShader.set("pageSize", new Vector2f(128, 256));
            quad.draw();

            defaultShader.end();
        }

        tileShader.begin();
        {
            tileShader.set("vram", 0);
            tileShader.set("scroll", new Vector2i(160 - camX, 112 - camY));
            glActiveTexture(GL_TEXTURE0);
            tileTexture.bind();
            tileBatch.draw();
            tileTexture.unbind();
        }
        tileShader.end();
        screen.end();

        int windowW = UPF.WINDOW.getWindowWidth();
        int windowH = UPF.WINDOW.getWindowHeight();
        float scale = Math.min((windowW / 320.0f), (windowH / 224.0f));

        int w = Math.round(320 * scale);
        int h = Math.round(224 * scale);
        int x = (windowW - w) / 2;
        int y = (windowH - h) / 2;


        glViewport(x, y, w, h);
        blitShader.begin();
        blitShader.set("screen", 0);
        screen.draw();
        blitShader.end();
    }

    public void upload(PSXImage image) {
        tileTexture.upload(image);
    }

    public void setCameraPosition(int camX, int camY) {
        this.camX = camX;
        this.camY = camY;
    }

    @Override
    public void onDispose() {
        quad.onDispose();
        defaultShader.onDispose();

        screen.onDispose();

        blitShader.onDispose();

        tileShader.onDispose();
        tileTexture.onDispose();
        tileBatch.onDispose();
    }
}