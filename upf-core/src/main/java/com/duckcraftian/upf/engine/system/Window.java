package com.duckcraftian.upf.engine.system;

import com.duckcraftian.upf.impl.IDisposable;
import org.lwjgl.sdl.*;

public class Window implements IDisposable {

    private String windowTitle;
    private int windowWidth, windowHeight;

    private final long windowPointer;
    private final long openglContextPointer;

    public Window(String windowTitle, int windowWidth, int windowHeight) {
        this.windowTitle = windowTitle;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;

        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_CONTEXT_MAJOR_VERSION, 4);
        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_CONTEXT_MINOR_VERSION, 6);
        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_CONTEXT_PROFILE_MASK, SDLVideo.SDL_GL_CONTEXT_PROFILE_CORE);
        SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_DOUBLEBUFFER, 1);

        windowPointer = SDLVideo.SDL_CreateWindow(windowTitle, windowWidth, windowHeight, SDLVideo.SDL_WINDOW_OPENGL);


        if (windowPointer == 0) {
            throw new RuntimeException("Failed to create SDL Window");
        }

        SDLVideo.SDL_SetWindowMinimumSize(windowPointer, 640, 480);
        SDLVideo.SDL_SetWindowResizable(windowPointer, true);

        openglContextPointer = SDLVideo.SDL_GL_CreateContext(windowPointer);

        if (openglContextPointer == 0)
            throw new RuntimeException("Failed to create SDL OpenGL Context");
    }

    public void onResize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
    }

    public void setWindowTitle(String title) {
        SDLVideo.SDL_SetWindowTitle(windowPointer, title);
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public long getWindowPointer() {
        return windowPointer;
    }

    public long getOpenGLContextPointer() {
        return openglContextPointer;
    }

    @Override
    public void onDispose() {
        SDLVideo.SDL_GL_DestroyContext(openglContextPointer);
        SDLVideo.SDL_DestroyWindow(windowPointer);
    }
}