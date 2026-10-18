package com.duckcraftian.upf.engine.gfx;

import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.utils.FileUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture implements IDisposable {

    private int textureId;
    private String texturePath;

    private int width, height;

    public Texture(int width, int height, ByteBuffer buf) {
        this.texturePath = "";
        generateTexture(width, height, buf);
    }

    public Texture(String texturePath) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.texturePath = texturePath;
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            String resolvedPath;
            if (Path.of(texturePath).isAbsolute()) {
                resolvedPath = texturePath;
            } else {
                try {
                    resolvedPath = FileUtils.resolveAssetFile(texturePath).getAbsolutePath();
                } catch (URISyntaxException e) {
                    throw new RuntimeException("Could not resolve texture resource: " + texturePath, e);
                }
            }

            stbi_set_flip_vertically_on_load(true);
            ByteBuffer buf = stbi_load(resolvedPath, w, h, channels, 4);
            if (buf == null) throw new RuntimeException("Image file: " + texturePath + " could not be loaded");

            width = w.get();
            height = h.get();

            generateTexture(width, height, buf);
            stbi_image_free(buf);
        }
    }

    public Texture(byte[] rawImageBytes) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.texturePath = "";
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer fileBuffer = MemoryUtil.memAlloc(rawImageBytes.length);
            fileBuffer.put(rawImageBytes).flip();

            stbi_set_flip_vertically_on_load(true);
            ByteBuffer pixelBuffer = stbi_load_from_memory(fileBuffer, w, h, channels, 4);
            if (pixelBuffer == null) throw new RuntimeException("Image could not be decoded from memory");

            width = w.get();
            height = h.get();

            generateTexture(width, height, pixelBuffer);
            stbi_image_free(pixelBuffer);
            MemoryUtil.memFree(fileBuffer);
        }
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void generateTexture(int width, int height, ByteBuffer buf) {
        textureId = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, textureId);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
    }

    public int getTextureId() {
        return textureId;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public void onDispose() {
        glDeleteTextures(textureId);
    }
}