package com.duckcraftian.upf.engine;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.pe.ParasiteEve;
import com.duckcraftian.upf.pe.disc.PERipper;
import com.duckcraftian.upf.pe.disc.SquareImageParser;
import com.duckcraftian.upf.pe.rooms.BackgroundTile;
import com.duckcraftian.upf.pe.rooms.PSXViewport;
import com.duckcraftian.upf.pe.disc.RoomParser;
import com.duckcraftian.upf.pe.rooms.Room;
import com.duckcraftian.upf.pe.rooms.RoomLoader;
import com.duckcraftian.upf.ps1.PSXMath;
import com.duckcraftian.upf.ps1.data_types.PSXImage;
import com.duckcraftian.upf.ps1.disc.DiscFileSystem;
import com.duckcraftian.upf.pe.gfx.PSXAnimation;
import com.duckcraftian.upf.ps1.gfx.PSXCamera;
import com.duckcraftian.upf.pe.gfx.PSXModel;
import com.duckcraftian.upf.ps1.gfx.PSXOrderingTable;
import com.duckcraftian.upf.pe.gfx.PSXRenderer;
import com.duckcraftian.upf.serialization.UPDHSerializer;
import com.duckcraftian.upf.system.Utils;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game implements IDisposable {

    private PSXRenderer renderer;
    private List<PSXViewport> viewports;

    private PSXCamera camera;
    private PERipper ripper;

    public static final String MAP_NAME = "m001";

    public Game() {
        UPF.REGISTRY_MANAGER.createNamespace(ParasiteEve.NAMESPACE);
    }

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
            Room room = RoomLoader.loadRoom(imagesFiles, MAP_NAME);
            viewports = room.viewports();

            renderer = new PSXRenderer();
            camera = new PSXCamera(renderer.getGTE(), viewports.get(0));
            camera.follow(-4, 65, -4813);

            renderer.setCameraPosition(camera.getCamX(), camera.getCamY());
            renderer.upload(room);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    public void update(float delta) {

    }

    public void render() {
        renderer.onRender(viewports);
    }

    public void resize(int width, int height) {

    }

    @Override
    public void onDispose() {
        renderer.onDispose();
    }

}