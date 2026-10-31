package com.duckcraftian.upf.pe.rooms;

import com.duckcraftian.upf.pe.ParasiteEve;
import com.duckcraftian.upf.pe.disc.RoomParser;
import com.duckcraftian.upf.pe.disc.SquareImageParser;
import com.duckcraftian.upf.pe.gfx.PEModel;
import com.duckcraftian.upf.pe.script.ScriptVM;
import com.duckcraftian.upf.ps1.data_types.PSXImage;
import com.duckcraftian.upf.serialization.UPDHSerializer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomLoader {

    public static Room loadRoom(Path imagesFile, String mapName) {
        try {
            byte[] background = UPDHSerializer.getStreamedData(imagesFile, "rooms/" + mapName + "/background");
            List<BackgroundTile> tiles = RoomParser.parseBackgroundBlock(background, 0);
            List<PSXViewport> viewports = RoomParser.parseViewports(background);

            List<PSXImage> images = new ArrayList<>();
            for (String kind : new String[]{"scratch", "texture"}) {
                byte[] bytes = UPDHSerializer.getStreamedData(imagesFile, "images/" + mapName + "/" + kind);
                images.addAll(SquareImageParser.convertBytesToImages(bytes));
            }

            byte[] container = UPDHSerializer.getStreamedData(imagesFile, "rooms/" + mapName + "/container");
            int[][] modelRecords = RoomParser.readModelRecords(container);

            int bank = RoomParser.readEntityBank(container);
            byte[] bankContainer = UPDHSerializer.getStreamedData(imagesFile, "entity/bank/" + bank + "/container");


            ScriptVM.loadRoom(container, bankContainer);

            Map<Integer, PEModel> models = new HashMap<>();
            for (int[] record : modelRecords) {
                PEModel model = RoomParser.parseModel(container, record[1]);
                RoomParser.applyTextureRemap(model, record[2]);
                models.put(record[0], model);
            }


            if (bank != 0) {
                images.addAll(SquareImageParser.convertBytesToImages(UPDHSerializer.getStreamedData(imagesFile, "images/entity/bank/" + bank)));

                for (int[] record : RoomParser.readModelRecords(bankContainer)) {
                    PEModel model = RoomParser.parseModel(bankContainer, record[1]);
                    RoomParser.applyTextureRemap(model, (int) ParasiteEve.PLAYER_TEXTURE_WORD);
                    models.put(record[0], model);
                }
            }

            return new Room(tiles, images, viewports, models);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}