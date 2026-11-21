package com.duckcraftian.upf.pe.effects;

import com.duckcraftian.upf.pe.rooms.PSXViewport;
import com.duckcraftian.upf.pe.script.ScriptVM;
import com.duckcraftian.upf.ps1.PSX;
import org.tinylog.Logger;

import static com.duckcraftian.upf.pe.disc.SquareImageParser.findKeyedAssetTable;
import static com.duckcraftian.upf.system.DataTypeHelper.*;

public class EffectTaskRunner implements ProcessCommand {

    public static final int EndEffectScript = 0;
    public static final int SetEffectVariable = 1;
    public static final int SpawnEffectTask = 2;
    public static final int RepeatEffectScript = 3;
    public static final int WaitEffectScript = 4;
    public static final int HoldOwnerActor = 5;

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

    public static final int[] texturePageWord = new int[4];
    public static final int[] effectModelAt = new int[6];

    public static PSXViewport currentView;

    public static int currentAge;
    public static int currentTask;

    public static int effectImageVariant;


    public static final EffectOp[] scriptOps = new EffectOp[6];
    public static final EffectCallback[][] effectCallbacks = new EffectCallback[104][16];
    public static final boolean[][] warnedCallbacks = new boolean[104][16];

    public static int program114StartupFlag;

    static {

        scriptOps[EndEffectScript] = (slot, a, b) -> {
            ProcessManager.slotPc[slot] -= 3;
            if (count[slot] == 0) {
                ProcessManager.slotState[slot] = 4;
                ScriptVM owner = ProcessManager.slotOwner[slot];
                if (held[slot] != 0 && owner != null) {
                    owner.setCoreFlags(owner.getCoreFlags() & 0xC0FFFFFF);
                    owner.setAttackEffectState(4);
                    held[slot] = 0;
                }
            }
            return false;
        };

        scriptOps[SetEffectVariable] = (slot, a, b) -> {
            variables[slot][a] = b;
            return true;
        };

        scriptOps[SpawnEffectTask] = (slot, a, _) -> {
            spawn(slot, a);
            return true;
        };

        scriptOps[RepeatEffectScript] = (slot, a, b) -> {
            int v = variables[slot][b] - 1;
            if (v < -32768) v += 65536;
            variables[slot][b] = v;

            if (v > 0) {
                ProcessManager.slotPc[slot] = a - 6;
                return true;
            }

            variables[slot][b] = 0;
            return false;
        };

        scriptOps[WaitEffectScript] = (slot, a, b) -> {
            if (b != 1) {
                delay[slot] = a;
                return false;
            }

            if (variables[slot][a] == 0) {
                ProcessManager.slotPc[slot] -= 3;
                return false;
            }

            return true;
        };

        scriptOps[HoldOwnerActor] = (slot, a, b) -> {
            ScriptVM owner = ProcessManager.slotOwner[slot];
            if (a == 0) {
                owner.setCoreFlags((owner.getCoreFlags() & 0xC0FFFFFF) | 0x01000000);
                held[slot] = 1;
            } else {
                if (held[slot] != 0 && owner != null) {
                    if (owner.getAttackEffectState() == 1) owner.setAttackEffectState(2);
                }
            }

            return true;
        };

    }

    @Override
    public void init() {

        for (int i = 0; i < 2; i++) {
            texturePageWord[2 * i] = PSX.getTPage(i, 0, 0x380, 0x100);
            texturePageWord[2 * i + 1] = PSX.getTPage(i, 0, 0x340, 0x100);
        }

        byte[] blob = EffectAssets.startupResourceBlob;
        effectModelAt[0] = findKeyedAssetTable(blob, 0x6DEAE684L);
        effectModelAt[1] = findKeyedAssetTable(blob, 0x8AB5B684L);
        effectModelAt[2] = findKeyedAssetTable(blob, 0x5AB32504L);
        effectModelAt[3] = findKeyedAssetTable(blob, 0x5AB32104L);
        effectModelAt[4] = findKeyedAssetTable(blob, 0x83ACD504L);
        effectModelAt[5] = findKeyedAssetTable(blob, 0x57E8EB04L);

        for (int i = 0; i < 4; i++)
            Logger.debug("texturePageWord[" + i + "]=" + texturePageWord[i]);

        for (int i = 0; i < 6; i++)
            Logger.debug("effectModelAt[" + i + "]=" + effectModelAt[i]);

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
        currentProcess = slot;
        if (held[slot] != 0)
            effectImageVariant = findEffectImageVariant(ProcessManager.slotCommand[slot]);

        for (int i = 0; i < 8; i++) {
            if (taskId[slot][i] != 0xFFFF) {
                PSX.GTE.loadRotation(currentView.rotation());
                PSX.GTE.loadTranslation(currentView.translation());
                currentTask = i;
                currentAge = taskAge[slot][i];
                callEffect(taskId[slot][i], 2, taskStart[slot][i], argument[slot]);
                if (taskEnd[slot][i] != 0) poolDraw(taskEnd[slot][i]);
            }
        }

        return 0;
    }

    @Override
    public int execute(int slot) {
        runScript(slot);
        ScriptVM owner = ProcessManager.slotOwner[slot];
        if (held[slot] != 0 && owner != null) {
            int flags = owner.getCoreFlags();
            int counter = (flags >> 24) & 0x3F;
            if (counter >= 2)
                owner.setCoreFlags((flags & 0xC0FFFFFF) | (((counter - 1) & 0x3F) << 24));

            if (((owner.getCoreFlags() >> 24) & 0x3F) > 0) {
                if (owner.getAttackEffectState() == 0) {
                    if (owner.readCombatField(44) <= 0)
                        ProcessManager.slotState[slot] = 4;
                }
            }

            if (owner.getAttackEffectState() == 0) {
                if ((owner.getCoreFlags() & 0x180E) != 0)
                    ProcessManager.slotState[slot] = 4;

                if (owner.getActionMode() < 2) return 0;
            }
        }

        if (held[slot] != 0) effectImageVariant = findEffectImageVariant(ProcessManager.slotCommand[slot]);

        stepTasks(slot);
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

    public static int findEffectImageVariant(int type) {
        byte[] container = ScriptVM.getRoomContainer();
        long key = 0x73DECD80L;
        while (true) {
            int at = findKeyedAssetTable(container, key);
            if (at == -1) return -1;
            int imageType = u8(container, at + 3);
            long geometry = u32LE(container, at + 8);
            long x = (geometry >> 10) & 0x7FF;
            long y = geometry >> 21;
            if (imageType == type && x == 832) {
                if (y != 256) return 1;
                return 0;
            }

            key += 4;
        }
    }

    public static int spawn(int slot, int id) {
        int free = -1;

        for (int i = 0; i < 8; i++) {
            if (taskId[slot][i] == 0xFFFF) {
                free = i;
                break;
            }
        }

        if (free == -1) return -1;
        count[slot] += 1;
        taskId[slot][free] = id;
        taskAge[slot][free] = 0;

        int size = ProcessManager.slotProgram[slot].sizes[id];
        int payload = cursor[slot];
        used[slot] += size;

        if (used[slot] > 0x97C) {
            payload = 0;
            used[slot] = size;
            cursor[slot] = payload;
        }

        taskEnd[slot][free] = payload + size;
        taskStart[slot][free] = payload;
        currentTask = free;
        int result = callEffect(id, 0, payload, argument[slot]);

        if (result == 0) taskEnd[slot][free] = 0;
        cursor[slot] += size + result;
        used[slot] += result;
        return size + result;
    }

    public static void runScript(int slot) {
        currentProcess = slot;
        boolean running = true;

        while (running) {
            if (delay[slot] != 0) {
                delay[slot] -= 1;
                if (delay[slot] != 0) break;
            }

            int pc = ProcessManager.slotPc[slot];
            int[] script = ProcessManager.slotProgram[slot].script;
            int op = script[pc];
            int a = script[pc + 1];
            int b = script[pc + 2];
            ProcessManager.slotPc[slot] = pc + 3;
            if (op >= 0 && op < 6) {
                if (scriptOps[op] != null) running = scriptOps[op].run(slot, a, b);
            }
        }
    }

    public static void stepTasks(int slot) {
        for (int i = 0; i < 8; i++) {
            if (taskId[slot][i] != 0xFFFF) {
                currentTask = i;
                currentAge = taskAge[slot][i];
                int result = callEffect(taskId[slot][i], 1, taskStart[slot][i], argument[slot]);

                if (taskEnd[slot][i] != 0) {
                    if (result == 2) {
                        if (poolUpdate(taskEnd[slot][i]) == 0) result = 1;
                    } else
                        poolUpdate(taskEnd[slot][i]);
                }

                taskAge[slot][i] += 1;
                if (result == 1) {
                    taskId[slot][i] = 0xFFFF;
                    count[slot] -= 1;
                }
            }
        }
    }

    public static int callEffect(int id, int mode, int state, int argument) {
        int programIndex = ProcessManager.slotCommand[currentProcess] - 85;
        EffectCallback handler = effectCallbacks[programIndex][id];

        if (handler == null) {
            if (!warnedCallbacks[programIndex][id]) {
                warnedCallbacks[programIndex][id] = true;
                Logger.error("Unimplemented Effect <" + programIndex + ">[" + id + "]");
            }

            return 0;
        }

        return handler.run(mode, state, argument);
    }

    public static int poolOpen(int out, int stride, int count, int callbackId) {
        stride += 4;
        put32LE(arena[currentProcess], out, stride);
        put32LE(arena[currentProcess], out + 4, count);
        put32LE(arena[currentProcess], out + 8, callbackId);
        int entry = out + 0xC;

        for (int i = 0; i < count; i++) {
            put16LE(arena[currentProcess], entry, 0);
            entry += stride;
        }

        return stride * count + 0xC;
    }

    public static int poolOpenInTask(int handleAt, int extra, int stride, int count, int callbackId) {
        stride += 4;
        int header = taskEnd[currentProcess][currentTask] + extra;
        put32LE(arena[currentProcess], handleAt, header);
        put32LE(arena[currentProcess], header, stride);
        put32LE(arena[currentProcess], header + 4, count);
        put32LE(arena[currentProcess], header + 8, callbackId);

        int entry = header + 0xC;
        for (int i = 0; i < count; i++) {
            put16LE(arena[currentProcess], entry, 0);
            entry += stride;
        }

        return stride * count + 0xC;
    }

    public static int poolAlloc(int list) {
        int stride = s32LE(arena[currentProcess], list);
        int count = s32LE(arena[currentProcess], list + 4);
        int entry = list + 0xC;
        int i = 0;

        if (count > 0) {
            while (i < count) {
                if (s16LE(arena[currentProcess], entry) == 0) break;
                i += 1;
                entry += stride;
            }

            if (i < count) {
                put16LE(arena[currentProcess], entry, 1);
                put16LE(arena[currentProcess], entry + 2, 0);
                return entry + 4;
            }

        }

        return -1;
    }

    public static int poolUpdate(int list) {
        int savedAge = currentAge;
        int stride = s32LE(arena[currentProcess], list);
        int count = s32LE(arena[currentProcess], list + 4);
        int callbackId = s32LE(arena[currentProcess], list + 8);
        int active = 0;
        int entry = list + 0xC;
        for (int i = 0; i < count; i++) {
            if (s16LE(arena[currentProcess], entry) != 0) {
                active += 1;
                currentAge = s16LE(arena[currentProcess], entry + 2);
                if (callEffect(callbackId, 1, entry + 4, argument[currentProcess]) != 0)
                    put16LE(arena[currentProcess], entry, 0);
                else
                    put16LE(arena[currentProcess], entry + 2, s16LE(arena[currentProcess], entry + 2) + 1);
            }

            entry += stride;
        }

        currentAge = savedAge;
        return active;
    }

    public static int poolDraw(int list) {
        int savedAge = currentAge;
        int stride = s32LE(arena[currentProcess], list);
        int count = s32LE(arena[currentProcess], list + 4);
        int callbackId = s32LE(arena[currentProcess], list + 8);
        int active = 0;
        int entry = list + 0xC;
        for (int i = 0; i < count; i++) {
            if (s16LE(arena[currentProcess], entry) != 0) {
                active += 1;
                currentAge = s16LE(arena[currentProcess], entry + 2);
                callEffect(callbackId, 2, entry + 4, argument[currentProcess]);
            }

            entry += stride;
        }

        currentAge = savedAge;
        return active;
    }

}