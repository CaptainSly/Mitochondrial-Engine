package com.duckcraftian.upf.pe.gfx;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.engine.gfx.Framebuffer;
import com.duckcraftian.upf.engine.gfx.FramebufferQuad;
import com.duckcraftian.upf.engine.gfx.ShaderProgram;
import com.duckcraftian.upf.engine.gfx.TexturedQuad;
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
import com.duckcraftian.upf.system.Utils;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.duckcraftian.upf.pe.ParasiteEve.SCENE_OBJECT_MODELKEY;
import static com.duckcraftian.upf.ps1.PSX.GTE;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.*;

public class PERenderer implements IDisposable {

    private ShaderProgram defaultShader, tileShader, blitShader, polygonShader, textShader;
    private PSXTexturedQuad quad;

    private PSXTexture tileTexture;
    private PETileBatch tileBatch;

    private PSXTriangleBatch semiBatch;
    private PSXTriangleBatch triangleBatch;
    private PSXTriangleBatch shadowBatch;
    private PSXTriangleBatch textBatch;

    private Framebuffer psxBuffer;
    private FramebufferQuad screen;

    private Vector3f fadeColor = new Vector3f();
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
        shadowBatch = new PSXTriangleBatch(8);
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
                tileBatch.draw(); // ALL THESE SQUARES MAKE A CIRCLE
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

                glEnable(GL_BLEND);
                glBlendEquation(GL_FUNC_REVERSE_SUBTRACT);
                glBlendFunc(GL_ONE, GL_ONE);
                glDepthMask(false);
                shadowBatch.draw();
                glDepthMask(true);
                glBlendEquation(GL_FUNC_ADD);
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
        fadeColor.set(ScriptVM.fadeLevel[0] / 255.0f, ScriptVM.fadeLevel[1] / 255.0f, ScriptVM.fadeLevel[2] / 255.0f);
        blitShader.set("fade", fadeColor);
        screen.draw();
        blitShader.end();
    }

    public void drawMovie(TexturedQuad quad) {
        int windowW = UPF.WINDOW.getWindowWidth();
        int windowH = UPF.WINDOW.getWindowHeight();
        float scale = Math.min(windowW / 320.0f, windowH / 240.0f);
        int w = Math.round(320 * scale);
        int h = Math.round(240 * scale);
        int x0 = (windowW - w) / 2;
        int y0 = (windowH - h) / 2;

        glDisable(GL_DEPTH_TEST);
        glViewport(x0, y0, w, h);
        blitShader.begin();
        {
            blitShader.set("screen", 0);
            fadeColor.set(0, 0, 0);
            blitShader.set("fade", fadeColor);
            quad.draw();
        }
        blitShader.end();

        glEnable(GL_DEPTH_TEST);
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
        List<Integer> allShadow = new ArrayList<>();
        GTE.writeControl(29, 0x155);
        GTE.writeControl(30, 0x100);

        PEModel gunModel = ScriptVM.sceneObjectModels.get((int) SCENE_OBJECT_MODELKEY);

        for (ScriptVM vm : ScriptVM.getActors()) {
            PEModel model = models.get(vm.getType());
            if (model == null) {
                continue;
            }

            PEAnimation anim = vm.getAnimation();
            if (anim == null) {
                continue;
            }

            int[] rot = vm.modelMatrix();
            int[] pos = {vm.getPosX() >> 16, vm.getPosY() >> 16, vm.getPosZ() >> 16};
            int[][] pose = model.pose(anim, vm.getFrame(), rot, pos);
            int[] screen = model.project(pose, viewport.rotation(), viewport.translation());
            int[] lit = model.light(pose, vm.getShade(), vm.getLightNegativeY(), vm.getLightPositiveY(), ScriptVM.roomLightR, ScriptVM.roomLightG, ScriptVM.roomLightB);
            int[] q = shadowQuad(vm, pose, viewport);

            if (q != null) {
                allShadow.addAll(Utils.intArrayToList(q));
            }

            if (vm.getType() == 0 && gunModel != null && (ScriptVM.moveLock & 2) == 0) {
                int[][] gunPose = {pose[18], pose[18]};
                int[] gunScreen = gunModel.project(gunPose, viewport.rotation(), viewport.translation());
                int[] gunLight = gunModel.light(gunPose, vm.getShade(), vm.getLightNegativeY(), vm.getLightPositiveY(), ScriptVM.roomLightR, ScriptVM.roomLightG, ScriptVM.roomLightB);
                PSXOrderingTable gunOT = new PSXOrderingTable(4096, gunModel.getN34() + gunModel.getN28() + gunModel.getN24() + gunModel.getN1c());
                gunModel.sortIntoOT(gunScreen, gunOT);
                for (int value : gunModel.buildTriangles(gunOT, gunScreen, gunLight, allSemi))
                    all.add(value);
            }

            PSXOrderingTable ot = new PSXOrderingTable(4096, model.getN34() + model.getN28() + model.getN24() + model.getN1c());
            model.sortIntoOT(screen, ot);
            for (int value : model.buildTriangles(ot, screen, lit, allSemi))
                all.add(value);
        }

        int[] tris = new int[all.size()];
        for (int i = 0; i < all.size(); i++)
            tris[i] = all.get(i);

        int[] semiTris = new int[allSemi.size()];
        for (int i = 0; i < allSemi.size(); i++)
            semiTris[i] = allSemi.get(i);

        int[] shadowTris = new int[allShadow.size()];
        for (int i = 0; i < allShadow.size(); i++)
            shadowTris[i] = allShadow.get(i);

        upload(tris);
        semiBatch.upload(semiTris);
        shadowBatch.upload(shadowTris);
    }

    public void upload(PSXImage image) {
        tileTexture.upload(image);
    }

    public void upload(int[] tris) {
        triangleBatch.upload(tris);
    }

    public int[] shadowQuad(ScriptVM vm, int[][] pose, PSXViewport viewport) {
        if ((vm.getEntityFlags() & 0x400) != 0) return null;

        int r = vm.getShadowRadius();
        int[] m = pose[vm.getShadowMatrixIndex()];

        int ax = m[2];
        int az = m[8];
        int nx, nz;

        if (ax == 0 && az == 0) {
            nx = 0;
            nz = 4096;
        } else {
            double len = Math.sqrt(ax * ax + az * az);
            nx = (int) (ax * 4096 / len);
            nz = (int) (az * 4096 / len);
        }

        int[] basis = new int[]{-nz, 0, nx, 0, 4096, 0, nx, 0, nz};
        int y = vm.getPosY() >> 16;
        int tx = m[9];
        int tz = m[11];

        int[] rot = PSXMath.mulMatrix(viewport.rotation(), basis);
        int[] t = PSXMath.mulVector(viewport.rotation(), tx, y, tz);
        t[0] += viewport.translation()[0];
        t[1] += viewport.translation()[1];
        t[2] += viewport.translation()[2];
        GTE.loadRotation(rot);
        GTE.loadTranslation(t);

        int[][] c = {{-r, r, 0, 64}, {r, r, 63, 64}, {-r, -r, 0, 127}, {r, -r, 63, 127}};
        int[] px = new int[4];
        int[] py = new int[4];
        int maxDepth = -1;

        for (int i = 0; i < 4; i++) {
            GTE.writeData(0, (0 << 16) | (c[i][0] & 0xFFFF));
            GTE.writeData(1, c[i][1]);
            GTE.execute(PSX.CMD_RTPS);

            int sxy = GTE.readData(14);
            px[i] = (short) sxy;
            py[i] = (short) (sxy >> 16);
            maxDepth = Math.max(maxDepth, GTE.readData(19) >> 2);
        }

        if (maxDepth < 0 || maxDepth >= 0x1000) return null;

        int g = vm.getShadowIntensity() / 2;
        int rgb = g | (g << 8) | (g << 16);

        int[] order = {0, 1, 2, 1, 3, 2};
        int[] out = new int[48];

        for (int k = 0; k < 6; k++) {
            int i = order[k];
            out[8 * k + 0] = px[i];
            out[8 * k + 1] = py[i];
            out[8 * k + 2] = c[i][2];
            out[8 * k + 3] = c[i][3];
            out[8 * k + 4] = 0xCB;
            out[8 * k + 5] = 0x7210;
            out[8 * k + 6] = maxDepth;
            out[8 * k + 7] = rgb;
        }

        return out;
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
        shadowBatch.onDispose();
        textBatch.onDispose();
        tileBatch.onDispose();
    }
}