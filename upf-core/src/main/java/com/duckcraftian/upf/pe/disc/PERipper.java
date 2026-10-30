package com.duckcraftian.upf.pe.disc;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.pe.ParasiteEve;
import com.duckcraftian.upf.pe.RoomRange;
import com.duckcraftian.upf.pe.disc.problems.EntryProblem;
import com.duckcraftian.upf.ps1.disc.exceptions.NotRawImageException;
import com.duckcraftian.upf.pe.exceptions.SetupFailure;
import com.duckcraftian.upf.pe.disc.problems.SetupProblem;
import com.duckcraftian.upf.ps1.disc.DiscEntry;
import com.duckcraftian.upf.ps1.disc.DiscFileSystem;
import com.duckcraftian.upf.ps1.disc.RawDiscEntry;
import com.duckcraftian.upf.serialization.UPDHSerializer;
import com.duckcraftian.upf.system.FileUtils;
import com.duckcraftian.upf.utils.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static com.duckcraftian.upf.pe.ParasiteEve.*;
import static com.duckcraftian.upf.pe.disc.SquareImageParser.*;
import static com.duckcraftian.upf.pe.disc.problems.EntryProblem.EntryProblemKind.*;
import static com.duckcraftian.upf.pe.disc.problems.SetupProblem.SetupProblemKind.*;
import static com.duckcraftian.upf.ps1.disc.DiscConstants.DATA_2048;
import static com.duckcraftian.upf.ps1.disc.DiscConstants.RAW_2352;
import static com.duckcraftian.upf.system.DataTypeHelper.u16LE;
import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;

public class PERipper {

    public enum DiscId {
        DISC_1, DISC_2
    }

    public DiscId identify(DiscFileSystem fs) throws IOException {
        if (fs.findFile(DISC_1_PROGRAM) != null) return DiscId.DISC_1;
        if (fs.findFile(DISC_2_PROGRAM) != null) return DiscId.DISC_2;

        return null;
    }

    public List<EntryProblem> verify(DiscFileSystem fs, DiscTable table, ProgressListener progress) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        int fileCount = table.rows().length;
        List<EntryProblem> problems = new ArrayList<>();
        for (int i = 0; i < fileCount; i++) {
            digest.reset();

            RawDiscEntry row = table.rows()[i];
            progress.onProgress(i + 1, fileCount, row.path());

            DiscEntry entry = fs.findFile(row.path());
            if (entry == null) {
                problems.add(new EntryProblem(row.path(), MISSING));
                continue;
            }

            if (entry.lba() != row.lba() || entry.size() != row.iso_size()) {
                problems.add(new EntryProblem(row.path(), WRONG_LAYOUT));
                continue;
            }

            String actual = switch (row.mode()) {
                case DATA_2048 -> fs.hashFile(entry, digest);
                case RAW_2352 -> fs.hashRaw(entry, digest);
                default -> throw new IllegalArgumentException("Unknown mode '" + row.mode() + "' for " + row.path());
            };

            if (!actual.equals(row.sha1()))
                problems.add(new EntryProblem(row.path(), BAD_HASH));

        }

        return problems;
    }

    public DiscTable loadBundledTable(DiscId discId) throws URISyntaxException, IOException {
        String manifest = switch (discId) {
            case DISC_1 -> "d1_manifest.json";
            case DISC_2 -> "d2_manifest.json";
        };

        String manifestContent = FileUtils.readString(manifest);
        RawDiscEntry[] rows = UPF.GSON.fromJson(manifestContent, RawDiscEntry[].class);

        String exeName = switch (discId) {
            case DISC_1 -> DISC_1_PROGRAM;
            case DISC_2 -> DISC_2_PROGRAM;
        };

        return new DiscTable(discId, exeName, rows);
    }

    public void setup(ProgressListener progress) throws IOException, URISyntaxException, NoSuchAlgorithmException {
        Path dataDir = UPF.getEngineFolder("data");
        List<SetupProblem> problems = setupDisc(progress, dataDir);

        if (!problems.isEmpty())
            throw new SetupFailure(problems);
    }

    public void setup(Path dataDir, ProgressListener progress) throws IOException, NoSuchAlgorithmException, URISyntaxException {
        List<SetupProblem> problems = setupDisc(progress, dataDir);

        if (!problems.isEmpty())
            throw new SetupFailure(problems);
    }

    private List<SetupProblem> setupDisc(ProgressListener progress, Path dataDir) throws IOException, NoSuchAlgorithmException, URISyntaxException {
        List<SetupProblem> problems = new ArrayList<>();
        for (DiscId discId : DiscId.values()) {
            String discName = switch (discId) {
                case DISC_1 -> "pe_disc_1";
                case DISC_2 -> "pe_disc_2";
            };

            File disc = dataDir.resolve(discName + ".bin").toFile();
            if (!disc.exists()) {
                problems.add(new SetupProblem(discId.name(), FILE_NOT_FOUND, Optional.empty()));
                continue;
            }

            DiscFileSystem fs = null;
            try {

                try {
                    fs = new DiscFileSystem(dataDir.resolve(discName));
                } catch (NotRawImageException e) {
                    problems.add(new SetupProblem(discId.name(), NOT_RAW_IMAGE, Optional.empty()));
                    continue;
                }

                if (identify(fs) != discId) {
                    problems.add(new SetupProblem(discId.name(), WRONG_DISC, Optional.empty()));
                    continue;
                }

                List<EntryProblem> entryProblems = verify(fs, loadBundledTable(discId), progress);
                if (!entryProblems.isEmpty())
                    problems.add(new SetupProblem(discId.name(), BAD_ENTRIES, Optional.of(entryProblems)));

            } finally {
                if (fs != null)
                    fs.onDispose();
            }
        }

        return problems;
    }

    public List<RoomRange> readRoomTable(DiscFileSystem fs) throws IOException {
        byte[] slussyBacca = fs.readFile(fs.findFile(DISC_1_PROGRAM));
        long loadAddress = u32LE(slussyBacca, 0x18);

        List<RoomRange> ranges = new ArrayList<>();

        long tableRam = ParasiteEve.DISC_1_PROG_TBL_RAM;
        long tableFile = (tableRam - loadAddress) + 0x800;
        for (int i = 0; i < ParasiteEve.ROOM_COUNT; i++) {
            long start = u32LE(slussyBacca, (int) (tableFile + i * 8));
            long packed = u32LE(slussyBacca, (int) (tableFile + i * 8 + 4));
            long scratchSectors = packed & 0xFF;
            long textureSectors = (packed >> 8) & 0xFFF;
            long roomSectors = (packed >> 20) & 0xFFF;

            ranges.add(new RoomRange(start, scratchSectors, textureSectors, roomSectors));
        }

        return ranges;
    }

    public void ripImages(DiscFileSystem fs, Path outFile) throws IOException {
        List<RoomRange> ranges = readRoomTable(fs);
        DiscEntry pe = fs.findFile("PE.IMG");
        LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();

        for (int i = 0; i < ranges.size(); i++) {
            RoomRange range = ranges.get(i);
            if (range.scratch() == 0 && range.texture() == 0 && range.room() == 0)
                continue;

            int map = i + 1;
            String mapName = "m";
            if (map <= 9)
                mapName = "m00" + map;
            else if (map >= 10 && map < 100)
                mapName = "m0" + map;
            else if (map >= 100)
                mapName = "m" + map;

            byte[] scratch = fs.readSectors(pe, range.start(), (int) range.scratch());
            byte[] texture = fs.readSectors(pe, range.start() + range.scratch(), (int) range.texture());
            byte[] room = fs.readSectors(pe, range.start() + range.scratch() + range.texture(), (int) range.room());

            byte[] scratchConvert = convertImagesToBytes(readVramUploads(scratch));
            byte[] textureConvert = convertImagesToBytes(readVramUploads(texture));

            byte[] backgroundBlock = RoomParser.readBackgroundBlock(room);

            // Images
            entries.put("images/" + mapName + "/scratch", scratchConvert);
            entries.put("images/" + mapName + "/texture", textureConvert);

            // Rooms
            entries.put("rooms/" + mapName + "/background", backgroundBlock);
            entries.put("rooms/" + mapName + "/container", room);
        }

        int[] boundaries = readResidentBoundaries(fs);

        for (int bank = 10; bank <= 13; bank++) {
            int modelRange = bank + 8;
            int texRange = bank + 3;

            byte[] blob = fs.readSectors(pe, boundaries[modelRange], boundaries[modelRange + 1] - boundaries[modelRange]);
            byte[] tex = fs.readSectors(pe, boundaries[texRange], boundaries[texRange + 1] - boundaries[texRange]);

            entries.put("entity/bank/" + bank + "/container", blob);
            entries.put("images/entity/bank/" + bank, convertImagesToBytes(readVramUploads(tex)));
        }

        UPDHSerializer.write(outFile, entries);
    }

    public int[] readResidentBoundaries(DiscFileSystem fs) throws IOException {
        byte[] exe = fs.readFile(fs.findFile(DISC_1_PROGRAM));
        long loadAddress = u32LE(exe, 0x18);
        long at = (RESIDENT_TABLE_RAM - loadAddress) + 0x800;

        List<Integer> list = new ArrayList<>();
        while (true) {
            int v = u16LE(exe, (int) at);
            if (!list.isEmpty() && v < list.get(list.size() - 1)) break;
            list.add(v);
            at = at + 2;
        }

        return list.stream().mapToInt(Integer::intValue).toArray();
    }

}