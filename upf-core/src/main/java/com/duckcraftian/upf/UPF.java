package com.duckcraftian.upf;

import com.duckcraftian.upf.assets.Color;
import com.duckcraftian.upf.engine.system.Window;
import com.duckcraftian.upf.system.event.EventBus;
import com.duckcraftian.upf.system.registries.PluginRegistryManager;
import com.duckcraftian.upf.system.registries.RegistryManager;
import com.google.gson.Gson;

import java.io.File;
import java.nio.file.Path;

public class UPF {

    public static final String WINDOW_TITLE = "Mitochondrial Engine";
    public static final String MAJOR = "0";
    public static final String MINOR = "1";
    public static final String BUGFIX = "2";
    public static final String CODENAME = "Resonance";

    // ------------ COLORS -------------
    public static final Color WHITE = new Color(1, 1, 1, 1);
    public static final Color BLACK = new Color(0, 0, 0, 1);
    public static final Color RED = new Color(1, 0, 0, 1);
    public static final Color GREEN = new Color(0, 1, 0, 1);
    public static final Color BLUE = new Color(0, 0, 1, 1);

    public static String getVersion() {
        return String.format("%s.%s.%s-%s", MAJOR, MINOR, BUGFIX, CODENAME);
    }

    public static final String ENGINE_FOLDER = "mitochondria/";
    public static final String[] ENGINE_FOLDERS = {
            "data", "config", "saves", "libs", "mods"
    };

    public static final Path ENGINE_PATH = Path.of(System.getProperty("user.dir"), ENGINE_FOLDER);

    public static void createDataFolder() {
        File engineFolder = ENGINE_PATH.toFile();
        engineFolder.mkdir();

        for (String dir : ENGINE_FOLDERS)
            getEngineFolder(dir).toFile().mkdirs();
    }

    public static Path getEngineFolder(String path) {
        return Path.of(ENGINE_PATH.toString(), path);
    }

    public static Gson GSON;

    public static Window WINDOW;
    public static PluginRegistryManager PLUGIN_REGISTRY_MANAGER;
    public static RegistryManager REGISTRY_MANAGER;
    public static EventBus EVENT_BUS;


}