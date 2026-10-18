package com.duckcraftian.upf.system.plugins;


import com.duckcraftian.upf.system.registries.PluginRegistryManager;
import com.duckcraftian.upf.system.registries.RegistryManager;

public record PluginContext(PluginRegistryManager pluginRegistryManager, RegistryManager registryManager, String pluginId) {

}