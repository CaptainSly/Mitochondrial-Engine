package com.duckcraftian.upf.ps1.disc;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.impl.IDisposable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import static com.duckcraftian.upf.ps1.disc.DiscConstants.*;
import static com.duckcraftian.upf.system.DataTypeHelper.*;
import static com.duckcraftian.upf.system.Utils.toHex;

public class DiscFileSystem implements IDisposable {

    private BinLoader disc;
    private long rootLba;
    private long rootLen;

    public DiscFileSystem(String discName) throws URISyntaxException {
        this(UPF.getEngineFolder("data").resolve(discName));
    }

    public DiscFileSystem(Path discName) throws URISyntaxException {
        try {
            disc = new BinLoader(discName);
            readRootRecord();
        } catch (IOException e) {

            if (disc != null)
                disc.onDispose();

            throw new RuntimeException(e);
        }
    }

    private void readRootRecord() throws IOException {
        byte[] pvd = disc.readData(PVD_SECTOR);

        rootLba = u32LE(pvd, ROOT_RECORD_OFFSET + REC_LBA_OFFSET);
        rootLen = u32LE(pvd, ROOT_RECORD_OFFSET + REC_SIZE_OFFSET);
    }

    public String hashRaw(DiscEntry entry, MessageDigest digest) throws IOException {
        long sectorCount = (entry.size() + FORM1_DATA_SIZE - 1) / FORM1_DATA_SIZE;

        for (int i = 0; i < sectorCount; i++) {
            byte[] raw = disc.readRawSector(entry.lba() + i);
            digest.update(raw, 0, RAW_SECTOR_SIZE);
        }

        return toHex(digest.digest());
    }

    public String hashFile(DiscEntry entry, MessageDigest digest) throws IOException {
        long sectorCount = (entry.size() + FORM1_DATA_SIZE - 1) / FORM1_DATA_SIZE;
        long bytesLeft = entry.size();

        for (int i = 0; i < sectorCount; i++) {
            long discSector = entry.lba() + i;
            byte[] chunk = disc.readData(discSector);

            long usable = Math.min(FORM1_DATA_SIZE, bytesLeft);
            digest.update(chunk, 0, (int) usable);

            if (bytesLeft == 0)
                break;

            bytesLeft -= usable;
        }

        return toHex(digest.digest());
    }

    public byte[] readSectors(DiscEntry file, long firstSector, int count) throws IOException {
        int length = count * FORM1_DATA_SIZE;
        byte[] out = new byte[length];
        for (int i = 0; i < count; i++) {
            byte[] sector = disc.readData(file.lba() + firstSector + i);
            System.arraycopy(sector, 0, out, i * FORM1_DATA_SIZE, FORM1_DATA_SIZE);
        }

        return out;
    }

    public byte[] readFile(DiscEntry entry) throws IOException {
        long sectors = (entry.size() + FORM1_DATA_SIZE - 1) / FORM1_DATA_SIZE;
        byte[] out = new byte[(int) entry.size()];

        for (int i = 0; i < sectors; i++) {
            int copied = i * FORM1_DATA_SIZE;
            int remaining = (int) (entry.size() - copied);
            byte[] chunk = disc.readData(entry.lba() + i);
            System.arraycopy(chunk, 0, out, i * FORM1_DATA_SIZE, Math.min(FORM1_DATA_SIZE, remaining));
        }

        return out;
    }

    public DiscEntry findFile(String path) throws IOException {
        List<String> parts = new ArrayList<>();
        for (String s : path.split("/"))
            if (!s.isEmpty())
                parts.add(s);

        if (parts.isEmpty())
            return null;

        long curLba = rootLba;
        long curLen = rootLen;

        for (int i = 0; i < parts.size(); i++) {
            String wanted = parts.get(i);
            boolean isLast = (i == parts.size() - 1);

            List<DiscEntry> entries = listDir(curLba, curLen);
            DiscEntry match = null;
            for (DiscEntry e : entries)
                if (e.name().equalsIgnoreCase(wanted)) {
                    match = e;
                    break;
                }

            if (match == null)
                return null;

            if (isLast)
                return match;

            if (!match.isDir())
                return null;

            curLba = match.lba();
            curLen = match.size();
        }

        return null;
    }

    public List<DiscEntry> walk(long dirLba, long dirLen, String prefix) throws IOException {
        List<DiscEntry> result = new ArrayList<>();
        for (DiscEntry e : listDir(dirLba, dirLen)) {
            String path = prefix + e.name();
            if (e.isDir()) walk(e.lba(), e.size(), path + "/").forEach(result::add);
            else result.add(new DiscEntry(path, e.lba(), e.size(), false));
        }

        return result;
    }

    public List<DiscEntry> listDir(long lba, long len) throws IOException {
        long sectors = (len + FORM1_DATA_SIZE - 1) / FORM1_DATA_SIZE;

        byte[] data = new byte[(int) (sectors * FORM1_DATA_SIZE)];
        for (int i = 0; i < sectors; i++)
            System.arraycopy(disc.readData(lba + i), 0, data, i * FORM1_DATA_SIZE, FORM1_DATA_SIZE);

        List<DiscEntry> entries = new ArrayList<>();
        int pos = 0;
        while (pos < len) {
            int recLen = u8(data, pos);

            if (recLen == 0) {
                pos = (pos / FORM1_DATA_SIZE + 1) * FORM1_DATA_SIZE;
                continue;
            }

            long entryLba = u32LE(data, pos + REC_LBA_OFFSET);
            long entrySize = u32LE(data, pos + REC_SIZE_OFFSET);

            int flags = u8(data, pos + REC_FLAGS_OFFSET);
            boolean isDir = (flags & FLAG_DIRECTORY) != 0;

            int nameLen = u8(data, pos + REC_NAME_LEN_OFFSET);
            if (nameLen == 1 && (u8(data, pos + REC_NAME_OFFSET) == 0x00 || (u8(data, pos + REC_NAME_OFFSET) == 0x01))) {
                pos += recLen;
                continue;
            }

            String name = ascii(data, pos + REC_NAME_OFFSET, nameLen);
            int semi = name.lastIndexOf(";");
            if (semi != -1)
                name = name.substring(0, semi);

            entries.add(new DiscEntry(name, entryLba, entrySize, isDir));
            pos += recLen;
        }

        return entries;
    }

    public BinLoader getDisc() {
        return disc;
    }

    public long getRootLba() {
        return rootLba;
    }

    public long getRootLen() {
        return rootLen;
    }

    @Override
    public void onDispose() {
        disc.onDispose();
    }
}