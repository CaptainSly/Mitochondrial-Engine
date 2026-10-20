package com.duckcraftian.upf.ps1;

import com.duckcraftian.upf.ps1.disc.BinLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BinTest {

    BinLoader binLoader;

    @BeforeAll
    void createBinLoader() throws IOException {
        binLoader = new BinLoader("pe_disc_1");
    }

    @AfterAll
    void disposeBinLoader() {
        binLoader.onDispose();
    }

    @Test
    void totalSectorTest() {
        assertEquals(210685, binLoader.getTotalSectors());
    }

    @Test
    void readRawSectorTest() throws IOException {
        var fmt = HexFormat.ofDelimiter(" ").withUpperCase();
        byte[] bytes = binLoader.readRawSector(16);

        assertEquals("00 02 16 02", fmt.formatHex(bytes, 12, 16));
        assertEquals("00 00 09 00", fmt.formatHex(bytes, 16, 20));
        assertEquals("00 FF FF FF FF FF FF FF FF FF FF 00", fmt.formatHex(bytes, 0, 12));
    }

}