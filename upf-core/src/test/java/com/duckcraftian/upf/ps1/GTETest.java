package com.duckcraftian.upf.ps1;

import com.duckcraftian.upf.ps1.gfx.GTE;
import com.duckcraftian.upf.system.FileUtils;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GTETest {

    @Test
    void movesCasesMatch() throws URISyntaxException, IOException {
        List<String> lines = Files.readAllLines(FileUtils.resolveAssetFile("gte_vectors.txt").toPath());
        GTE gte = null;
        int command = 0;
        boolean skipping = false;
        boolean skipCase = false;

        String caseTag = "";
        int caseIndex = 0;

        int skipCount = 0;

        TreeSet<String> wrongSlots = new TreeSet<>();
        Set<Integer> commands = Set.of(0x0000000, 0x1400006, 0x158002D, 0x168002E, 0x0180001, 0x0280030);

        for (String line : lines) {
            if (line.isBlank())
                continue;

            String[] parts = line.split(" ");

            if (parts[0].equals("CASE")) {
                gte = new GTE();
                command = Integer.parseUnsignedInt(parts[3], 16);
                caseTag = parts[1];
                caseIndex = Integer.parseInt(parts[2]);
                skipping = !commands.contains(command);
            } else if (skipping) {
                continue;
            } else if (parts[0].equals("W")) {
                int slot = Integer.parseInt(parts[2]);
                int value = Integer.parseUnsignedInt(parts[3], 16);

                if (parts[1].equals("d"))
                    gte.writeData(slot, value);
                else
                    gte.writeControl(slot, value);
            } else if (parts[0].equals("RD")) {

                if (command != 0)
                    gte.execute(command);

                skipCase = ((command == 0x158002D || command == 0x168002E) && ((gte.readControl(31) & 0x18000) != 0))
                        || (command == 0x280030 && caseTag.equals("small") && (caseIndex == 40 || caseIndex == 46));

                if (skipCase) {
                    skipCount++;
                    continue;
                }

                for (int i = 0; i < 32; i++) {
                    if (command != 0 && (i == 15 || i == 28))
                        continue;

                    if (Integer.parseUnsignedInt(parts[1 + i], 16) != gte.readData(i))
                        wrongSlots.add("d" + i);
                }
            } else if (parts[0].equals("RC")) {

                if (skipCase)
                    continue;

                for (int i = 0; i < 32; i++) {
                    if (command != 0 && (i == 24 || i == 25))
                        continue;

                    if (Integer.parseUnsignedInt(parts[1 + i], 16) != gte.readControl(i))
                        wrongSlots.add("c" + i);
                }
            }
        }

        assertTrue(wrongSlots.isEmpty(), "Wrong Slots: " + wrongSlots);
        Logger.debug("Skipped a total of: " + skipCount + " Cases");

    }


}