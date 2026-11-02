package com.duckcraftian.upf.pe.gfx;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.engine.gfx.Framebuffer;
import com.duckcraftian.upf.engine.gfx.FramebufferQuad;
import com.duckcraftian.upf.engine.gfx.ShaderProgram;
import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.pe.rooms.BackgroundTile;
import com.duckcraftian.upf.pe.rooms.PSXViewport;
import com.duckcraftian.upf.pe.rooms.Room;
import com.duckcraftian.upf.pe.script.ScriptVM;
import com.duckcraftian.upf.ps1.PSX;
import com.duckcraftian.upf.ps1.PSXMath;
import com.duckcraftian.upf.ps1.data_types.PSXImage;
import com.duckcraftian.upf.ps1.gfx.PSXOrderingTable;
import com.duckcraftian.upf.ps1.gfx.PSXTexture;
import com.duckcraftian.upf.ps1.gfx.PSXTexturedQuad;
import com.duckcraftian.upf.ps1.gfx.PSXTriangleBatch;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.tinylog.Logger;

import java.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class PERenderer implements IDisposable {

    private ShaderProgram defaultShader, tileShader, blitShader, polygonShader, textShader;
    private PSXTexturedQuad quad;

    private PSXTexture tileTexture;
    private PETileBatch tileBatch;

    private PSXTriangleBatch semiBatch;
    private PSXTriangleBatch triangleBatch;
    private PSXTriangleBatch textBatch;

    private Framebuffer psxBuffer;
    private FramebufferQuad screen;

    private int camX, camY;

    private boolean isDebug = false;

    public PERenderer() {
        // Debug
        quad = new PSXTexturedQuad();
        defaultShader = new ShaderProgram("shaders/ps1/ps1_default");
        defaultShader.createUniform("vram");
        defaultShader.createUniform("pageOrigin");
        defaultShader.createUniform("pageSize");

        textShader = new ShaderProgram("shaders/ps1/ps1_text");
        textShader.createUniform("vram");
        textShader.createUniform("scroll");

        tileShader = new ShaderProgram("shaders/ps1/ps1_tiles");
        tileShader.createUniform("vram");
        tileShader.createUniform("scroll");

        blitShader = new ShaderProgram("shaders/ps1/ps1_blit");
        blitShader.createUniform("screen");
        blitShader.createUniform("fade");

        polygonShader = new ShaderProgram("shaders/ps1/ps1_poly");
        polygonShader.createUniform("vram");
        polygonShader.createUniform("scroll");
        polygonShader.createUniform("opacity");

        tileTexture = new PSXTexture();
        triangleBatch = new PSXTriangleBatch(8);
        semiBatch = new PSXTriangleBatch(8);
        textBatch = new PSXTriangleBatch(7);
        psxBuffer = new Framebuffer(320, 224, GL_NEAREST, GL_NEAREST, GL_RGBA8, true);
        screen = new FramebufferQuad(psxBuffer);
    }

    public void onRender(List<PSXViewport> viewports) {

        // Clamp Camera Position
        PSXViewport viewport = viewports.get(ScriptVM.viewportIndex);

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
                polygonShader.set("scroll", new Vector2i(viewport.width() / 2 - camX, viewport.height() / 2 - camY));
                polygonShader.set("opacity", 1.0f);
                glActiveTexture(GL_TEXTURE0);
                tileTexture.bind();
                triangleBatch.draw();

                polygonShader.set("opacity", 0.5f);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glDepthMask(false);
                semiBatch.draw();
                glDepthMask(true);
                glDisable(GL_BLEND);

                tileTexture.unbind();
            }
            polygonShader.end();

            textShader.begin();
            {
                textShader.set("vram", 0);
                textShader.set("scroll", new Vector2i(0, 0));
                glActiveTexture(GL_TEXTURE0);
                tileTexture.bind();
                textBatch.draw();
                tileTexture.unbind();
            }
            textShader.end();

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
        blitShader.set("fade", ScriptVM.fadeLevel / 255.0f);
        screen.draw();
        blitShader.end();
    }

    public void upload(List<BackgroundTile> tiles) {
        if (tileBatch != null)
            tileBatch.onDispose();

        tileBatch = new PETileBatch(tiles);
    }

    public void uploadText(int[] verts) {
        textBatch.upload(verts);
    }

    public void upload(Room room) {
        for (PSXImage image : room.images())
            upload(image);

        upload(room.tiles());
        upload(room.models(), room.viewports().get(0));
    }

    public void upload(Map<Integer, PEModel> models, PSXViewport viewport) {
        List<Integer> all = new ArrayList<>();
        List<Integer> allSemi = new ArrayList<>();
        PSX.GTE.writeControl(29, 0x155);
        PSX.GTE.writeControl(30, 0x100);

        for (ScriptVM vm : ScriptVM.getActors()) {
            PEModel model = models.get(vm.getType());
            if (model == null) {
                continue;
            }

            PEAnimation anim = vm.getAnimation();
            if (anim == null) {
                continue;
            }

            int[] rot = PSXMath.rotMatrixYXZ(0, vm.getRotY(), 0);
            int[] pos = {vm.getPosX() >> 16, vm.getPosY() >> 16, vm.getPosZ() >> 16};
            int[][] pose = model.pose(anim, vm.getFrame(), rot, pos);
            int[] screen = model.project(pose, viewport.rotation(), viewport.translation());
            PSXOrderingTable ot = new PSXOrderingTable(1024, model.getN34() + model.getN28() + model.getN24() + model.getN1c());
            model.sortIntoOT(screen, ot);
            for (int value : model.buildTriangles(ot, screen, allSemi))
                all.add(value);
        }

        int[] tris = new int[all.size()];
        for (int i = 0; i < all.size(); i++)
            tris[i] = all.get(i);

        int[] semiTris = new int[allSemi.size()];
        for (int i = 0; i < allSemi.size(); i++)
            semiTris[i] = allSemi.get(i);

        upload(tris);
        semiBatch.upload(semiTris);
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

    @Override
    public void onDispose() {
        quad.onDispose();
        screen.onDispose();

        defaultShader.onDispose();
        blitShader.onDispose();
        tileShader.onDispose();
        polygonShader.onDispose();
        textShader.onDispose();

        tileTexture.onDispose();

        triangleBatch.onDispose();
        semiBatch.onDispose();
        textBatch.onDispose();
        tileBatch.onDispose();
    }
}