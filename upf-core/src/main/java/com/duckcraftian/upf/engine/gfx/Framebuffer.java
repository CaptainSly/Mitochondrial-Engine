package com.duckcraftian.upf.engine.gfx;

import com.duckcraftian.upf.impl.IDisposable;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL30.*;

public class Framebuffer implements IDisposable {

    private final int framebufferObject;
    private final int framebufferTexture;
    private final int framebufferRenderBufferObject;
    private int width, height;
    private boolean hasDepth;

    public Framebuffer(int width, int height) {
        this.width = width;
        this.height = height;
        framebufferObject = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferObject);

        framebufferTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, framebufferTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, MemoryUtil.NULL);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, framebufferTexture, 0);
        glBindTexture(GL_TEXTURE_2D, 0);

        framebufferRenderBufferObject = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, framebufferRenderBufferObject);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, framebufferRenderBufferObject);

        if (!(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE)) {
            throw new RuntimeException("Failed to create framebuffer");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        this.hasDepth = true;
    }

    public Framebuffer(int width, int height, int minFilter, int magFilter, int colorFormat, boolean hasDepth) {
        this.width = width;
        this.height = height;
        this.hasDepth = hasDepth;
        framebufferObject = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferObject);

        framebufferTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, framebufferTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, colorFormat, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, MemoryUtil.NULL);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, magFilter);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, framebufferTexture, 0);
        glBindTexture(GL_TEXTURE_2D, 0);

        if (hasDepth) {
            framebufferRenderBufferObject = glGenRenderbuffers();
            glBindRenderbuffer(GL_RENDERBUFFER, framebufferRenderBufferObject);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, framebufferRenderBufferObject);
        } else {
            framebufferRenderBufferObject = 0;
        }

        if (!(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE)) {
            throw new RuntimeException("Failed to create framebuffer");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void begin() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferObject);
        glViewport(0, 0, width, height);
        glClearColor(1, 0, 0, 1);

        if (hasDepth)
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        else
            glClear(GL_COLOR_BUFFER_BIT);
    }

    public void end() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void beginOverlay() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferObject);
        glViewport(0, 0, width, height);
    }

    public int getFramebufferObject() {
        return framebufferObject;
    }

    public int getFramebufferTexture() {
        return framebufferTexture;
    }

    public int getFramebufferRenderBufferObject() {
        return framebufferRenderBufferObject;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean hasDepth() {
        return hasDepth;
    }

    @Override
    public void onDispose() {
        glDeleteTextures(framebufferTexture);
        glDeleteRenderbuffers(framebufferRenderBufferObject);
        glDeleteFramebuffers(framebufferObject);
    }
}