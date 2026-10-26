package com.duckcraftian.upf.ps1.gfx;

import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.pe.rooms.BackgroundTile;
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
        int[] data = new int[tileCount * 9];

        for (int i = 0; i < tiles.size(); i++) {
            BackgroundTile tile = tiles.get(i);
            data[i * 9] = tile.x();
            data[i * 9 + 1] = tile.y();
            data[i * 9 + 2] = tile.u();
            data[i * 9 + 3] = tile.v();
            data[i * 9 + 4] = tile.pageX();
            data[i * 9 + 5] = tile.pageY();
            data[i * 9 + 6] = tile.clutX();
            data[i * 9 + 7] = tile.clutY();
            data[i * 9 + 8] = tile.orderIndex();
        }

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        int bytesPerTile = 9 * 4;

        IntBuffer buffer = MemoryUtil.memCallocInt(tileCount * 9);
        buffer.put(data).flip();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribIPointer(0, 4, GL_INT, bytesPerTile, 0);
        glVertexAttribDivisor(0, 1);

        glEnableVertexAttribArray(1);
        glVertexAttribIPointer(1, 4, GL_INT, bytesPerTile, 16);
        glVertexAttribDivisor(1, 1);

        glEnableVertexAttribArray(2);
        glVertexAttribIPointer(2, 1, GL_INT, bytesPerTile, 32);
        glVertexAttribDivisor(2, 1);


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