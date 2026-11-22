package com.duckcraftian.upf.engine.system;

import com.duckcraftian.upf.impl.IDisposable;

public interface EntryPoint extends IDisposable {

    void init();

    void update(float delta);

    void render();

    void resize(int width, int height);
}