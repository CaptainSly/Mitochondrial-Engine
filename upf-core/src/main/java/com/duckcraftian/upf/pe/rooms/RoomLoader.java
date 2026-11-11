package com.duckcraftian.upf.pe.rooms;

import com.duckcraftian.upf.pe.ParasiteEve;
import com.duckcraftian.upf.pe.disc.RoomParser;
import com.duckcraftian.upf.pe.disc.SquareImageParser;
import com.duckcraftian.upf.pe.gfx.BackgroundAnimation;
import com.duckcraftian.upf.pe.gfx.PEModel;
import com.duckcraftian.upf.pe.script.ScriptVM;
import com.duckcraftian.upf.ps1.data_types.PSXImage;
import com.duckcraftian.upf.serialization.UPDHSerializer;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.duckcraftian.upf.pe.ParasiteEve.SCENE_OBJECT_MODELKEY;
import static com.duckcraftian.upf.pe.ParasiteEve.STAND_IN_ACTION_SET;
import static com.duckcraftian.upf.pe.disc.RoomParser.parseModel;
import static com.duckcraftian.upf.pe.disc.RoomParser.readKeyedOffset;
import static com.duckcraftian.upf.serialization.UPDHSerializer.getStreamedData;

public class RoomLoader {

    public static Room loadRoom(Path imagesFile, String mapName) {
        try {
            byte[] background = getStreamedData(imagesFile, "rooms/" + mapName + "/background");
            BackgroundAnimation.initBackground(background);
            List<BackgroundTile> tiles = RoomParser.parseBackgroundBlock(background, 0);
            List<PSXViewport> viewports = RoomParser.parseViewports(background);

            List<PSXImage> images = new ArrayList<>();
            for (String kind : new String[]{"scratch", "texture"}) {
                byte[] bytes = getStreamedData(imagesFile, "images/" + mapName + "/" + kind);
                images.addAll(SquareImageParser.convertBytesToImages(bytes));
            }

            byte[] objs = getStreamedData(imagesFile, "images/common/objects");
            List<PSXImage> objImages = SquareImageParser.convertBytesToImages(objs);
            for (PSXImage image : objImages) images.add(image);

            Map<Integer, PEModel> sceneModels = new HashMap<>();
            byte[] sceneAssets = getStreamedData(imagesFile, "common/sceneAssets");
            int gunOffset = readKeyedOffset(sceneAssets, SCENE_OBJECT_MODELKEY);
            PEModel gunModel = parseModel(sceneAssets, gunOffset);
            sceneModels.put((int) SCENE_OBJECT_MODELKEY, gunModel);

            byte[] container = getStreamedData(imagesFile, "rooms/" + mapName + "/container");
            int[][] modelRecords = RoomParser.readModelRecords(container);

            int bank = RoomParser.readEntityBank(container);
            byte[] bankContainer = null;
            if (bank != 0)
                bankContainer = getStreamedData(imagesFile, "entity/bank/" + bank + "/container");

            byte[] actionSet = null;
            if (bank != 0) {
                int actionIndex = 22 + 8 * ((bank - 10) / 2) + STAND_IN_ACTION_SET;
                actionSet = getStreamedData(imagesFile, "entity/actionSet/" + actionIndex);
            }

            Map<Integer, PEModel> models = new HashMap<>();
            for (int[] record : modelRecords) {
                PEModel model = RoomParser.parseModel(container, record[1]);
                RoomParser.applyTextureRemap(model, record[2]);
                models.put(record[0], model);
            }

            if (bank != 0) {
                images.addAll(SquareImageParser.convertBytesToImages(getStreamedData(imagesFile, "images/entity/bank/" + bank)));

                for (int[] record : RoomParser.readModelRecords(bankContainer)) {
                    PEModel model = RoomParser.parseModel(bankContainer, record[1]);
                    RoomParser.applyTextureRemap(model, (int) ParasiteEve.PLAYER_TEXTURE_WORD);
                    models.put(record[0], model);
                }
            }

            ScriptVM.models = models;
            ScriptVM.sceneObjectModels = sceneModels;
            ScriptVM.loadRoom(container, bankContainer, actionSet);
            return new Room(tiles, images, viewports, models, background);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}