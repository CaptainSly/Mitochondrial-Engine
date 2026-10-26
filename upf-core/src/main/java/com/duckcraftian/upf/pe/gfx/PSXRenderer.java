package com.duckcraftian.upf.pe.gfx;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.engine.gfx.Framebuffer;
import com.duckcraftian.upf.engine.gfx.FramebufferQuad;
import com.duckcraftian.upf.engine.gfx.ShaderProgram;
import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.pe.rooms.Actor;
import com.duckcraftian.upf.pe.rooms.BackgroundTile;
import com.duckcraftian.upf.pe.rooms.PSXViewport;
import com.duckcraftian.upf.pe.rooms.Room;
import com.duckcraftian.upf.ps1.PSXMath;
import com.duckcraftian.upf.ps1.data_types.PSXImage;
import com.duckcraftian.upf.ps1.gfx.*;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class PSXRenderer implements IDisposable {

    private ShaderProgram defaultShader, tileShader, blitShader, polygonShader;
    private PSXTexturedQuad quad;

    private GTE gte;

    private PSXTexture tileTexture;
    private PSXTileBatch tileBatch;
    private PSXTriangleBatch triangleBatch;
    private Framebuffer psxBuffer;
    private FramebufferQuad screen;

    private int camX, camY;

    private boolean isDebug = false;

    public PSXRenderer() {
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

        polygonShader = new ShaderProgram("shaders/ps1/ps1_poly");
        polygonShader.createUniform("vram");
        polygonShader.createUniform("scroll");

        tileTexture = new PSXTexture();
        triangleBatch = new PSXTriangleBatch();
        psxBuffer = new Framebuffer(320, 224, GL_NEAREST, GL_NEAREST, GL_RGBA8, true);
        screen = new FramebufferQuad(psxBuffer);
    }

    public void onRender(List<PSXViewport> viewports) {

        // Clamp Camera Position
        PSXViewport viewport = viewports.get(0);
        camX = Math.clamp(camX, viewport.minX(), viewport.maxX());
        camY = Math.clamp(camY, viewport.minY(), viewport.maxY());

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        screen.begin();
        {
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

            polygonShader.begin();
            {
                polygonShader.set("vram", 0);
                polygonShader.set("scroll", new Vector2i(160 - camX, 112 - camY));
                glActiveTexture(GL_TEXTURE0);
                tileTexture.bind();
                triangleBatch.draw();
                tileTexture.unbind();
            }
            polygonShader.end();
        }
        screen.end();
        glDisable(GL_DEPTH_TEST);

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

    public void upload(List<BackgroundTile> tiles) {
        tileBatch = new PSXTileBatch(tiles);
    }
    public void upload(Room room) {
        for (PSXImage image : room.images())
            upload(image);

        upload(room.tiles());
        upload(room.actors(), room.viewports().get(0));
    }

    public void upload(List<Actor> actors, PSXViewport viewport) {
        List<Integer> all = new ArrayList<>();
        gte.writeControl(29, 0x155);
        gte.writeControl(30, 0x100);

        for (Actor actor : actors) {
            int[] rot = PSXMath.rotMatrixYXZ(0, actor.yaw(), 0);
            int[][] pose = actor.model().pose(actor.animation(), 0, rot, actor.position());
            int[] screen = actor.model().project(gte, pose, viewport.rotation(), viewport.translation());
            PSXOrderingTable ot = new PSXOrderingTable(1024, actor.model().getN34() + actor.model().getN28());
            actor.model().sortIntoOT(gte, screen, ot);
            for (int value : actor.model().buildTriangles(ot, screen))
                all.add(value);
        }

        int[] tris = new int[all.size()];
        for (int i = 0; i < all.size(); i++)
            tris[i] = all.get(i);

        upload(tris);
    }

    public void upload(PSXImage image) {
        tileTexture.upload(image);
    }

    public void upload(int[] tris) {
        triangleBatch.upload(tris);
    }

    public void setCameraPosition(int camX, int camY) {
        this.camX = camX;
        this.camY = camY;
    }

    public GTE getGTE() {
        return gte;
    }

    @Override
    public void onDispose() {
        quad.onDispose();
        screen.onDispose();

        defaultShader.onDispose();
        blitShader.onDispose();
        tileShader.onDispose();
        polygonShader.onDispose();

        tileTexture.onDispose();

        triangleBatch.onDispose();
        tileBatch.onDispose();
    }
}