package com.duckcraftian.upf.engine.system;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.engine.Game;
import com.duckcraftian.upf.engine.audio.AudioOutput;
import com.duckcraftian.upf.system.event.EventBus;
import com.duckcraftian.upf.system.registries.PluginRegistryManager;
import com.duckcraftian.upf.system.registries.RegistryManager;
import com.duckcraftian.upf.utils.Input;
import com.google.gson.GsonBuilder;
import org.lwjgl.opengl.GL;
import org.lwjgl.sdl.*;
import org.tinylog.Logger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLVideo.SDL_GL_SetSwapInterval;
import static org.lwjgl.sdl.SDLVideo.SDL_GL_SwapWindow;
import static org.lwjgl.sdl.SDL_Event.create;


public class Engine {

    private Window window;
    private PluginRegistryManager pluginRegistryManager;
    private RegistryManager registryManager;

    private PluginLoader pluginLoader;
    private EventBus eventBus;
    private Game game;

    private boolean isRunning = false;

    public Engine() {
        if (!UPF.ENGINE_PATH.toFile().exists()) {
            UPF.createDataFolder();
        }

        this.pluginRegistryManager = new PluginRegistryManager();
        UPF.PLUGIN_REGISTRY_MANAGER = pluginRegistryManager;

        this.registryManager = new RegistryManager();
        UPF.REGISTRY_MANAGER = registryManager;

        pluginLoader = new PluginLoader(UPF.getEngineFolder("mods"), UPF.PLUGIN_REGISTRY_MANAGER, UPF.REGISTRY_MANAGER);

        UPF.GSON = new GsonBuilder().setPrettyPrinting().create();

        eventBus = new EventBus();
        UPF.EVENT_BUS = eventBus;
    }

    public void run() {
        init();

        loop();

        dispose();
    }

    private void init() {
        Logger.debug(UPF.WINDOW_TITLE + ": " + UPF.getVersion());

        if (!SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO | SDLInit.SDL_INIT_GAMEPAD)) {
            Logger.error("SDL could not be initialized! SDL Error: " + SDLError.SDL_GetError());
        }

        window = new Window(UPF.WINDOW_TITLE + " " + UPF.getVersion(), 1280, 720);

        pluginLoader.preInitializePlugins();

        UPF.WINDOW = window;

        SDL_GL_SetSwapInterval(1);

        GL.createCapabilities();

        pluginLoader.initializePlugins();

        // Is POST Initialization needed in UPF?
        // Post Initialization has been commented out until either it's required
        // or a new step is put in above creating the game.
//        pluginLoader.postInitializePlugins();

        game = new Game();
        game.init();
        isRunning = true;
    }

    private void loop() {
        SDL_Event event = create();

        long previousTime = System.nanoTime();

        while (isRunning) {
            long currentTime = System.nanoTime();
            float delta = (currentTime - previousTime) / 1E9f;
            previousTime = currentTime;

            Input.update();

            while (SDL_PollEvent(event)) {
                int type = event.type();

                if (type == SDL_EVENT_MOUSE_MOTION) {
                    SDL_MouseMotionEvent motionEvent = event.motion();
                    Input.onMouseMotion(motionEvent.xrel(), motionEvent.yrel(), motionEvent.x(), motionEvent.y());
                }

                if (type == SDL_EVENT_MOUSE_BUTTON_DOWN || type == SDL_EVENT_MOUSE_BUTTON_UP) {
                    SDL_MouseButtonEvent buttonEvent = event.button();
                    boolean isDown = type == SDL_EVENT_MOUSE_BUTTON_DOWN;
                    Input.onMouseButtonDown(buttonEvent.button() - 1, isDown);
                }

                if (type == SDL_EVENT_MOUSE_WHEEL) {
                    SDL_MouseWheelEvent wheelEvent = event.wheel();
                    Input.onMouseWheel(wheelEvent.x(), wheelEvent.y());
                }

                if (type == SDL_EVENT_KEY_DOWN || type == SDL_EVENT_KEY_UP) {
                    int scancode = event.key().scancode();
                    boolean down = (type == SDL_EVENT_KEY_DOWN);
                    Input.onKeyDown(scancode, down);
                }

                if (type == SDL_EVENT_GAMEPAD_ADDED || type == SDL_EVENT_GAMEPAD_REMOVED) {
                    SDL_GamepadDeviceEvent deviceEvent = event.gdevice();
                    boolean wasAdded = type == SDL_EVENT_GAMEPAD_ADDED;
                }

                if (type == SDL_EVENT_GAMEPAD_REMAPPED) {
                    SDL_GamepadDeviceEvent deviceEvent = event.gdevice();
                }

                if (type == SDL_EVENT_GAMEPAD_UPDATE_COMPLETE) {
                    SDL_GamepadDeviceEvent deviceEvent = event.gdevice();
                }

                if (type == SDL_EVENT_GAMEPAD_SENSOR_UPDATE) {
                    SDL_GamepadSensorEvent sensorEvent = event.gsensor();
                }

                if (type == SDL_EVENT_GAMEPAD_BUTTON_DOWN || type == SDL_EVENT_GAMEPAD_BUTTON_UP) {
                    SDL_GamepadButtonEvent buttonEvent = event.gbutton();
                    boolean isDown = type == SDL_EVENT_GAMEPAD_BUTTON_DOWN;

                }

                if (type == SDL_EVENT_GAMEPAD_TOUCHPAD_DOWN || type == SDL_EVENT_GAMEPAD_TOUCHPAD_UP) {
                    SDL_GamepadTouchpadEvent touchEvent = event.gtouchpad();
                    boolean isDown = type == SDL_EVENT_GAMEPAD_TOUCHPAD_DOWN;
                }

                if (type == SDL_EVENT_GAMEPAD_AXIS_MOTION) {
                    SDL_GamepadAxisEvent axisEvent = event.gaxis();
                }

                if (type == SDL_EVENT_GAMEPAD_TOUCHPAD_MOTION) {
                    SDL_GamepadTouchpadEvent touchEvent = event.gtouchpad();
                }

                if (type == SDL_EVENT_WINDOW_RESIZED || type == SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED) {
                    int width = event.window().data1();
                    int height = event.window().data2();
                    window.onResize(width, height);
                    game.resize(width, height);
                }

                if (type == SDL_EVENT_QUIT)
                    isRunning = false;
            }

            glViewport(0, 0, window.getWindowWidth(), window.getWindowHeight());
            glClearColor(0, 0, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Update
            game.update(delta);

            // Render
            game.render();

            SDL_GL_SwapWindow(window.getWindowPointer());
        }

    }

    private void dispose() {
        if (UPF.AUDIO != null)
            UPF.AUDIO.onDispose();

        Logger.info("Disposing");
        pluginLoader.onDispose();

        window.onDispose();
        SDLInit.SDL_Quit();
    }

    static void main() {
        Engine e = new Engine();
        e.run();
    }

}