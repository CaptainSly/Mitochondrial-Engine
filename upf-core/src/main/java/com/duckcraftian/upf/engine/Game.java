package com.duckcraftian.upf.engine;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.pe.ParasiteEve;
import com.duckcraftian.upf.ps1.disc.BinLoader;

import java.io.IOException;

public class Game implements IDisposable {



    public Game() {
        UPF.REGISTRY_MANAGER.createNamespace(ParasiteEve.NAMESPACE);
    }

    public void init() {
    }


    public void update(float delta) {

    }

    public void render() {

    }

    public void resize(int width, int height) {

    }

    @Override
    public void onDispose() {

    }

}