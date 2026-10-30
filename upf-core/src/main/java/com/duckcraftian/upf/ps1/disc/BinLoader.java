package com.duckcraftian.upf.ps1.disc;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.impl.IDisposable;
import com.duckcraftian.upf.ps1.disc.exceptions.NotRawImageException;
import com.duckcraftian.upf.system.FileUtils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static com.duckcraftian.upf.ps1.disc.DiscConstants.*;

public class BinLoader implements IDisposable {

    private final RandomAccessFile bin;
    private int totalSectors;

    public BinLoader(String discName) throws IOException, URISyntaxException {
        this(UPF.getEngineFolder("data").resolve(discName));
    }

    public BinLoader(Path discName) throws IOException, URISyntaxException {
        bin = new RandomAccessFile(FileUtils.resolveAssetFile(discName + ".bin"), "r");
        setup();
    }

    private void setup() throws IOException {
        if (bin.length() % RAW_SECTOR_SIZE != 0) {
            bin.close();

            throw new NotRawImageException("Not a raw MODE2/2352 image");
        }
        totalSectors = (int) (bin.length() / RAW_SECTOR_SIZE);
    }

    public byte[] readData(long sector) throws IOException {
        return readBytes(sector, DATA_OFFSET, FORM1_DATA_SIZE);
    }

    public byte[] readRawSector(long sector) throws IOException {
        return readBytes(sector, 0, RAW_SECTOR_SIZE);
    }

    private byte[] readBytes(long sector, int skip, int length) throws IOException {
        if (sector < 0 || sector >= totalSectors)
            throw new IllegalArgumentException("Sectors can't be greater than totalSectors" + totalSectors);

        byte[] bytes = new byte[length];
        bin.seek(sector * RAW_SECTOR_SIZE + skip);
        bin.readFully(bytes);
        return bytes;
    }

    public int getTotalSectors() {
        return totalSectors;
    }

    @Override
    public void onDispose() {
        if (bin != null) {
            try {
                bin.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}