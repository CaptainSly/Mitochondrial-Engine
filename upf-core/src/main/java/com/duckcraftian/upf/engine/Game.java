package com.duckcraftian.upf.engine;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.pe.ParasiteEve;
import com.duckcraftian.upf.pe.disc.PERipper;
import com.duckcraftian.upf.pe.gfx.PECamera;
import com.duckcraftian.upf.pe.gfx.PERenderer;
import com.duckcraftian.upf.pe.rooms.PSXViewport;
import com.duckcraftian.upf.pe.rooms.Room;
import com.duckcraftian.upf.pe.rooms.RoomLoader;
import com.duckcraftian.upf.pe.script.ScriptVM;
import com.duckcraftian.upf.ps1.disc.DiscFileSystem;
import com.duckcraftian.upf.utils.Input;
import org.lwjgl.sdl.SDLScancode;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Game implements IDisposable {

    private PERenderer renderer;
    private List<PSXViewport> viewports;

    private PECamera camera;
    private PERipper ripper;

    private Room room;

    public Path imagesFiles;

    public Game() {
        UPF.REGISTRY_MANAGER.createNamespace(ParasiteEve.NAMESPACE);
    }

    // 30Hz Update Accumulator
    private float accumulator = 0;
    private final float TICK = 1f / 30;

    public void init() {
        viewports = new ArrayList<>();
        renderer = new PERenderer();

        imagesFiles = UPF.getEngineFolder("data").resolve("images.udh");
        if (!Files.exists(imagesFiles)) {
            DiscFileSystem dfs = null;
            try {
                dfs = new DiscFileSystem("pe_disc_1");
                ripper = new PERipper();
                ripper.ripImages(dfs, imagesFiles);

            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                dfs.onDispose();
            }
        }

        loadMap("m010");
    }

    public void loadMap(String mapName) {
        Logger.debug("Attempting to load map: " + mapName);
        try {
            room = RoomLoader.loadRoom(imagesFiles, mapName);
            viewports = room.viewports();
            camera = new PECamera(viewports.get(0));

            renderer.setCameraPosition(camera.getCamX(), camera.getCamY());
            renderer.upload(room);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    public void update(float delta) {

        accumulator += delta;
        while (accumulator >= TICK) {
            accumulator -= TICK;
            tick();
        }

    }

    public void tick() {
        //Player Pad Control
        int pad = 0;
        if (Input.isKeyDown(SDLScancode.SDL_SCANCODE_W)) pad |= 0x08;

        if (Input.isKeyDown(SDLScancode.SDL_SCANCODE_S)) pad |= 0x20;

        if (Input.isKeyDown(SDLScancode.SDL_SCANCODE_A)) pad |= 0x40;

        if (Input.isKeyDown(SDLScancode.SDL_SCANCODE_D)) pad |= 0x10;

        if (Input.isKeyDown(SDLScancode.SDL_SCANCODE_LSHIFT)) pad |= 0x01;

        ScriptVM.pad = pad;
        ScriptVM.runAll();
        ScriptVM.stepFade();

        int token = ScriptVM.takePendingRoom();
        if (token != 0) {
            loadMap(String.format("m%03d", ParasiteEve.mapFromToken(token)));
            return;
        }

        camera.step();

        for (ScriptVM vm : ScriptVM.getActors())
            vm.advanceAnim();

        ScriptVM player = ScriptVM.getPlayer();
        if ((player != null) && camera.canFollow()) {
            int x = player.getPosX() >> 16;
            int y = player.getPosY() >> 16;
            int z = player.getPosZ() >> 16;
            camera.follow(x, y, z);
        }

        renderer.upload(room.models(), viewports.get(0));
    }

    public void render() {
        renderer.setCameraPosition(camera.getCamX(), camera.getCamY());
        renderer.onRender(viewports);
    }

    public void resize(int width, int height) {

    }

    @Override
    public void onDispose() {
        renderer.onDispose();
    }

}