package com.duckcraftian.upf.pe.effects;

import com.duckcraftian.upf.pe.script.ScriptVM;
import org.tinylog.Logger;

import java.util.Arrays;

import static com.duckcraftian.upf.pe.ParasiteEve.FX_ROOM_OVERLAY_BASE;
import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;

public class ProcessManager {

    public static final int[] slotState = new int[22];
    public static final int[] slotCommand = new int[22];
    public static final int[] slotTicks = new int[22];
    public static final int[] slotOwnerKind = new int[22];
    public static final int[] slotLiveSlots = new int[22];
    public static final int[] slotPc = new int[22];

    public static final EffectProgram[] slotProgram = new EffectProgram[22];
    public static final ScriptVM[] slotOwner = new ScriptVM[22];

    public static final ProcessCommand[] handlers = new ProcessCommand[86];
    public static final EffectProgram[] fxPrograms = new EffectProgram[104];

    public static boolean programsLoadedFlag;

    public static void loadExePrograms() {
        for (int i = 0; i < 23; i++)
            fxPrograms[i] = EffectProgram.EXE_PROGRAMS[i];
    }

    public static void loadRoomPrograms(byte[] container) {
        long dirOffset = u32LE(container, 4);
        long listWord = u32LE(container, (int) (dirOffset + 4));
        long count = listWord >> 22;
        long listAt = listWord & 0x3FFFFF;
        for (int i = 0; i < count; i++) {
            long recordAt = listAt + 12 * i;
            long size = u32LE(container, (int) recordAt);
            long slotWord = u32LE(container, (int) (recordAt + 4));
            long pointer = u32LE(container, (int) (recordAt + 8));

            long slot = slotWord >> 24;
            long dataAt = slotWord & 0xFFFFFF;
            if (slot >= 0x55) {
                int index = (int) (slot - 0x55);
                if (fxPrograms[index] == null) {
                    byte[] overlay = Arrays.copyOfRange(container, (int) dataAt, (int) (dataAt + size));
                    fxPrograms[index] = EffectProgram.parse(overlay, FX_ROOM_OVERLAY_BASE, pointer);
                    Logger.debug("Loaded Program Index: " + index +  ", fxPrograms[index].script.length: " + fxPrograms[index].script.length + ", initializeAddress=0x" + Integer.toHexString(fxPrograms[32].initializeAddress));
                }
            }
        }

        Logger.debug("Loaded " + count + " Programs");

    }

    public static void resetSlot(int slot) {
        slotState[slot] = 0;
        slotCommand[slot] = -1;
        slotOwnerKind[slot] = -1;
        slotLiveSlots[slot] = -1;
        slotTicks[slot] = 0;
        slotOwner[slot] = null;
    }

    public static void clearSlot(int slot) {
        if (slotCommand[slot] == 0x72) {
            for (int i = 23; i < 30; i++) fxPrograms[i] = null;
            programsLoadedFlag = false;
        }

        resetSlot(slot);
    }

    public static int findSlot(int command) {
        int first = 0;
        int last = 10;
        if (command >= 70 && command <= 84) {
            first = 11;
            last = 21;
        }

        for (int i = first; i <= last; i++) if (slotState[i] == 0) return i;
        return -1;
    }

    public static int startSlot(int slot) {
        if (slot < 0 || slot >= 22) return -16;
        int st = slotState[slot];

        if (st < 1 || st > 2) return 0;
        int cmd = slotCommand[slot];
        if (cmd < 0 || cmd >= 0xC0) return -17;
        if (cmd >= 0x55) cmd = 0x55;
        if (handlers[cmd] == null) return -18;
        return handlers[cmd].start(slot);
    }

    public static int execSlot(int slot) {
        if (slot < 0 || slot >= 22) return -19;
        int st = slotState[slot];
        if (st != 1 && st != 2 && st != 4 && st != 5) return 0;
        if (slotState[slot] == 4) {
            slotState[slot] = 5;
            return 0;
        }

        if (slotState[slot] == 5) {
            clearSlot(slot);
            return 0;
        }

        if (slotState[slot] == 1) slotState[slot] = 2;
        int cmd = slotCommand[slot];
        if (cmd < 0 || cmd >= 0xC0) return -20;
        if (cmd >= 0x55) cmd = 0x55;
        if (handlers[cmd] == null) return -21;
        int ret = handlers[cmd].execute(slot);
        slotTicks[slot] += 1;
        return ret;
    }

    public static int stop(int slot, ScriptVM owner, int force) {
        if (slot < 0 || slot >= 22) return -22;
        int st = slotState[slot];

        if (st == 0 || st == 6) return 0;
        if (force == 0 && slotOwner[slot] != owner) return 0;

        int cmd = slotCommand[slot];
        if (cmd < 0 || cmd >= 0xC0) return -23;
        if (cmd >= 0x55) cmd = 0x55;
        if (handlers[cmd] == null) return -24;

        int result = handlers[cmd].stop(slot);
        clearSlot(slot);
        return result;
    }

    public static int bindProgram(int slot, int index) {
        EffectProgram program = fxPrograms[index];
        if (program == null) {
            Logger.error("No EffectProgram at Index: " + index);
            return -1;
        }

        slotProgram[slot] = program;
        slotPc[slot] = 0;
        return 0;
    }

    public static int freeOwner(ScriptVM owner) {
        if (owner == null) return -25;
        int result = 0;
        for (int i = 0; i < 11; i++) {
            if (slotOwner[i] == owner) {
                result = stop(i, owner, 1);
                if (result != 0) return result;
                clearSlot(i);
            }
        }

        for (int i = 0; i < 11; i++) {
            if (slotOwner[i + 11] == owner) {
                result = stop(i + 11, owner, 1);
                if (result != 0) return result;
                clearSlot(i + 11);
            }
        }

        return result;
    }

    public static int stopAll() {
        int result = 0;
        for (int i = 0; i < 11; i++) {
            int command = slotCommand[i];
            if ((command >= 0 && command < 8) || (command >= 85 && command <= 114)) {
                result = stop(i, ScriptVM.getPlayer(), 1);
                if (result != 0) return result;
                clearSlot(i);
            }
        }
        return result;
    }

    public static int stopUpperHalf() {
        int result = 0;
        for (int i = 0; i < 11; i++) {
            result = stop(i + 11, null, 1);
            if (result != 0) return result;
            clearSlot(i + 11);
        }
        return result;
    }

    public static int stopLowerHalf() {
        int result = 0;
        for (int i = 0; i < 11; i++) {
            result = stop(i, null, 1);
            if (result != 0) return result;
            clearSlot(i);
        }
        return result;
    }

    public static void stopAllBoth() {
        stopLowerHalf();
        stopUpperHalf();
    }

    public static int sendCmd(int slot, int arg1, int arg2, int arg3, int arg4, int arg5, ScriptVM caller) {
        if (slot < 0 || slot >= 22) return -10;
        int cmd = slotCommand[slot];

        if (cmd < 0 || cmd >= 0xC0) return -11;
        if (cmd >= 0x55) cmd = 0x55;
        if (handlers[cmd] == null) return -12;
        if (arg1 == 1 && arg2 == 0) {
            caller.set(arg3, slotOwnerKind[slot]);
            caller.set(arg4, slotLiveSlots[slot]);
            caller.set(arg5, slotTicks[slot]);
        }

        return handlers[cmd].send(slot, arg1, arg2, arg3, arg4, arg5, caller);
    }

    public static int setGetState(int slot, int set, int value, ScriptVM caller) {
        if (slot < 0 || slot >= 22) return -13;
        int cmd = slotCommand[slot];

        if (cmd < 0 || cmd >= 0x55) cmd = 0x55;
        if (handlers[cmd] == null) return -15;
        if (set == 0) {
            if (value >= 0 && value < 6) slotState[slot] = value;
        } else caller.set(value, slotState[slot]);

        return slotState[slot];
    }

    public static void runBatch() {
        if ((ScriptVM.gameStateFlags & 0x80) == 0) return;
        for (int i = 0; i < 11; i++) startSlot(i + 11);
        if ((ScriptVM.gameStateFlags & 0x104) != 0) return;
        for (int i = 0; i < 11; i++) execSlot(i + 11);
    }

    public static void runPrimary() {
        if ((ScriptVM.gameStateFlags & 0x80) == 0) return;
        for (int i = 0; i < 11; i++) startSlot(i);
        if ((ScriptVM.gameStateFlags & 4) != 0) return;
        for (int slot = 0; slot < 11; slot++) {
            int command = slotCommand[slot];
            if ((ScriptVM.gameStateFlags & 0x100) == 0 || (command >= 85 && command <= 114))
                execSlot(slot);
        }
    }

    public static void roomSetup() {
        if ((ScriptVM.gameStateFlags & 0x80) != 0) return;
        for (int slot = 0; slot < 22; slot++) resetSlot(slot);
        for (int c = 0; c < 8; c++) if (handlers[c] != null) handlers[c].init();
        if (handlers[85] != null) handlers[85].init();
        for (int c = 8; c < 85; c++) if (handlers[c] != null) handlers[c].init();
        programsLoadedFlag = false;
        ScriptVM.gameStateFlags |= 0x80;
    }

    public static void roomTeardown() {
        if ((ScriptVM.gameStateFlags & 0x80) != 0) {
            for (int c = 0; c < 8; c++) if (handlers[c] != null) handlers[c].unload();
            if (handlers[85] != null) handlers[85].unload();
            for (int c = 8; c < 85; c++) if (handlers[c] != null) handlers[c].unload();
            ScriptVM.gameStateFlags &= ~0x80;
        }

        for (int c = 8; c < 85; c++) handlers[c] = null;
        for (int i = 30; i < 104; i++) fxPrograms[i] = null;
    }

    public static int startProcess(int command, ScriptVM owner) {
        if (command < 0 || command >= 192) return -7;
        if (command >= 108 && command <= 114 && !programsLoadedFlag) {
            for (int i = 0; i < 7; i++) fxPrograms[23 + i] = EffectProgram.DISC_PROGRAMS[i];
            programsLoadedFlag = true;
        }

        roomSetup();
        int original = command;
        int subCommand = 0;

        if (command >= 85) {
            subCommand = command - 85;
            command = 85;
        }

        if (handlers[command] == null) return -8;

        int slot = findSlot(command);
        if (slot == -1) return -3;
        slotState[slot] = 1;
        slotCommand[slot] = original;

        Logger.debug("Effect Process Started: Command=" + original + ", slot=" + slot);

        slotOwnerKind[slot] = 0;
        slotLiveSlots[slot] = 0;
        slotTicks[slot] = 0;
        slotOwner[slot] = owner;

        if (command == 85) bindProgram(slot, subCommand);
        handlers[command].initialize(slot);

        return slot;
    }

}