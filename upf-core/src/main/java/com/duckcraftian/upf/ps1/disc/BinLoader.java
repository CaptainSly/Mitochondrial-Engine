package com.duckcraftian.upf.ps1.disc;

import com.duckcraftian.upf.impl.IDisposable;

import java.io.IOException;
import java.io.RandomAccessFile;

import static com.duckcraftian.upf.UPF.getEngineFolder;
import static com.duckcraftian.upf.ps1.disc.DiscConstants.*;

public class BinLoader implements IDisposable {

    private RandomAccessFile bin;

    private final int totalSectors;

    public BinLoader(String discName) throws IOException {
        bin = new RandomAccessFile(getEngineFolder("data").resolve(discName + ".bin").toFile(), "r");
        if (bin.length() % RAW_SECTOR_SIZE != 0)
            throw new RuntimeException("Not a raw MODE2/2352 image");

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