package com.duckcraftian.upf.system.registries;

import java.util.ArrayList;
import java.util.List;

public class PluginRegistryManager {

    private final List<String> loadedPlugins;

    public PluginRegistryManager() {
        loadedPlugins = new ArrayList<>();
    }

    public List<String> getLoadedPlugins() {
        return loadedPlugins;
    }

}