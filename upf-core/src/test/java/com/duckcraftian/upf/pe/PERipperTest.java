package com.duckcraftian.upf.pe;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.pe.disc.DiscTable;
import com.duckcraftian.upf.pe.disc.EntryProblem;
import com.duckcraftian.upf.pe.disc.PERipper;
import com.duckcraftian.upf.ps1.disc.DiscEntry;
import com.duckcraftian.upf.ps1.disc.DiscFileSystem;
import com.duckcraftian.upf.ps1.disc.RawDiscEntry;
import com.duckcraftian.upf.utils.FileUtils;
import com.duckcraftian.upf.utils.Utils;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static com.duckcraftian.upf.pe.ParasiteEve.DISC_1_PROGRAM;
import static com.duckcraftian.upf.pe.disc.EntryProblem.EntryProblemKind.*;
import static com.duckcraftian.upf.pe.disc.PERipper.DiscId.DISC_1;
import static com.duckcraftian.upf.pe.disc.PERipper.DiscId.DISC_2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PERipperTest {

    DiscFileSystem d1_fs;
    DiscFileSystem d2_fs;

    PERipper ripper;

    @BeforeAll
    void init() throws IOException {
        d1_fs = new DiscFileSystem("pe_disc_1");
        d2_fs = new DiscFileSystem("pe_disc_2");
        ripper = new PERipper();
        UPF.GSON = new GsonBuilder().setPrettyPrinting().create();
    }

    DiscTable loadTable(String manifestName, PERipper.DiscId discId, String exeName) throws URISyntaxException, IOException {
        String json = FileUtils.readString(manifestName);
        RawDiscEntry[] rows = UPF.GSON.fromJson(json, RawDiscEntry[].class);
        return new DiscTable(discId, exeName, rows);
    }

    List<Arguments> verifyRealDiscs() {
        return List.of(
                Arguments.of("pe_disc_1", "d1_manifest.json", DISC_1, "SLUS_006.62"),
                Arguments.of("pe_disc_2", "d2_manifest.json", DISC_2, "SLUS_006.68")
        );
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
    void verifyRealDiscTest(String imageName, String manifestName, PERipper.DiscId discId, String exeName) throws IOException, URISyntaxException, NoSuchAlgorithmException {
        DiscFileSystem fs = new DiscFileSystem(imageName);
        DiscTable table = loadTable(manifestName, discId, exeName);

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
        DiscTable real = loadTable("d1_manifest.json", DISC_1, DISC_1_PROGRAM);
        RawDiscEntry pe = null, cnf = null;

        for (RawDiscEntry entry : real.rows()) {
            if (entry.path().equalsIgnoreCase("PE.IMG")) {
                pe = entry;
            } else if (entry.path().equalsIgnoreCase("SYSTEM.CNF")) {
                cnf = entry;
            }

            if (pe != null && cnf != null)
                break;

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


}