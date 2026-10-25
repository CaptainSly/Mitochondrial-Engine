package com.duckcraftian.upf.engine;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.pe.ParasiteEve;
import com.duckcraftian.upf.pe.disc.PERipper;
import com.duckcraftian.upf.pe.disc.SquareImageParser;
import com.duckcraftian.upf.pe.disc.rooms.BackgroundTile;
import com.duckcraftian.upf.pe.disc.rooms.PSXViewport;
import com.duckcraftian.upf.pe.disc.rooms.RoomParser;
import com.duckcraftian.upf.ps1.data_types.PSXImage;
import com.duckcraftian.upf.ps1.disc.DiscFileSystem;
import com.duckcraftian.upf.ps1.gfx.PSXRenderer;
import com.duckcraftian.upf.serialization.UPDHSerializer;
import com.duckcraftian.upf.system.Utils;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Game implements IDisposable {

    private PSXRenderer renderer;
    private List<BackgroundTile> tiles;
    private List<PSXViewport> viewports;

    public static final String MAP_NAME = "m362";

    public Game() {
        UPF.REGISTRY_MANAGER.createNamespace(ParasiteEve.NAMESPACE);
    }

    public void init() {
        tiles = new ArrayList<>();
        viewports = new ArrayList<>();

        Path imagesFiles = UPF.getEngineFolder("data").resolve("images.udh");
        if (!Files.exists(imagesFiles)) {
            DiscFileSystem dfs = null;
            try {
                dfs = new DiscFileSystem("pe_disc_1");
                PERipper ripper = new PERipper();
                ripper.ripImages(dfs, imagesFiles);

            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                dfs.onDispose();
            }
        }

        try {
            byte[] bytes = UPDHSerializer.getStreamedData(imagesFiles, "rooms/" + MAP_NAME + "/background");
            tiles = RoomParser.parseBackgroundBlock(bytes, 0);
            viewports = RoomParser.parseViewports(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        renderer = new PSXRenderer(tiles);
        renderer.setCameraPosition(160, 112);

        String[] imageKeys = {"images/" + MAP_NAME + "/scratch", "images/" + MAP_NAME + "/texture"};
        for (String key : imageKeys) {
            try {
                byte[] bytes = UPDHSerializer.getStreamedData(imagesFiles, key);
                List<PSXImage> images = SquareImageParser.convertBytesToImages(bytes);
                for (PSXImage image : images)
                    renderer.upload(image);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Logger.debug("projectionDistance: " + viewports.get(0).projectionDistance() + ", rotation: " + Utils.intArrayToString(viewports.get(0).rotation()) + " translation: " + Utils.intArrayToString(viewports.get(0).translation()) + " image: " + viewports.get(0).width() + "x" + viewports.get(0).height() + ", minX: " + viewports.get(0).minX() + ", minY: " + viewports.get(0).minY() + ", maxX: " + viewports.get(0).maxX() + ", maxY: " + viewports.get(0).maxY());

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