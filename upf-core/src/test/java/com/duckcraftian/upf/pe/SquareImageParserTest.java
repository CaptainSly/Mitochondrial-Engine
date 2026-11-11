package com.duckcraftian.upf.pe;

import com.duckcraftian.upf.pe.disc.PERipper;
import com.duckcraftian.upf.pe.disc.SquareImageParser;
import com.duckcraftian.upf.pe.rooms.RoomRange;
import com.duckcraftian.upf.ps1.data_types.PSXImage;
import com.duckcraftian.upf.ps1.disc.DiscEntry;
import com.duckcraftian.upf.ps1.disc.DiscFileSystem;
import org.joml.Vector4i;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SquareImageParserTest {

    private DiscFileSystem d1fs;
    private PERipper ripper;

    private DiscEntry pe;
    private List<RoomRange> ranges;
    private RoomRange range;
    private byte[] scratch, texture;

    @BeforeAll
    void oonga() throws URISyntaxException, IOException {
        d1fs = new DiscFileSystem("pe_disc_1");
        ripper = new PERipper();
        ranges = ripper.readRoomTable(d1fs);
        range = ranges.get(0);
        pe = d1fs.findFile("PE.IMG");
        scratch = d1fs.readSectors(pe, range.start(), (int) range.scratch());
        texture = d1fs.readSectors(pe, range.start() + range.scratch(), (int) range.texture());
    }

    @AfterAll
    void boonga() {
        d1fs.onDispose();
    }

    @Test
    void loadSquareImages() {
        List<PSXImage> images = SquareImageParser.readVramUploads(scratch);
        assertEquals(4, images.size());

        Vector4i[] expected = {
                new Vector4i(768, 0, 64, 255),
                new Vector4i(768, 255, 64, 1),
                new Vector4i(832, 0, 64, 255),
                new Vector4i(832, 255, 64, 1)
        };

        for (int i = 0; i < images.size(); i++) {
            PSXImage image = images.get(i);

            assertEquals(expected[i].x, image.x());
            assertEquals(expected[i].y, image.y());
            assertEquals(expected[i].z, image.width());
            assertEquals(expected[i].w, image.height());

            int size = image.width() * 2 * image.height();
            assertEquals(size, image.pixels().length);
        }

        List<PSXImage> textures = SquareImageParser.readVramUploads(texture);
        assertEquals(12, textures.size());

        Vector4i[] textureExpected = {
                new Vector4i(896, 0, 64, 255),
                new Vector4i(896, 255, 64, 1),
                new Vector4i(0, 480, 256, 32),
                new Vector4i(512, 0, 64, 255),
                new Vector4i(512, 255, 64, 1),
                new Vector4i(576, 0, 64, 255),
                new Vector4i(576, 255, 64, 1),
                new Vector4i(640, 0, 64, 255),
                new Vector4i(640, 255, 64, 1),
                new Vector4i(0, 450, 256, 2),
                new Vector4i(0, 452, 256, 2),
                new Vector4i(0, 454, 256, 2)

        };

        for (int i = 0; i < textures.size(); i++) {
            PSXImage image = textures.get(i);

            assertEquals(textureExpected[i].x, image.x());
            assertEquals(textureExpected[i].y, image.y());
            assertEquals(textureExpected[i].z, image.width());
            assertEquals(textureExpected[i].w, image.height());

            int size = image.width() * 2 * image.height();
            assertEquals(size, image.pixels().length);
        }

    }

    @Test
    void oversizedRecordIsRejected() {
        byte[] bad = Arrays.copyOf(scratch, scratch.length);
        bad[65604] = (byte) 0xDEADBEEF;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> SquareImageParser.readVramUploads(bad));
        assertTrue(exception.getMessage().startsWith("Record: 0"));
    }

}