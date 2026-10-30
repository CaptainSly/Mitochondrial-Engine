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

    public static final String MAP_NAME = "m001";

    public Game() {
        UPF.REGISTRY_MANAGER.createNamespace(ParasiteEve.NAMESPACE);
    }

    // 30Hz Update Accumulator
    private float accumulator = 0;
    private final float TICK = 1f / 30;

    public void init() {
        viewports = new ArrayList<>();

        Path imagesFiles = UPF.getEngineFolder("data").resolve("images.udh");
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

        try {
            room = RoomLoader.loadRoom(imagesFiles, MAP_NAME);
            viewports = room.viewports();

            renderer = new PERenderer();

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
        ScriptVM.runAll();
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