package com.duckcraftian.upf.engine.gfx;

import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.utils.Utils;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class Mesh implements IDisposable {

    private int vaoId;
    private List<Integer> vboList;

    private int numVertices;

    public Mesh(Vector3f[] positions, Vector3f[] colors, Vector2f[] uvCoords, int[] indices) {
        this.numVertices = indices.length;
        vboList = new ArrayList<>();

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Positions
        int vboId = glGenBuffers();
        vboList.add(vboId);
        FloatBuffer positionsBuffer = MemoryUtil.memCallocFloat(positions.length * 3);
        positionsBuffer.put(Utils.convertVec3ArrayToFloatArray(positions));
        positionsBuffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, positionsBuffer, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL15.GL_FLOAT, false, 0, 0);

        // Texture Coordinates
        vboId = glGenBuffers();
        vboList.add(vboId);
        FloatBuffer uvCoordsBuffer = MemoryUtil.memCallocFloat(uvCoords.length * 2);
        uvCoordsBuffer.put(Utils.convertVec2ArrayToFloatArray(uvCoords));
        uvCoordsBuffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, uvCoordsBuffer, GL_STATIC_DRAW);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);

        // Index
        vboId = glGenBuffers();
        vboList.add(vboId);
        IntBuffer indicesBuffer = MemoryUtil.memCallocInt(indices.length);
        indicesBuffer.put(indices);
        indicesBuffer.flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);


        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(positionsBuffer);
        MemoryUtil.memFree(uvCoordsBuffer);
        MemoryUtil.memFree(indicesBuffer);
    }

    public void draw() {
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, numVertices, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    @Override
    public void onDispose() {
        vboList.forEach(GL30::glDeleteBuffers);
        glDeleteVertexArrays(vaoId);
    }

}