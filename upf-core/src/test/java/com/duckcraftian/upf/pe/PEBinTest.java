package com.duckcraftian.upf.pe;

import com.duckcraftian.upf.ps1.disc.BinLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PEBinTest {

    BinLoader loader;
    HexFormat fmt = HexFormat.ofDelimiter(" ").withUpperCase();

    @BeforeAll
    void createBinLoader() throws IOException {
        loader = new BinLoader("pe_disc_1");
    }

    @AfterAll
    void disposeBinLoader() {
        loader.onDispose();
    }

    @Test
    void CD001Test() throws IOException {
        byte[] data = loader.readData(16);
        assertEquals("01 43 44 30 30 31", fmt.formatHex(data, 0, 6));
    }

}