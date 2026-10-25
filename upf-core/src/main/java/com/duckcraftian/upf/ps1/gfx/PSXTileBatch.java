package com.duckcraftian.upf.ps1.gfx;

import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.pe.disc.rooms.BackgroundTile;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

public class PSXTileBatch implements IDisposable {

    private int vao;
    private int vbo;
    private int tileCount;

    public PSXTileBatch(List<BackgroundTile> tiles) {
        tileCount = tiles.size();
        int[] data = new int[tileCount * 8];

        for (int i = 0; i < tiles.size(); i++) {
            BackgroundTile tile = tiles.get(i);
            data[i * 8] = tile.x();
            data[i * 8 + 1] = tile.y();
            data[i * 8 + 2] = tile.u();
            data[i * 8 + 3] = tile.v();
            data[i * 8 + 4] = tile.pageX();
            data[i * 8 + 5] = tile.pageY();
            data[i * 8 + 6] = tile.clutX();
            data[i * 8 + 7] = tile.clutY();
        }

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        int bytesPerTile = 8 * 4;

        IntBuffer buffer = MemoryUtil.memCallocInt(tileCount * 8);
        buffer.put(data).flip();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribIPointer(0, 4, GL_INT, bytesPerTile, 0);
        glVertexAttribDivisor(0, 1);

        glVertexAttribIPointer(1, 4, GL_INT, bytesPerTile, 16);
        glVertexAttribDivisor(1, 1);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(buffer);
    }

    public void draw() {
        glBindVertexArray(vao);
        {
            glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, tileCount);
        }
        glBindVertexArray(0);
    }

    @Override
    public void onDispose() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}