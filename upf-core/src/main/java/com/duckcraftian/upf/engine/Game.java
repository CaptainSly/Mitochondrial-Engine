package com.duckcraftian.upf.engine;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.pe.AyaStats;
import com.duckcraftian.upf.pe.ParasiteEve;
import com.duckcraftian.upf.pe.audio.PEAudio;
import com.duckcraftian.upf.pe.disc.PERipper;
import com.duckcraftian.upf.pe.disc.RoomParser;
import com.duckcraftian.upf.pe.gfx.PECamera;
import com.duckcraftian.upf.pe.gfx.PEModel;
import com.duckcraftian.upf.pe.gfx.PERenderer;
import com.duckcraftian.upf.pe.gfx.PEText;
import com.duckcraftian.upf.pe.rooms.PSXViewport;
import com.duckcraftian.upf.pe.rooms.Room;
import com.duckcraftian.upf.pe.rooms.RoomLoader;
import com.duckcraftian.upf.pe.script.ScriptVM;
import com.duckcraftian.upf.ps1.PSX;
import com.duckcraftian.upf.ps1.data_types.PSXImage;
import com.duckcraftian.upf.ps1.disc.DiscFileSystem;
import com.duckcraftian.upf.ps1.video.MoviePlayer;
import com.duckcraftian.upf.system.Utils;
import com.duckcraftian.upf.utils.Input;
import org.lwjgl.sdl.SDLScancode;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.duckcraftian.upf.pe.ParasiteEve.AYA_STATS;
import static com.duckcraftian.upf.pe.ParasiteEve.PEAUDIO;
import static com.duckcraftian.upf.pe.disc.SquareImageParser.convertBytesToImages;
import static com.duckcraftian.upf.serialization.UPDHSerializer.getStreamedData;
import static com.duckcraftian.upf.system.DataTypeHelper.s16LE;

public class Game implements IDisposable {

    private PERenderer renderer;
    private List<PSXViewport> viewports;
    private MoviePlayer moviePlayer;
    private String[] movies;

    private PECamera camera;
    private PERipper ripper;

    private Room room;

    private boolean paused;

    public Path imageFile;

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

        imageFile = UPF.getEngineFolder("data").resolve("pe.udh");
        if (!Files.exists(imageFile)) {
            DiscFileSystem dfs = null, dfs2 = null;
            try {
                dfs = new DiscFileSystem("pe_disc_1");
                dfs2 = new DiscFileSystem("pe_disc_2");
                ripper = new PERipper();
                ripper.ripImages(dfs, dfs2, imageFile);

            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                dfs.onDispose();
                dfs2.onDispose();
            }
        }

        PEAUDIO = new PEAudio();
        AYA_STATS = new AyaStats();
        try {
            PEAUDIO.init(imageFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int[] random = new int[5];

        PSX.srand(1);
        for (int i = 0; i < random.length; i++) random[i] = PSX.rand();
        Arrays.stream(random).forEach(i -> Logger.debug("Golden Number: " + i));

        PSX.srand(0);
        for (int i = 0; i < random.length; i++) random[i] = PSX.rand();
        Arrays.stream(random).forEach(i -> Logger.debug("Golden Number: " + i));


        try {
            byte[] fontImages = getStreamedData(imageFile, "text/font/images");
            for (PSXImage image : convertBytesToImages(fontImages))
                renderer.upload(image);

            byte[] fontMetrics = getStreamedData(imageFile, "text/font/metrics");
            byte[] normals = getStreamedData(imageFile, "render/normals");
            byte[] levelStats = getStreamedData(imageFile, "battle/levelStats");
            byte[] dataPack = getStreamedData(imageFile, "common/dataPack");
            byte[] weaponEffectCodes = getStreamedData(imageFile, "battle/weaponEffectCodes");
            byte[] actionAnimRows = getStreamedData(imageFile, "battle/actionAnimRows");

            PEText.metrics = fontMetrics;
            AYA_STATS.levelStats = levelStats;
            AYA_STATS.dataPack = dataPack;
            AYA_STATS.weaponEffectCodes = weaponEffectCodes;
            AYA_STATS.actionAnimRows = actionAnimRows;

            PEModel.normals = new int[240 * 3];
            for (int i = 0; i < 240; i++) {
                PEModel.normals[3 * i] = s16LE(normals, 8 * i);
                PEModel.normals[3 * i + 1] = s16LE(normals, 8 * i + 2);
                PEModel.normals[3 * i + 2] = s16LE(normals, 8 * i + 4);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ScriptVM.movieStarter = m -> playMovie(movieName(m), false, false);

        loadMap("m004");
    }

    public void loadMap(String mapName) {
        try {
            room = RoomLoader.loadRoom(imageFile, mapName);
            viewports = room.viewports();
            camera = new PECamera(viewports.get(0));
            PEAUDIO.camera = camera;

            renderer.setCameraPosition(camera.getCamX(), camera.getCamY());
            renderer.upload(room);

            Logger.debug("Loading map: " + mapName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void playMovie(String name, boolean canSkip, boolean blocking) {
        Logger.debug("Playing Movie: " + name);
        try {
            moviePlayer = new MoviePlayer();
            moviePlayer.start(getStreamedData(imageFile, "movies/" + name), PEAUDIO.source, canSkip);
            moviePlayer.blocking = blocking;
            ScriptVM.movieElapsed = moviePlayer.elapsed();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(float delta) {

        if (ScriptVM.takeBattleTextureLoading())
            loadBattleTextures();

        if (moviePlayer != null) {
            moviePlayer.step();
            if (moviePlayer.skippable && (Input.isKeyJustDown(SDLScancode.SDL_SCANCODE_F) || Input.isKeyJustDown(SDLScancode.SDL_SCANCODE_ESCAPE))) {
                moviePlayer.done = true;
            }

            if (moviePlayer.done) {
                moviePlayer.stop();
                moviePlayer = null;
                ScriptVM.movieElapsed = -1;
            } else {
                ScriptVM.movieElapsed = moviePlayer.elapsed();
            }

            if (moviePlayer != null && moviePlayer.blocking) return;
        }

        if (moviePlayer == null && !ScriptVM.openMessages.isEmpty()) {
            if (Input.isKeyJustDown(SDLScancode.SDL_SCANCODE_F))
                confirmPressed = true;

            if (Input.isKeyJustDown(SDLScancode.SDL_SCANCODE_W))
                upPressed = true;

            if (Input.isKeyJustDown(SDLScancode.SDL_SCANCODE_S))
                downPressed = true;
        }

        if (Input.isKeyJustDown(SDLScancode.SDL_SCANCODE_P))
            paused = !paused;

        if (Input.isKeyJustDown(SDLScancode.SDL_SCANCODE_F8))
            ScriptVM.dumpActors(camera);


        if (!paused) {
            accumulator += delta;
            while (accumulator >= TICK) {
                accumulator -= TICK;
                tick();
            }
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
            pad |= (0x100 | 0x200 | 0x20000000);


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

        if (moviePlayer != null) pad = 0;
        ScriptVM.pad = pad;
        PEAUDIO.newFrame();
        ScriptVM.runAll();

        ScriptVM.updateBattle();

        boolean changed = ScriptVM.stepBackgroundAnimations(ScriptVM.viewportIndex);
        boolean dirty = ScriptVM.takeBackgroundDirty();

        if (changed || dirty)
            renderer.upload(RoomParser.parseBackgroundBlock(room.background(), ScriptVM.viewportIndex));

        ScriptVM.stepFade();
        ScriptVM.checkAutoFadeIn();
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

        if (moviePlayer != null) {
            moviePlayer.draw(renderer);
            return;
        }

        renderer.setCameraPosition(camera.getCamX(), camera.getCamY());
        renderer.onRender(viewports);
    }

    private void loadBattleTextures() {
        try {
            byte[] data = getStreamedData(imageFile, "images/battle/textures");
            List<PSXImage> images = convertBytesToImages(data);
            images.forEach(renderer::upload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private String movieName(int idx) {
        if (movies == null) {
            try {
                movies = new String(getStreamedData(imageFile, "movies/names"), StandardCharsets.UTF_8).split("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return movies[idx];
    }

    public void resize(int width, int height) {

    }

    @Override
    public void onDispose() {
        if (moviePlayer != null)
            moviePlayer.onDispose();

        renderer.onDispose();
    }

}