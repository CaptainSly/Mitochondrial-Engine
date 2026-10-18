package com.duckcraftian.upf.engine.system;

import com.duckcraftian.upf.system.plugins.*;
import com.duckcraftian.upf.system.registries.PluginRegistryManager;
import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.system.registries.Registry;
import com.duckcraftian.upf.system.registries.RegistryManager;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class PluginLoader implements IDisposable {

    private final Path pluginDirectory;
    private final PluginRegistryManager pluginRegistryManager;
    private final RegistryManager registryManager;

    private final Map<String, IPlugin> loadedPlugins;
    private final Map<String, UPFPlugin> pluginMetadata;
    private final Map<String, URLClassLoader> pluginClassLoaders;
    private final List<String> loadOrder;

    public PluginLoader(Path pluginDirectory, PluginRegistryManager pluginRegistryManager, RegistryManager registryManager) {
        this.pluginDirectory = pluginDirectory;
        this.pluginRegistryManager = pluginRegistryManager;
        this.registryManager = registryManager;

        this.loadedPlugins = new HashMap<>();
        this.pluginMetadata = new HashMap<>();
        this.pluginClassLoaders = new HashMap<>();
        this.loadOrder = new ArrayList<>();
    }

    public void preInitializePlugins() throws CircularDependencyException {
        List<Path> jarFiles;

        try (Stream<Path> stream = Files.list(pluginDirectory)) {
            jarFiles = new ArrayList<>(stream.filter(p -> p.toString().endsWith(".jar")).toList());
        } catch (IOException e) {
            Logger.error("Failed to read plugin directory: " + e.getMessage());
            return;
        }

        if (jarFiles.isEmpty()) {
            Logger.info("No plugins found in: " + pluginDirectory);
            return;
        }

        jarFiles.forEach(this::loadJar);

        resolveDependencyOrder();
        for (String pluginId : loadOrder) {
            IPlugin plugin = loadedPlugins.get(pluginId);
            UPFPlugin meta = pluginMetadata.get(pluginId);

            PluginContext context = new PluginContext(pluginRegistryManager, registryManager, pluginId);

            plugin.onPreInitialize(context);
            plugin.onEnable();

            pluginRegistryManager.getLoadedPlugins().add(meta.id());
            Logger.info("Loaded plugin: " + meta.name() + " v" + meta.version());
        }

    }

    public void initializePlugins() {
        for (String pluginId : loadOrder) {
            IPlugin plugin = loadedPlugins.get(pluginId);
            plugin.onInitialize();
            Logger.info("Initialize Plugin: " + pluginId);
        }
    }

    public void postInitializePlugins() {
        for (String pluginId : loadOrder) {
            IPlugin plugin = loadedPlugins.get(pluginId);
            plugin.onPostInitialize();
            Logger.info("Post Initialized Plugin: " + pluginId);
        }
    }

    private void disposePlugins() {
        for (String pluginId : loadOrder) {
            IPlugin plugin = loadedPlugins.get(pluginId);
            plugin.onDispose();
            Logger.info("Disposed Plugin: " + pluginId);
        }
    }

    private void loadJar(Path jarPath) {
        try {
            URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, getClass().getClassLoader());

            try (JarFile jar = new JarFile(jarPath.toFile())) {
                jar.entries().asIterator().forEachRemaining(entry -> {
                    if (entry.getName().endsWith(".class")) {
                        String className = entry.getName().replace("/", ".").replace(".class", "");
                        tryLoadPluginClass(classLoader, className);
                    }
                });
            }

        } catch (IOException e) {
            Logger.error("Failed to load JAR: " + jarPath + " - " + e.getMessage());
        }

    }

    private void tryLoadPluginClass(URLClassLoader classLoader, String className) {
        try {
            Class<?> clazz = classLoader.loadClass(className);

            UPFPlugin annotation = clazz.getAnnotation(UPFPlugin.class);
            if (annotation == null) return;

            if (!IPlugin.class.isAssignableFrom(clazz)) {
                Logger.warn(className + " has @PEPlugin but does not implement IPlugin");
                return;
            }

            IPlugin plugin = (IPlugin) clazz.getDeclaredConstructor().newInstance();

            pluginMetadata.put(annotation.id(), annotation);
            loadedPlugins.put(annotation.id(), plugin);
            pluginClassLoaders.put(annotation.id(), classLoader);

            Logger.info("Discovered Plugin: " + annotation.name() + " v" + annotation.version());

        } catch (Exception e) {
            Logger.warn("Failed to load class: " + className + " - " + e.getMessage());
        }
    }

    private void resolveDependencyOrder() throws CircularDependencyException {
        List<String> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> inProgress = new HashSet<>();

        for (String pluginId : pluginMetadata.keySet()) {
            if (!visited.contains(pluginId)) {
                visit(pluginId, sorted, visited, inProgress);
            }
        }

        loadOrder.addAll(sorted);
    }

    private void visit(String pluginId, List<String> sorted, Set<String> visited, Set<String> inProgress) throws CircularDependencyException {
        if (inProgress.contains(pluginId)) {
            throw new CircularDependencyException("Circular dependency detected involving plugin: " + pluginId);
        }

        if (visited.contains(pluginId)) return;

        inProgress.add(pluginId);

        UPFPlugin meta = pluginMetadata.get(pluginId);
        if (meta != null) {
            for (String dep : meta.dependencies()) {
                if (!pluginMetadata.containsKey(dep))
                    throw new RuntimeException("Plugin " + pluginId + " requires missing dependency: " + dep);

                visit(dep, sorted, visited, inProgress);
            }
        }

        inProgress.remove(pluginId);
        visited.add(pluginId);
        sorted.add(pluginId);
    }

    public Path getPluginDirectory() {
        return pluginDirectory;
    }

    public PluginRegistryManager getRegistryManager() {
        return pluginRegistryManager;
    }

    public Map<String, IPlugin> getLoadedPlugins() {
        return loadedPlugins;
    }

    public Map<String, UPFPlugin> getPluginMetadata() {
        return pluginMetadata;
    }

    public Map<String, URLClassLoader> getPluginClassLoaders() {
        return pluginClassLoaders;
    }

    @Override
    public void onDispose() {
        disposePlugins();
        for (URLClassLoader loader : getPluginClassLoaders().values()) {
            try {
                loader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}