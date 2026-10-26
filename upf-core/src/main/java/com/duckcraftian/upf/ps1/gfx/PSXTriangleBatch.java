package com.duckcraftian.upf.ps1.gfx;

import com.duckcraftian.upf.impl.IDisposable;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.*;

public class PSXTriangleBatch implements IDisposable {

    private int vao;
    private int vbo;
    private int vertexCount;

    public PSXTriangleBatch() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glEnableVertexAttribArray(0);
        glVertexAttribIPointer(0, 4, GL_INT, 28, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribIPointer(1, 2, GL_INT, 28, 16);
        glEnableVertexAttribArray(2);
        glVertexAttribIPointer(2, 1, GL_INT, 28, 24);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void upload(int[] tris) {
        vertexCount = tris.length / 7;
        IntBuffer buffer = MemoryUtil.memAllocInt(tris.length);
        buffer.put(tris).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        MemoryUtil.memFree(buffer);
    }

    public void draw() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    @Override
    public void onDispose() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }
}