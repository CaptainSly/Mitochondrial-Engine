package com.duckcraftian.upf.pe;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.pe.disc.DiscTable;
import com.duckcraftian.upf.pe.disc.PERipper;
import com.duckcraftian.upf.pe.disc.PERipper.DiscId;
import com.duckcraftian.upf.pe.disc.SquareImageParser;
import com.duckcraftian.upf.pe.disc.problems.EntryProblem;
import com.duckcraftian.upf.pe.disc.problems.SetupProblem;
import com.duckcraftian.upf.pe.exceptions.SetupFailure;
import com.duckcraftian.upf.ps1.data_types.PSXImage;
import com.duckcraftian.upf.ps1.disc.DiscEntry;
import com.duckcraftian.upf.ps1.disc.DiscFileSystem;
import com.duckcraftian.upf.ps1.disc.RawDiscEntry;
import com.duckcraftian.upf.serialization.UPDHSerializer;
import com.duckcraftian.upf.system.Utils;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.duckcraftian.upf.pe.ParasiteEve.DISC_1_PROGRAM;
import static com.duckcraftian.upf.pe.disc.PERipper.DiscId.DISC_1;
import static com.duckcraftian.upf.pe.disc.PERipper.DiscId.DISC_2;
import static com.duckcraftian.upf.pe.disc.SquareImageParser.*;
import static com.duckcraftian.upf.pe.disc.problems.EntryProblem.EntryProblemKind.*;
import static com.duckcraftian.upf.pe.disc.problems.SetupProblem.SetupProblemKind.*;
import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PERipperTest {

    DiscFileSystem d1_fs;
    DiscFileSystem d2_fs;

    PERipper ripper;

    @BeforeAll
    void init() throws URISyntaxException {
        d1_fs = new DiscFileSystem("pe_disc_1");
        d2_fs = new DiscFileSystem("pe_disc_2");
        ripper = new PERipper();
        UPF.GSON = new GsonBuilder().setPrettyPrinting().create();
    }

    List<Arguments> verifyRealDiscs() {
        return List.of(Arguments.of("pe_disc_1", DISC_1), Arguments.of("pe_disc_2", DISC_2));
    }

    @AfterAll
    void dispose() {
        d1_fs.onDispose();
        d2_fs.onDispose();
    }

    @Test
    void identifyTest() throws IOException {
        assertEquals(DISC_1, ripper.identify(d1_fs));
        assertEquals(DISC_2, ripper.identify(d2_fs));
    }

    @ParameterizedTest
    @MethodSource("verifyRealDiscs")
    void verifyRealDiscTest(String imageName, DiscId discId) throws IOException, URISyntaxException, NoSuchAlgorithmException {
        DiscFileSystem fs = new DiscFileSystem(imageName);
        DiscTable table = ripper.loadBundledTable(discId);

        try {
            List<EntryProblem> problems = ripper.verify(fs, table, (n, total, label) -> {
                Logger.debug("fileCount: " + n + ", fileTotal: " + total + ", fileLabel: " + label);
            });

            assertTrue(problems.isEmpty(), "problems: " + problems);
        } finally {
            fs.onDispose();
        }
    }

    @Test
    void damagedTableTest() throws URISyntaxException, IOException, NoSuchAlgorithmException {
        DiscTable real = ripper.loadBundledTable(DISC_1);
        RawDiscEntry pe = null, cnf = null;

        for (RawDiscEntry entry : real.rows()) {
            if (entry.path().equalsIgnoreCase("PE.IMG")) {
                pe = entry;
            } else if (entry.path().equalsIgnoreCase("SYSTEM.CNF")) {
                cnf = entry;
            }

            if (pe != null && cnf != null) break;

        }

        if (pe == null || cnf == null) {
            boolean peExists = !(pe == null);
            boolean cnfExists = !(cnf == null);
            throw new IllegalArgumentException("PE Exists: " + peExists + ", CNF Exists: " + cnfExists);
        }


        RawDiscEntry badHash = new RawDiscEntry(pe.path(), pe.lba(), pe.iso_size(), pe.sectors(), pe.mode(), pe.bytes(), Utils.stringMultiplier("0", 40));
        RawDiscEntry missing = new RawDiscEntry("NOPE.BIN", pe.lba(), pe.iso_size(), pe.sectors(), pe.mode(), pe.bytes(), pe.sha1());
        RawDiscEntry badLayout = new RawDiscEntry(cnf.path(), cnf.lba() + 1, cnf.iso_size(), cnf.sectors(), cnf.mode(), cnf.bytes(), cnf.sha1());

        DiscTable table = new DiscTable(DISC_1, DISC_1_PROGRAM, new RawDiscEntry[]{badHash, missing, badLayout});
        List<EntryProblem> problems = ripper.verify(d1_fs, table, (n, j, s) -> {
        });

        assertEquals(3, problems.size());
        assertEquals(new EntryProblem("PE.IMG", BAD_HASH), problems.get(0));
        assertEquals(new EntryProblem("NOPE.BIN", MISSING), problems.get(1));
        assertEquals(new EntryProblem("SYSTEM.CNF", WRONG_LAYOUT), problems.get(2));
    }

    @Test
    void setupBothMissing(@TempDir Path tempDir) {
        SetupFailure failure = assertThrows(SetupFailure.class, () -> ripper.setup(tempDir, (count, total, label) -> {
        }));
        List<SetupProblem> problems = failure.getProblems();
        assertEquals(2, problems.size());

        SetupProblem disc1MissingProblem = new SetupProblem(DISC_1.name(), FILE_NOT_FOUND, Optional.empty());
        SetupProblem disc2MissingProblem = new SetupProblem(DISC_2.name(), FILE_NOT_FOUND, Optional.empty());

        assertEquals(disc1MissingProblem, failure.getProblems().get(0));
        assertEquals(disc2MissingProblem, failure.getProblems().get(1));

    }

    @Test
    void setupNotRawImage(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("pe_disc_1.bin"), new byte[1000]);

        SetupFailure failure = assertThrows(SetupFailure.class, () -> ripper.setup(tempDir, (a, b, c) -> {
        }));
        assertEquals(2, failure.getProblems().size());

        SetupProblem disc1NotRawImageProblem = new SetupProblem(DISC_1.name(), NOT_RAW_IMAGE, Optional.empty());
        SetupProblem disc2MissingProblem = new SetupProblem(DISC_2.name(), FILE_NOT_FOUND, Optional.empty());

        assertEquals(disc1NotRawImageProblem, failure.getProblems().get(0));
        assertEquals(disc2MissingProblem, failure.getProblems().get(1));
    }

    @Test
    void setupSwappedDiscs(@TempDir Path tempDir) throws IOException {
        Path pe_disc_1 = tempDir.resolve("pe_disc_2.bin");
        Path pe_disc_2 = tempDir.resolve("pe_disc_1.bin");

        Path actualDisc1 = UPF.getEngineFolder("data").resolve("pe_disc_1.bin");
        Path actualDisc2 = UPF.getEngineFolder("data").resolve("pe_disc_2.bin");

        Files.createSymbolicLink(pe_disc_1, actualDisc1);
        Files.createSymbolicLink(pe_disc_2, actualDisc2);

        SetupFailure failure = assertThrows(SetupFailure.class, () -> ripper.setup(tempDir, (a, b, c) -> {
        }));

        assertEquals(2, failure.getProblems().size());

        SetupProblem disc1Problem = new SetupProblem(DISC_1.name(), WRONG_DISC, Optional.empty());
        SetupProblem disc2Problem = new SetupProblem(DISC_2.name(), WRONG_DISC, Optional.empty());

        assertEquals(disc1Problem, failure.getProblems().get(0));
        assertEquals(disc2Problem, failure.getProblems().get(1));
    }

    @Test
    void setupHappyPath() {
        AtomicInteger calls = new AtomicInteger();
        assertDoesNotThrow(() -> ripper.setup((a, b, c) -> {
            calls.getAndIncrement();
        }));

        assertEquals(56, calls.get());
    }

    @Test
    void readSectorMatchesReadData() throws IOException {
        DiscEntry peimg = d1_fs.findFile("PE.IMG");
        byte[] img = d1_fs.readSectors(peimg, 0, 3);
        assertEquals(6144, img.length);

        for (int i = 0; i < 3; i++) {
            byte[] single = d1_fs.getDisc().readData(peimg.lba() + i);
            assertTrue(Arrays.equals(Arrays.copyOfRange(img, i * 2048, (i + 1) * 2048), single));
        }
    }

    @Test
    void roomOnePiecesMatchGoldenHashes() throws IOException, NoSuchAlgorithmException {
        List<RoomRange> ranges = ripper.readRoomTable(d1_fs);
        RoomRange range = ranges.get(0);
        DiscEntry pe = d1_fs.findFile("PE.IMG");
        byte[] scratch = d1_fs.readSectors(pe, range.start(), (int) range.scratch());
        byte[] texture = d1_fs.readSectors(pe, range.start() + range.scratch(), (int) range.texture());
        byte[] room = d1_fs.readSectors(pe, range.start() + range.scratch() + range.texture(), (int) range.room());

        String scratchSha = Utils.hash(scratch);
        String textureSha = Utils.hash(texture);
        String roomSha = Utils.hash(room);

        assertEquals(ParasiteEve.SCRATCH_HASH, scratchSha);
        assertEquals(ParasiteEve.TEXTURE_HASH, textureSha);
        assertEquals(ParasiteEve.ROOM_HASH, roomSha);

        long dirOffset = u32LE(scratch, 4);
        long timsWord = u32LE(scratch, (int) (dirOffset + 0x28));
        long count = timsWord >> 22;
        long tableOff = timsWord & 0x3FFFFF;
        assertEquals(4, count);
    }

    @Test
    void ripImagesTest(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("images.udh");
        ripper.ripImages(d1_fs, file);
        assertTrue(Files.exists(tempDir.resolve("images.udh")));

        RoomRange range = ripper.readRoomTable(d1_fs).get(0);
        DiscEntry pe = d1_fs.findFile("PE.IMG");
        byte[] scratch = d1_fs.readSectors(pe, range.start(), (int) range.scratch());
        byte[] texture = d1_fs.readSectors(pe, range.start() + range.scratch(), (int) range.texture());

        byte[] expectedScratch = convertImagesToBytes(readVramUploads(scratch));
        byte[] expectedTexture = convertImagesToBytes(readVramUploads(texture));

        byte[] actualScratch = UPDHSerializer.getStreamedData(file, "images/m001/scratch");
        byte[] actualTexture = UPDHSerializer.getStreamedData(file, "images/m001/texture");

        assertArrayEquals(expectedScratch, actualScratch);
        assertArrayEquals(expectedTexture, actualTexture);
    }

}