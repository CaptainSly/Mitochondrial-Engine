package com.duckcraftian.upf.pe.effects;

import com.duckcraftian.upf.pe.rooms.PSXViewport;
import com.duckcraftian.upf.pe.script.ScriptVM;
import com.duckcraftian.upf.ps1.PSX;
import com.duckcraftian.upf.ps1.PSXMath;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.duckcraftian.upf.pe.disc.SquareImageParser.findKeyedAssetTable;
import static com.duckcraftian.upf.ps1.PSX.*;
import static com.duckcraftian.upf.system.DataTypeHelper.*;

public class EffectTaskRunner implements ProcessCommand {

    public static final int EndEffectScript = 0;
    public static final int SetEffectVariable = 1;
    public static final int SpawnEffectTask = 2;
    public static final int RepeatEffectScript = 3;
    public static final int WaitEffectScript = 4;
    public static final int HoldOwnerActor = 5;

    // Fan Sweep Program Variables
    public static final int FanSweepController = 0;
    public static final int FanSweepProgram = 7;
    public static final int FanSweepSpark = 8;
    public static final int FanSweepEmber = 9;

    public static final int[] fanSweepAnchor = new int[3];
    public static int fanSweepTrailPool;

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

    public static final List<Integer> effectVertices = new ArrayList<>();

    public static PSXViewport currentView;

    public static int currentAge;
    public static int currentTask;

    public static int effectImageVariant;
    public static int renderPalette, renderCellLayout, renderTpage, renderShape, renderDepth, renderCellWidth, renderCellHeight;

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

        effectCallbacks[FanSweepProgram][FanSweepSpark] = (mode, spark, argument) -> {
            byte[] a = arena[currentProcess];
            if (mode == 1) {
                put16LE(a, spark + 4, u16LE(a, spark + 4) + 1);
                int y = (currentAge * 700) / 36 - 700 + fanSweepAnchor[1];
                int x = fanSweepAnchor[0] + (PSXMath.rcos(s16LE(a, spark + 2)) * s16LE(a, spark + 12)) / 4096;
                int z = fanSweepAnchor[2] + (PSXMath.rsin(s16LE(a, spark + 2)) * s16LE(a, spark + 12)) / 4096;

                put16LE(a, spark + 8, y);
                put16LE(a, spark + 6, x);
                put16LE(a, spark + 10, z);
                put16LE(a, spark + 2, u16LE(a, spark + 2) + 0x80);
                if ((PSX.rand() & 7) == 0) {
                    int point = poolAlloc(fanSweepTrailPool);
                    if (point != -1) {
                        put16LE(a, point, s16LE(a, spark + 6));
                        put16LE(a, point + 2, s16LE(a, spark + 8));
                        put16LE(a, point + 4, s16LE(a, spark + 10));
                    }
                }
                if (currentAge < 0x24) return 0;
                return 1;
            }

            if (mode == 2) {
                int st = s16LE(a, spark);
                int timer = s16LE(a, spark + 4);
                int size, alpha;
                if (st == 0) {
                    size = PSXMath.rsin((timer << 10) / 12);
                    alpha = 0x40;
                    if (timer >= 12) {
                        put16LE(a, spark + 4, 0);
                        put16LE(a, spark, 1);
                    }
                } else if (st == 1) {
                    size = 0x1000;
                    alpha = 0x40;
                    if (timer >= 12) {
                        put16LE(a, spark + 4, 0);
                        put16LE(a, spark, 2);
                    }
                } else {
                    size = 0x1000;
                    alpha = PSXMath.rcos((timer << 10) / 12) / 64;
                    put16LE(a, spark + 12, s16LE(a, spark + 12) + timer * 4);
                }
                int[] position = {s16LE(a, spark + 6), s16LE(a, spark + 8), s16LE(a, spark + 10)};
                size = size * 3 / 2;
                int[] spin = {0, 0, currentAge << 7, 0};
                int kind = renderPalette;
                int row = u16LE(EffectAssets.drawTable, 0x20 + 2 * kind) + 3;
                if (kind == 4 && effectImageVariant != 0) row = u16LE(EffectAssets.drawTable, 0x20 + 2 * kind) + 7;
                int[] color = {u8(EffectAssets.fxConstants, 0x6C), u8(EffectAssets.fxConstants, 0x6D), u8(EffectAssets.fxConstants, 0x6E)};
                drawEffectShape(position, spin, size, size, 0x24, PSX.getClut(0, row), 1, alpha, color);
                return 0;
            }

            return 0;
        };

        effectCallbacks[FanSweepProgram][FanSweepEmber] = (mode, state, _) -> {
            byte[] a = arena[currentProcess];
            if (mode == 1) {
                put16LE(a, state, s16LE(a, state) + (PSX.rand() & 7) - 3);
                put16LE(a, state + 2, s16LE(a, state + 2) + (PSX.rand() & 3) - 7);
                put16LE(a, state + 4, s16LE(a, state + 4) + (PSX.rand() & 7) - 3);
                if (currentAge < 0x12) return 0;
                return 1;
            }

            if (mode == 2) {
                int[] position = {s16LE(a, state), s16LE(a, state + 2), s16LE(a, state + 4)};
                byte[] color = new byte[4];
                sampleColorTrack(EffectAssets.emberColorTrack, currentAge, color);
                int[] sample = {color[0] & 0xFF, color[1] & 0xFF, color[2] & 0xFF, 0};
                drawTileAndHalo(position, sample, 0x80, 1);
                return 0;
            }

            return 0;
        };

        effectCallbacks[FanSweepProgram][FanSweepController] = (mode, payload, argument) -> {
            byte[] a = arena[currentProcess];
            int end = taskEnd[currentProcess][currentTask];

            if (mode == 0) {
                put32LE(a, payload, PSX.rand());
                int handles = poolOpen(end, 0x10, 0x18, FanSweepSpark);
                handles += poolOpenInTask(payload + 4, handles, 8, 0x12, FanSweepEmber);
                return handles;
            }

            if (mode == 1) {
                if (currentAge < 0x20 && (currentAge & 1) != 0) {
                    int spark = poolAlloc(end);
                    if (spark != -1) {
                        put16LE(a, spark + 2, s32LE(a, payload));
                        put16LE(a, spark + 12, 0xC8);
                        put16LE(a, spark, 0);
                        put16LE(a, spark + 4, 0);
                        put32LE(a, payload, s32LE(a, payload) - 0x555);

                    }
                }

                if (currentAge >= 0x46) return 1;
                poolUpdate(s32LE(a, payload + 4));
                fanSweepTrailPool = s32LE(a, payload + 4);
                return 0;
            }

            if (mode == 2) {
                GTE.loadRotation(currentView.rotation());
                GTE.loadTranslation(currentView.translation());
                renderDepth = 8;
                poolDraw(s32LE(a, payload + 4));

                ScriptVM player = ScriptVM.getPlayer();
                fanSweepAnchor[0] = player.getPosX() >> 16;
                fanSweepAnchor[1] = player.getPosY() >> 16;
                fanSweepAnchor[2] = player.getPosZ() >> 16;
                renderCellWidth = 0x20;
                renderCellHeight = 0x20;
                renderTpage = texturePageWord[u16LE(EffectAssets.drawTable, 18)];
                renderPalette = 1;
                EffectAssets.selectEffectPage(1);
                renderCellLayout = 1;
                renderShape = 0;
                return 0;
            }

            return 0;
        };

    }

    @Override
    public void init() {

        for (int i = 0; i < 2; i++) {
            texturePageWord[2 * i] = getTPage(i, 0, 0x380, 0x100);
            texturePageWord[2 * i + 1] = getTPage(i, 0, 0x340, 0x100);
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
                GTE.loadRotation(currentView.rotation());
                GTE.loadTranslation(currentView.translation());
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

    public static void drawEffectShape(int[] position, int[] rotation, int scaleX, int scaleY, int texture, int clut, int page, int intensity, int[] color) {
        if (rotation == null) rotation = new int[]{0, 0, 0, 0};
        int tpageWord = renderTpage;
        if (page != 0xFF) tpageWord = renderTpage | getTPage(0, page, 0, 0);
        int r, g, b;
        if (color == null) {
            r = intensity;
            g = intensity;
            b = intensity;
        } else {
            r = intensity * (color[0] & 0xFF) / 128;
            g = intensity * (color[1] & 0xFF) / 128;
            b = intensity * (color[2] & 0xFF) / 128;
        }
        int rgb = packRGBColor(r, g, b);
        if (page != 0xFF) rgb |= 0x01000000;

        int u, v;
        if (renderCellLayout != 0) {
            u = texture & 0xF;
            v = 0;
            if (u >= 8) {
                u -= 8;
                v = 0x20;
            }

            u <<= 4;
            v += (texture / 16) * 16;
        } else {
            u = (texture & 0xF) << 4;
            v = (texture / 16) * 16;
        }
        if (renderPalette == 4 && effectImageVariant != 0) v += 0x60;
        int width = renderCellWidth;
        int height = renderCellHeight;
        int sx = scaleX * (width >> 4);
        int sy = scaleY * (height >> 4);

        GTE.loadRotation(currentView.rotation());
        GTE.loadTranslation(currentView.translation());
        GTE.writeData(0, ((position[1] << 16) | (position[0] & 0xFFFF)));
        GTE.writeData(1, position[2]);
        GTE.execute(CMD_MVMVA);

        int[] translation = {GTE.readData(25), GTE.readData(26), GTE.readData(27)};
        int[] matrix = PSXMath.rotMatrixYXZ(rotation[0], rotation[1], rotation[2]);
        PSXMath.scaleMatrix(matrix, sx, sy, 0x1000);
        if (rotation[3] == 1) PSXMath.mulRotMatrix(matrix);
        GTE.loadRotation(matrix);
        GTE.loadTranslation(translation);

        int quads = u16LE(EffectAssets.drawTable, 0x2C + 2 * renderShape);
        int first = (int) (u32LE(EffectAssets.drawTable, 0x1D8 + 4 * renderShape) - 0x800E11E4);

        for (int q = 0; q < quads; q++) {
            long at = first + 32 * q;
            for (int k = 0; k < 3; k++) {
                GTE.writeData(2 * k, ((s16LE(EffectAssets.drawTable, (int) (at + 8 * k + 2)) << 16) | (s16LE(EffectAssets.drawTable, (int) (at + 8 * k)) & 0xFFFF)));
                GTE.writeData(2 * k + 1, s16LE(EffectAssets.drawTable, (int) (at + 8 * k + 4)));
            }

            GTE.execute(0x4A280030);
            int sxy0 = GTE.readData(12);
            int sxy1 = GTE.readData(13);
            int sxy2 = GTE.readData(14);
            if (GTE.readData(24) == 0) break;
            int depth = GTE.readData(19) >> 2;
            GTE.writeData(0, ((s16LE(EffectAssets.drawTable, (int) (at + 26)) << 16) | (s16LE(EffectAssets.drawTable, (int) (at + 24)) & 0xFFFF)));
            GTE.writeData(1, s16LE(EffectAssets.drawTable, (int) (at + 28)));
            GTE.execute(0x4A180001);
            int sxy3 = GTE.readData(14);
            depth -= renderDepth;
            if (depth >= 0 && depth < 0x1000) {
                pushVertex(sxy0, u, v, tpageWord, clut, depth, rgb);
                pushVertex(sxy1, u + width - 1, v, tpageWord, clut, depth, rgb);
                pushVertex(sxy2, u, v + height - 1, tpageWord, clut, depth, rgb);
                pushVertex(sxy1, u + width - 1, v, tpageWord, clut, depth, rgb);
                pushVertex(sxy3, u + width - 1, v + height - 1, tpageWord, clut, depth, rgb);
                pushVertex(sxy2, u, v + height - 1, tpageWord, clut, depth, rgb);
            }
        }
    }


    public static void pushVertex(int sxy, int u, int v, int tpageWord, int clut, int depth, int rgb) {
        int x = sxy & 0xFFFF;
        if (x >= 0x8000) x -= 0x10000;
        int y = (sxy >> 16) & 0xFFFF;
        if (y >= 0x8000) y -= 0x10000;

        effectVertices.add(x);
        effectVertices.add(y);
        effectVertices.add(u);
        effectVertices.add(v);
        effectVertices.add(tpageWord);
        effectVertices.add(clut);
        effectVertices.add(depth);
        effectVertices.add(rgb);
    }

    public static void sampleColorTrack(byte[] track, int time, byte[] out) {
        int count = s32LE(track, 4);
        int keyAt = 0;
        if (s32LE(track, 0) == 0) {
            count = 0;
            int total = 0;
            keyAt = 8;
            while (true) {
                int length = u8(track, keyAt + 3);
                if (length == 0) break;
                put16LE(track, keyAt + 6, total);
                total += length;
                count += 1;
                put16LE(track, keyAt + 4, length);
                keyAt += 8;
            }
            put32LE(track, 0, total);
            put32LE(track, 4, count);
        }

        if (time > s32LE(track, 0)) time = s32LE(track, 0);
        keyAt = 8 + (count * 8 - 8);
        for (int i = 0; i < count; i++) {
            if (time >= u16LE(track, keyAt + 6)) break;
            keyAt -= 8;
        }

        int length = u16LE(track, keyAt + 4);
        int fraction = ((time - u16LE(track, keyAt + 6)) << 12) / length;
        PSXMath.loadAverageCol(track, keyAt, track, keyAt + 8, 4096 - fraction, fraction, out, 0);
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

    public static void drawTileAndHalo(int[] position, int[] color, int scale, int abr) {
        GTE.writeData(0, ((position[1] << 16) | (position[0] & 0xFFFF)));
        GTE.writeData(1, position[2]);
        GTE.execute(0x4A180001);
        int sxy = GTE.readData(14);
        int depth = (GTE.readData(19) >> 2) - renderDepth;
        if (depth < 0 || depth >= 0x1000) return;
        int r = (color[0] * scale / 128) & 0xFF;
        int g = (color[1] * scale / 128) & 0xFF;
        int b = (color[2] * scale / 128) & 0xFF;
        int x = sxy & 0xFFFF;
        if (x >= 0x8000) x -= 0x10000;
        int y = (sxy >> 16) & 0xFFFF;
        if (y >= 0x8000) y -= 0x10000;
        int blend = 0;
        int glowSemi = 0x01000000;
        if (abr == 0xFF) glowSemi = 0;
        else blend = abr & 3;

        int glowRGB = (r >> 2) | ((g >> 2) << 8) | ((b >> 2) << 16) | glowSemi;
        int coreRGB = r | (g << 8) | (b << 16) | 0x01000000;

        pushVertex(packXY(x - 1, y - 1), 0, 0, -1, blend, depth, glowRGB);
        pushVertex(packXY(x + 2, y - 1), 0, 0, -1, blend, depth, glowRGB);
        pushVertex(packXY(x - 1, y + 2), 0, 0, -1, blend, depth, glowRGB);
        pushVertex(packXY(x + 2, y - 1), 0, 0, -1, blend, depth, glowRGB);
        pushVertex(packXY(x + 2, y + 2), 0, 0, -1, blend, depth, glowRGB);
        pushVertex(packXY(x - 1, y + 2), 0, 0, -1, blend, depth, glowRGB);

        pushVertex(packXY(x, y), 0, 0, -1, blend, depth, coreRGB);
        pushVertex(packXY(x + 1, y), 0, 0, -1, blend, depth, coreRGB);
        pushVertex(packXY(x, y + 1), 0, 0, -1, blend, depth, coreRGB);
        pushVertex(packXY(x + 1, y), 0, 0, -1, blend, depth, coreRGB);
        pushVertex(packXY(x + 1, y + 1), 0, 0, -1, blend, depth, coreRGB);
        pushVertex(packXY(x, y + 1), 0, 0, -1, blend, depth, coreRGB);
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

    private static int packXY(int x, int y) {
        return ((y & 0xFFFF) << 16) | (x & 0xFFFF);
    }

}