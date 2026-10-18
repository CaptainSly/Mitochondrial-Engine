package com.duckcraftian.upf.engine.gfx;

public class TextureRegion {

    private Texture parentTexture;
    private final float u1, v1, u2, v2;
    private final short index;

    public TextureRegion(Texture parentTexture, short index, float u1, float v1, float u2, float v2) {
        this.parentTexture = parentTexture;
        this.u1 = u1;
        this.u2 = u2;
        this.v1 = v1;
        this.v2 = v2;
        this.index = index;
    }

    public void bind() {
        parentTexture.bind();
    }

    public void unbind() {
        parentTexture.unbind();
    }

    public float getV2() {
        return v2;
    }

    public float getU2() {
        return u2;
    }

    public float getV1() {
        return v1;
    }

    public float getU1() {
        return u1;
    }

    public int getTextureId() {
        return parentTexture.getTextureId();
    }

    public String getTexturePath() {
        return parentTexture.getTexturePath();
    }

    public short getIndex() {
        return index;
    }
}