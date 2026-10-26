package com.duckcraftian.upf.pe.rooms;

import com.duckcraftian.upf.pe.disc.RoomParser;
import com.duckcraftian.upf.pe.disc.SquareImageParser;
import com.duckcraftian.upf.pe.gfx.PSXAnimation;
import com.duckcraftian.upf.pe.gfx.PSXModel;
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
            int[][] animRecords = RoomParser.readAnimationRecords(container);

            Map<Integer, PSXModel> models = new HashMap<>();
            for (int[] record : modelRecords) {
                PSXModel model = RoomParser.parseModel(container, record[1]);
                RoomParser.applyTextureRemap(model, record[2]);
                models.put(record[0], model);
            }

            int[][] placements = {
                    {2, -161, 0, -5352, 0},
                    {3, 396, 0, -5362, 480},
                    {4, 795, 0, -4974, 0}
            };

            List<Actor> actors = new ArrayList<>();
            for (int[] p : placements) {
                int[] animRecord = null;
                for (int[] a : animRecords)
                    if (a[0] == p[0] && a[1] == 0)
                        animRecord = a;
                PSXAnimation animation = RoomParser.parseAnimation(container, animRecord[2]);
                actors.add(new Actor(models.get(p[0]), animation, new int[]{p[1], p[2], p[3]}, p[4]));
            }

            return new Room(tiles, images, viewports, actors);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}