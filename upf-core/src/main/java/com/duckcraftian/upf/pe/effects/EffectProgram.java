package com.duckcraftian.upf.pe.effects;

import static com.duckcraftian.upf.pe.ParasiteEve.*;
import static com.duckcraftian.upf.system.DataTypeHelper.*;

public class EffectProgram {

    public static final EffectProgram[] EXE_PROGRAMS = new EffectProgram[23];
    public static final EffectProgram[] DISC_PROGRAMS = new EffectProgram[7];

    private int[] callbacks;
    int[] sizes;
    int[] script;
    byte[] progData;
    long progBase;

    int initializeAddress;
    private int scriptAddress;

    public EffectProgram() {
        callbacks = new int[8];
        sizes = new int[8];
    }

    public static void loadAll(byte[] exePrograms, byte[] discPrograms) {
        for (int i = 0; i < 23; i++) {
            long p = u32LE(exePrograms, 4 * i);
            EXE_PROGRAMS[i] = parse(exePrograms, FX_PROGRAMS_RAM, p);
        }

        for (int i = 0; i < DISC_PROGRAMS.length; i++) {
            DISC_PROGRAMS[i] = parse(discPrograms, FX_DISC_BASE, FX_DISC_PROGRAM_RAM[i]);
        }
    }

    public static EffectProgram parse(byte[] data, long base, long programAddress) {
        int at = (int) (programAddress - base);
        EffectProgram program = new EffectProgram();
        program.progData = data;
        program.progBase = base;
        for (int k = 0; k < 8; k++) {
            program.callbacks[k] = (int) u32LE(data, at + 4 * k);
            program.sizes[k] = u16LE(data, at + 0x20 + 2 * k);
        }

        program.initializeAddress = (int) u32LE(data, at + 0x30);
        program.scriptAddress = (int) u32LE(data, at + 0x34);
        int scriptAt = (int) (program.scriptAddress - base);
        int count = 0;

        int op = -1;
        while (op != 0) {
            op = s16LE(data, scriptAt + 6 * count);
            count += 1;
        }

        program.script = new int[3 * count];

        for (int i = 0; i < count; i++) {
            program.script[3 * i] = s16LE(data, scriptAt + 6 * i);
            program.script[3 * i + 1] = s16LE(data, scriptAt + 6 * i + 2);
            program.script[3 * i + 2] = s16LE(data, scriptAt + 6 * i + 4);
        }

        return program;
    }

}