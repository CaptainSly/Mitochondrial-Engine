package com.duckcraftian.upf.engine.gfx;

import com.duckcraftian.upf.impl.IDisposable;

public class TextureAtlas implements IDisposable {

    private final TextureRegion[][] regions;
    private final Texture atlasTexture;

    private final int pixelWidth, pixelHeight;
    private final int numCols, numRows;

    public TextureAtlas(Texture atlasTexture, int pixelWidth, int pixelHeight) {
        this.atlasTexture = atlasTexture;

        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;

        int textureWidth = atlasTexture.getWidth();
        int textureHeight = atlasTexture.getHeight();

        regions = new TextureRegion[textureWidth / pixelWidth][textureHeight / pixelHeight];

        numRows = textureHeight / pixelHeight;
        numCols = textureWidth / pixelWidth;

        sliceAtlas(textureWidth, textureHeight);
    }

    public TextureAtlas(String texturePath, int pixelWidth, int pixelHeight) {
        atlasTexture = new Texture(texturePath);

        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;

        int textureWidth = atlasTexture.getWidth();
        int textureHeight = atlasTexture.getHeight();

        regions = new TextureRegion[textureWidth / pixelWidth][textureHeight / pixelHeight];

        numRows = textureHeight / pixelHeight;
        numCols = textureWidth / pixelWidth;

        sliceAtlas(textureWidth, textureHeight);
    }

    private void sliceAtlas(int textureWidth, int textureHeight) {


        for (int y = 0; y < textureHeight; y += pixelHeight) {
            for (int x = 0; x < textureWidth; x += pixelWidth) {
                int col = x / pixelWidth;
                int row = y / pixelHeight;

                // stbi_set_flip_vertically_on_load(true) (see Texture.java) flips the pixel
                // buffer vertically at load time, so GL's V=0 samples the SOURCE FILE'S BOTTOM
                // row, not its top. row (as scanned top-down here) needs mirroring before it's
                // turned into a V range, or "row 0" ends up sampling the wrong band of the GPU
                // texture entirely — same correction applied in raycaster.fs's atlasUVMin/Max,
                // which independently re-derives UVs from just the index, not these floats.
                int flippedRow = numRows - 1 - row;

                float u1 = (float) x / textureWidth;
                float v1 = (float) (flippedRow * pixelHeight) / textureHeight;
                float u2 = (float) (x + pixelWidth) / textureWidth;
                float v2 = (float) ((flippedRow + 1) * pixelHeight) / textureHeight;

                // Row-major, matching raycaster.fs's atlasUVMin/atlasUVMax decode
                // (col = index % atlasColumns, row = index / atlasColumns) exactly.
                short index = (short) (row * numCols + col);

                regions[col][row] = new TextureRegion(atlasTexture, index, u1, v1, u2, v2);
            }
        }

    }

    public TextureRegion getRegion(int x, int y) {
        return regions[x][y];
    }

    public Texture getAtlasTexture() {
        return atlasTexture;
    }

    public TextureRegion[][] getRegions() {
        return regions;
    }

    public int getPixelWidth() {
        return pixelWidth;
    }

    public int getPixelHeight() {
        return pixelHeight;
    }

    public int getNumCols() {
        return numCols;
    }

    public int getNumRows() {
        return numRows;
    }

    @Override
    public void onDispose() {
        atlasTexture.onDispose();
    }
}