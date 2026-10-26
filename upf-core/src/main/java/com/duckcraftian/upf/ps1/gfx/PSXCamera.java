package com.duckcraftian.upf.ps1.gfx;

import com.duckcraftian.upf.pe.rooms.PSXViewport;
import com.duckcraftian.upf.ps1.PSX;

public class PSXCamera {

    private GTE gte;
    private PSXViewport viewport;

    private int camX;
    private int camY;

    public PSXCamera(GTE gte, PSXViewport viewport) {
        this.gte = gte;
        setViewport(viewport);
    }

    public void follow(int worldX, int worldY, int worldZ) {
        int[] r = viewport.rotation();
        int[] t = viewport.translation();
        loadViewport(r, t);

        gte.writeData(0, (worldY << 16) | (worldX & 0xFFFF));
        gte.writeData(1, worldZ);
        gte.execute(PSX.CMD_RTPS);

        int sx = (short) gte.readData(14);
        int sy = (short) (gte.readData(14) >> 16);

        camX = (viewport.width() / 2 - 160) + sx;
        camY = (viewport.height() / 2 - 112) + sy;
    }

    public void setViewport(PSXViewport viewport) {
        this.viewport = viewport;
        int[] r = viewport.rotation();
        int[] t = viewport.translation();

        loadViewport(r, t);
    }

    private void loadViewport(int[] r, int[] t) {
        gte.loadRotation(r);
        gte.loadTranslation(t);
        gte.writeControl(26, viewport.projectionDistance());
        gte.writeControl(24, 160 << 16);
        gte.writeControl(25, 112 << 16);
    }

    public int getCamX() {
        return camX;
    }

    public int getCamY() {
        return camY;
    }
}