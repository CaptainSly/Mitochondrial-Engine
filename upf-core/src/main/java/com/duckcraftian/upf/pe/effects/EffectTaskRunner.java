package com.duckcraftian.upf.pe.effects;

import com.duckcraftian.upf.pe.script.ScriptVM;

public class EffectTaskRunner implements ProcessCommand {

    public static int currentProcess;

    public static final int[] argument = new int[22];
    public static final int[] count = new int[22];
    public static final int[] held = new int[22];
    public static final int[] delay = new int[22];
    public static final int[] used = new int[22];
    public static final int[] cursor = new int[22];

    public static final int[][] variables = new int[22][7];
    public static final int[][] taskId = new int[22][8];
    public static final int[][] taskAge = new int[22][8];
    public static final int[][] taskStart = new int[22][8];
    public static final int[][] taskEnd = new int[22][8];
    public static final byte[][] arena = new byte[22][0x97C];

    public static int program114StartupFlag;

    @Override
    public void init() {

    }

    public int initialize(int slot) {
        ProcessManager.slotOwnerKind[slot] = 1;
        ProcessManager.slotLiveSlots[slot] = 0;
        delay[slot] = 0;
        used[slot] = 0;
        cursor[slot] = 0;
        count[slot] = 0;
        held[slot] = 0;
        for (int i = 6; i >= 0; i--) variables[slot][i] = 0;
        for (int i = 0; i < 8; i++) {
            taskId[slot][i] = 0xFFFF;
            taskAge[slot][i] = 0;
            taskStart[slot][i] = 0;
        }

        return 0;
    }

    @Override
    public int send(int slot, int arg1, int arg2, int arg3, int arg4, int arg5, ScriptVM caller) {
        if (arg1 == 0) {
            int startup = ProcessManager.slotProgram[slot].initializeAddress;
            currentProcess = slot;

            switch (startup) {
                case 0x801F1B00, 0x801F1B10, 0x801F1B20, 0x801F1B30, 0x801F1B40, 0x801F1B50:
                    argument[slot] = 0x801F1F28;
                    break;
                case 0x801F1B60:
                    if (arg2 == 1) {
                        if (program114StartupFlag == 0) argument[slot] = 1;
                        else {
                            program114StartupFlag = 0;
                            argument[slot] = 0;
                        }
                    } else argument[slot] = 0;

                    break;
            }

        }
        return 0;
    }

    @Override
    public int start(int slot) {
        return 0;
    }

    @Override
    public int execute(int slot) {
        return 0;
    }

    @Override
    public int stop(int slot) {
        ProcessManager.slotState[slot] = 4;
        return 0;
    }

    @Override
    public void unload() {

    }


}