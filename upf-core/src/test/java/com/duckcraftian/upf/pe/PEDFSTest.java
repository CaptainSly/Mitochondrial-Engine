package com.duckcraftian.upf.pe;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.ps1.disc.DiscEntry;
import com.duckcraftian.upf.ps1.disc.DiscFileSystem;
import com.duckcraftian.upf.ps1.disc.RawDiscEntry;
import com.duckcraftian.upf.system.FileUtils;
import com.duckcraftian.upf.system.Utils;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static com.duckcraftian.upf.ps1.disc.DiscConstants.DATA_2048;
import static com.duckcraftian.upf.ps1.disc.DiscConstants.RAW_2352;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PEDFSTest {

    private DiscFileSystem fileSystem;
    private MessageDigest d;

    public List<Arguments> discCases() {
        return List.of(
                Arguments.of("pe_disc_1", "d1_manifest.json"),
                Arguments.of("pe_disc_2", "d2_manifest.json")
        );
    }

    @BeforeAll
    void createFileSystem() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        fileSystem = new DiscFileSystem("pe_disc_1");

        UPF.GSON = new GsonBuilder().setPrettyPrinting().create();
        d = MessageDigest.getInstance("SHA-1");
    }

    @AfterAll
    void disposeFileSystem() {
        fileSystem.onDispose();
    }

    @Test
    void findFileTest() throws IOException {
        DiscEntry PE = fileSystem.findFile("PE.IMG");
        DiscEntry FMV000 = fileSystem.findFile("FMV1/FMV000.STR");
        DiscEntry pe = fileSystem.findFile("pe.img");
        DiscEntry none = fileSystem.findFile("NOPE.bin");
        DiscEntry x = fileSystem.findFile("pe.img/x");

        // PE.IMG check
        assertEquals(1013, PE.lba());
        assertEquals(206213120, PE.size());
        assertEquals(1013, pe.lba());
        assertEquals(206213120, pe.size());

        // FMV Check
        assertEquals(101705, FMV000.lba());

        // Non existant checks
        assertNull(none);
        assertNull(x);
    }

    @Test
    void readFileTest() throws IOException {
        DiscEntry slus = fileSystem.findFile("SLUS_006.62");
        byte[] slusBytes = fileSystem.readFile(slus);
        byte[] sha = d.digest(slusBytes);

        assertEquals("452fb033f2eaa4b18aa20a5bca60b8125af3a37b", Utils.toHex(sha));
    }

    @Test
    void hashTest() throws IOException {
        String hash = fileSystem.hashFile(fileSystem.findFile("PE.IMG"), d);
        assertEquals("146c0ce7308bf9fdc2ba5a84230e198db0663f3b", hash);
    }

    @ParameterizedTest
    @MethodSource("discCases")
    void walkAndHashTest(String imageName, String manifestName) throws IOException, URISyntaxException, NoSuchAlgorithmException {
        DiscFileSystem fs = new DiscFileSystem(imageName);
        try {
            RawDiscEntry[] expected = UPF.GSON.fromJson(FileUtils.readString(manifestName), RawDiscEntry[].class);
            List<DiscEntry> found = fs.walk(fs.getRootLba(), fs.getRootLen(), "");

            assertEquals(expected.length, found.size());

            for (RawDiscEntry m : expected) {
                DiscEntry match = null;
                for (DiscEntry f : found)
                    if (f.name().equalsIgnoreCase(m.path())) match = f;

                assertNotNull(match, "Missing: " + m.path());
                assertEquals(m.lba(), match.lba(), m.path());
                assertEquals(m.iso_size(), match.size(), m.path());

                String actual = switch (m.mode()) {
                    case DATA_2048 -> fs.hashFile(match, d);
                    case RAW_2352 -> fs.hashRaw(match, d);
                    default -> throw new IllegalArgumentException("Unknown mode '" + m.mode() + "' for " + m.path());
                };

                assertEquals(m.sha1(), actual, m.path());
            }

        } finally {
            fs.onDispose();
        }
    }

}