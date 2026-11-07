package com.duckcraftian.upf.engine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.duckcraftian.upf.pe.audio.PEAudio;
import com.duckcraftian.upf.pe.gfx.PEModel;
import org.lwjgl.sdl.SDLScancode;
import org.tinylog.Logger;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.pe.ParasiteEve;
import com.duckcraftian.upf.pe.disc.PERipper;
import com.duckcraftian.upf.pe.disc.RoomParser;
import com.duckcraftian.upf.pe.disc.SquareImageParser;
import com.duckcraftian.upf.pe.gfx.PECamera;
import com.duckcraftian.upf.pe.gfx.PERenderer;
import com.duckcraftian.upf.pe.gfx.PEText;
import com.duckcraftian.upf.pe.rooms.PSXViewport;
import com.duckcraftian.upf.pe.rooms.Room;
import com.duckcraftian.upf.pe.rooms.RoomLoader;
import com.duckcraftian.upf.pe.script.ScriptVM;
import com.duckcraftian.upf.ps1.data_types.PSXImage;
import com.duckcraftian.upf.ps1.disc.DiscFileSystem;
import com.duckcraftian.upf.serialization.UPDHSerializer;
import com.duckcraftian.upf.system.Utils;
import com.duckcraftian.upf.utils.Input;

import static com.duckcraftian.upf.pe.ParasiteEve.PEAUDIO;
import static com.duckcraftian.upf.system.DataTypeHelper.s16LE;

public class Game implements IDisposable {

    private PERenderer renderer;
    private List<PSXViewport> viewports;


    private PECamera camera;
    private PERipper ripper;

    private Room room;

    public Path imagesFiles;

    private boolean confirmPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;

    public Game() {
        UPF.REGISTRY_MANAGER.createNamespace(ParasiteEve.NAMESPACE);
    }

    // 30Hz Update Accumulator
    private float accumulator = 0;
    private final float TICK = 1f / 30;

    public void init() {
        viewports = new ArrayList<>();
        renderer = new PERenderer();

        imagesFiles = UPF.getEngineFolder("data").resolve("pe.udh");
        if (!Files.exists(imagesFiles)) {
            DiscFileSystem dfs = null, dfs2 = null;
            try {
                dfs = new DiscFileSystem("pe_disc_1");
                dfs2 = new DiscFileSystem("pe_disc_2");
                ripper = new PERipper();
                ripper.ripImages(dfs, dfs2, imagesFiles);

            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                dfs.onDispose();
                dfs2.onDispose();
            }
        }

        PEAUDIO = new PEAudio();
        try {
            PEAUDIO.init(imagesFiles);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            byte[] fontImages = UPDHSerializer.getStreamedData(imagesFiles, "text/font/images");
            for (PSXImage image : SquareImageParser.convertBytesToImages(fontImages))
                renderer.upload(image);

            byte[] fontMetrics = UPDHSerializer.getStreamedData(imagesFiles, "text/font/metrics");
            PEText.metrics = fontMetrics;
            byte[] normals = UPDHSerializer.getStreamedData(imagesFiles, "render/normals");

            PEModel.normals = new int[240 * 3];
            for (int i = 0; i < 240; i++) {
                PEModel.normals[3 * i] = s16LE(normals, 8 * i);
                PEModel.normals[3 * i + 1] = s16LE(normals, 8 * i + 2);
                PEModel.normals[3 * i + 2] = s16LE(normals, 8 * i + 4);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        loadMap("m010");
    }

    public void loadMap(String mapName) {
        try {
            room = RoomLoader.loadRoom(imagesFiles, mapName);
            viewports = room.viewports();
            camera = new PECamera(viewports.get(0));

            renderer.setCameraPosition(camera.getCamX(), camera.getCamY());
            renderer.upload(room);

            Logger.debug("Loading map: " + mapName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void update(float delta) {

        if (!ScriptVM.openMessages.isEmpty()) {
            if (Input.isKeyJustDown(SDLScancode.SDL_SCANCODE_F))
                confirmPressed = true;

            if (Input.isKeyJustDown(SDLScancode.SDL_SCANCODE_W))
                upPressed = true;

            if (Input.isKeyJustDown(SDLScancode.SDL_SCANCODE_S))
                downPressed = true;
        }

        if (Input.isKeyJustDown(SDLScancode.SDL_SCANCODE_F8))
            ScriptVM.dumpActors();

        accumulator += delta;
        while (accumulator >= TICK) {
            accumulator -= TICK;
            tick();
        }

    }

    public void tick() {
        // Player Pad Control
        int pad = 0;
        if (Input.isKeyDown(SDLScancode.SDL_SCANCODE_W))
            pad |= 0x08;

        if (Input.isKeyDown(SDLScancode.SDL_SCANCODE_S))
            pad |= 0x20;

        if (Input.isKeyDown(SDLScancode.SDL_SCANCODE_A))
            pad |= 0x40;

        if (Input.isKeyDown(SDLScancode.SDL_SCANCODE_D))
            pad |= 0x10;

        if (Input.isKeyDown(SDLScancode.SDL_SCANCODE_LSHIFT))
            pad |= 0x01;

        if (Input.isKeyDown(SDLScancode.SDL_SCANCODE_F))
            pad |= (0x100 | 0x20000000);


        if (confirmPressed) {
            ScriptVM.confirm();
            confirmPressed = false;
        }

        if (upPressed) {
            ScriptVM.moveChoice(-1);
            upPressed = false;
        }

        if (downPressed) {
            ScriptVM.moveChoice(1);
            downPressed = false;
        }

        ScriptVM.pad = pad;
        ScriptVM.runAll();

        boolean changed = ScriptVM.stepBackgroundAnimations(ScriptVM.viewportIndex);
        boolean dirty = ScriptVM.takeBackgroundDirty();

        if (changed || dirty)
            renderer.upload(RoomParser.parseBackgroundBlock(room.background(), ScriptVM.viewportIndex));

        ScriptVM.stepFade();
        ScriptVM.stepMessages();

        List<Integer> allDialogue = new ArrayList<>();
        for (int id : ScriptVM.openMessages) {
            List<int[]> sections = ScriptVM.messageSections.get(id);
            int[] verts = sections.get(ScriptVM.messageSection.get(id));
            allDialogue.addAll(Utils.intArrayToList(verts));

            int[] choice = ScriptVM.messageChoices.get(id).get(ScriptVM.messageSection.get(id));
            if (choice[0] > 0)
                PEText.addPointer(allDialogue, choice[1], choice[2] + ScriptVM.messageCursor.get(id) * 12);
        }
        renderer.uploadText(Utils.intListToArray(allDialogue));

        int token = ScriptVM.takePendingRoom();
        if (token != 0) {
            loadMap(String.format("m%03d", ParasiteEve.mapFromToken(token)));
            return;
        }

        if (ScriptVM.viewportChanged) {
            ScriptVM.viewportChanged = false;
            camera.setViewport(viewports.get(ScriptVM.viewportIndex));
            renderer.upload(room.models(), viewports.get(ScriptVM.viewportIndex));
            renderer.upload(RoomParser.parseBackgroundBlock(room.background(), ScriptVM.viewportIndex));
        }

        int[] scroll = ScriptVM.takePendingScroll();
        if (scroll != null) {
            camera.beginScroll(scroll[0], scroll[1], scroll[2], scroll[3] == 1);
        }

        camera.step();

        for (ScriptVM vm : ScriptVM.getActors())
            vm.advanceAnim();

        ScriptVM player = ScriptVM.getPlayer();
        if ((player != null) && camera.canFollow() && !ScriptVM.cameraFrozen) {
            int x = player.getPosX() >> 16;
            int y = player.getPosY() >> 16;
            int z = player.getPosZ() >> 16;
            camera.follow(x, y, z);
        }

        renderer.upload(room.models(), viewports.get(ScriptVM.viewportIndex));
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