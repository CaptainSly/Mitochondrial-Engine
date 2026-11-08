package com.duckcraftian.upf.pe.gfx;

import com.duckcraftian.upf.pe.rooms.PSXViewport;
import com.duckcraftian.upf.ps1.PSX;
import com.duckcraftian.upf.ps1.PSXMath;

public class PECamera {

    private PSXViewport viewport;

    private int camX;
    private int camY;

    private int smoothX;
    private int smoothY;

    private int startX, startY;
    private int targetX, targetY;
    private boolean smooth;
    private int time;
    private int duration;


    // State Bools
    private boolean snap = true;
    private boolean moving = false;
    private boolean resumeAfter = true;
    private boolean held = false;

    public PECamera(PSXViewport viewport) {
        setViewport(viewport);
    }

    public void follow(int worldX, int worldY, int worldZ) {
        int[] sxsy = project(worldX, worldY, worldZ);

        int sx = sxsy[0];
        int sy = sxsy[1];

        if (snap) {
            smoothX = sx;
            smoothY = sy;
            snap = false;
        } else {
            smoothX = (smoothX + sx) / 2;
            smoothY = (smoothY + sy) / 2;
        }

        int tx = viewport.width() / 2 - 160 + smoothX;
        int ty = viewport.height() / 2 - 112 + smoothY;

        int[] target = clampToViewport(tx, ty);
        camX = target[0];
        camY = target[1];
    }

    public void beginFollowTransition(int worldX, int worldY, int worldZ, int duration, boolean smooth) {
        int[] sxsy = project(worldX, worldY, worldZ);

        int sx = sxsy[0];
        int sy = sxsy[1];

        int width = viewport.width();
        int height = viewport.height();

        int[] target = clampToViewport(width / 2 - 160 + sx, height / 2 - 112 + sy);
        begin(target, duration, smooth, true);
    }

    public void setViewport(PSXViewport viewport) {
        this.viewport = viewport;
        int[] r = viewport.rotation();
        int[] t = viewport.translation();

        camX = (viewport.minX() + viewport.maxX()) / 2;
        camY = (viewport.minY() + viewport.maxY()) / 2;
        snap = true;

        loadViewport(r, t);
    }

    public void beginScroll(int x, int y, int duration, boolean smooth) {
        begin(new int[]{x, y}, duration, smooth, false);
    }

    public void begin(int[] target, int duration, boolean smooth, boolean resumeAfter) {
        startX = camX;
        startY = camY;

        targetX = target[0];
        targetY = target[1];

        time = 1;

        this.duration = (duration == -1) ? 30 : duration;
        this.smooth = smooth;
        this.resumeAfter = resumeAfter;

        moving = true;
        held = false;
    }

    public int[] soundPosition(int worldX, int worldY, int worldZ) {
        int[] r = viewport.rotation();
        int[] t = viewport.translation();
        loadViewport(r, t);
        PSX.GTE.writeData(0, (worldY << 16) | (worldX & 0xFFFF));
        PSX.GTE.writeData(1, worldZ);
        PSX.GTE.execute(PSX.CMD_RTPS);
        int sx = (short) PSX.GTE.readData(14);
        int depth = PSX.GTE.readData(19) >> 2;
        int screenX = sx - camX + viewport.width() / 2;
        int pan = ((screenX + 40) << 7) / 400 + 64;
        if (pan < 0 || pan > 255) pan = 255;
        if (depth < 0x100) depth = 0x100;
        if (depth > 0x800) depth = 0x800;
        int d = 0x800 - depth;
        d = d * d / 0x700;
        int volume = d * 0x4F / 0x700 + 0x30;
        if (volume > 127) volume = 127;
        return new int[]{pan, volume};
    }

    public int[] toScreen(int worldX, int worldY, int worldZ) {
        loadViewport(viewport.rotation(), viewport.translation());
        PSX.GTE.writeData(0, (worldY << 16) | (worldX & 0xFFFF));
        PSX.GTE.writeData(1, worldZ);
        PSX.GTE.execute(PSX.CMD_RTPS);
        int sx = PSX.GTE.readData(14);
        int sy = PSX.GTE.readData(14) >> 16;
        return new int[] {
                sx - camX + viewport.width() / 2,
                sy - camY + viewport.height() / 2
        };
    }

    public boolean canFollow() {
        return !moving && !held;
    }

    public void step() {
        if (!moving) return;

        int dx = targetX - startX;
        int dy = targetY - startY;

        if (smooth) {
            int k = PSXMath.rcos(time * 2048 / duration + 2048) + 4096;
            camX = startX + dx * k / 8192;
            camY = startY + dy * k / 8192;
        } else {
            camX = startX + dx * time / duration;
            camY = startY + dy * time / duration;
        }

        time++;

        if (time > duration) {
            moving = false;

            if (resumeAfter)
                snap = true;
            else
                held = true;
        }
    }

    private int[] clampToViewport(int targetX, int targetY) {
        int x = Math.clamp(targetX, viewport.minX(), viewport.maxX());
        int y = Math.clamp(targetY, viewport.minY(), viewport.maxY());

        return new int[]{x, y};
    }

    private int[] project(int worldX, int worldY, int worldZ) {
        int[] r = viewport.rotation();
        int[] t = viewport.translation();
        loadViewport(r, t);

        PSX.GTE.writeData(0, (worldY << 16) | (worldX & 0xFFFF));
        PSX.GTE.writeData(1, worldZ);
        PSX.GTE.execute(PSX.CMD_RTPS);

        int sx = (short) PSX.GTE.readData(14);
        int sy = (short) (PSX.GTE.readData(14) >> 16);

        return new int[]{sx, sy};
    }

    private void loadViewport(int[] r, int[] t) {
        PSX.GTE.loadRotation(r);
        PSX.GTE.loadTranslation(t);
        PSX.GTE.writeControl(26, viewport.projectionDistance());
        PSX.GTE.writeControl(24, 160 << 16);
        PSX.GTE.writeControl(25, 112 << 16);
    }

    public int getCamX() {
        return camX;
    }

    public int getCamY() {
        return camY;
    }
}