package com.duckcraftian.upf.pe.disc;

import com.duckcraftian.upf.ps1.disc.DiscEntry;
import com.duckcraftian.upf.ps1.disc.DiscFileSystem;
import com.duckcraftian.upf.ps1.disc.RawDiscEntry;
import com.duckcraftian.upf.utils.ProgressListener;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static com.duckcraftian.upf.pe.ParasiteEve.DISC_1_PROGRAM;
import static com.duckcraftian.upf.pe.ParasiteEve.DISC_2_PROGRAM;
import static com.duckcraftian.upf.pe.disc.EntryProblem.EntryProblemKind.*;
import static com.duckcraftian.upf.ps1.disc.DiscConstants.DATA_2048;
import static com.duckcraftian.upf.ps1.disc.DiscConstants.RAW_2352;

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

}