package com.duckcraftian.upf.ps1.gfx;

import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.ps1.data_types.PSXImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_R16UI;
import static org.lwjgl.opengl.GL30.GL_RED_INTEGER;

public class PSXTexture implements IDisposable {

    public static final int WIDTH = 1024;
    public static final int HEIGHT = 512;

    private int textureId;

    public PSXTexture() {
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R16UI, WIDTH, HEIGHT, 0, GL_RED_INTEGER, GL_UNSIGNED_SHORT, (ByteBuffer) null);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void upload(PSXImage image) {
        ByteBuffer data = MemoryUtil.memAlloc(image.pixels().length);
        try {
            data.put(image.pixels()).flip();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexSubImage2D(GL_TEXTURE_2D, 0, image.x(), image.y(), image.width(), image.height(), GL_RED_INTEGER, GL_UNSIGNED_SHORT, data);
            glBindTexture(GL_TEXTURE_2D, 0);
        } finally {
            MemoryUtil.memFree(data);
        }
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    public void onDispose() {
        glDeleteTextures(textureId);
    }
}