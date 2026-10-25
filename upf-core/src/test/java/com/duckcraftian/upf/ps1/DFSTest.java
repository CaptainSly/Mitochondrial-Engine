package com.duckcraftian.upf.ps1;

import com.duckcraftian.upf.ps1.disc.DiscEntry;
import com.duckcraftian.upf.ps1.disc.DiscFileSystem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DFSTest {

    private DiscFileSystem fileSystem;

    @BeforeAll
    void createFileSystem() throws URISyntaxException {
        fileSystem = new DiscFileSystem("pe_disc_1");
    }

    @AfterAll
    void disposeFileSystem() {
        fileSystem.onDispose();
    }

    @Test
    void testFileSystem() throws IOException {
        assertTrue(fileSystem.getRootLba() > 16);
        assertTrue(fileSystem.getRootLen() > 0);
        assertEquals(0, fileSystem.getRootLen() % 2048);
    }

    @Test
    void listDirTest() throws IOException {
        List<DiscEntry> entries = fileSystem.listDir(fileSystem.getRootLba(), fileSystem.getRootLen());
        for (DiscEntry e : entries)
            Logger.debug(e.toString());
    }

}