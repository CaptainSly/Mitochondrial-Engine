package com.duckcraftian.upf.system.plugins;

import com.duckcraftian.upf.impl.IDisposable;

public interface IPlugin extends IDisposable {

    void onInitialize();

    void onPreInitialize(PluginContext context);

    void onPostInitialize();

    void onEnable();

    boolean canDisable();

    void onDisable();

    void onShutdown();

}