package com.duckcraftian.upf.pe.effects;

import com.duckcraftian.upf.pe.ParasiteEve;
import com.duckcraftian.upf.pe.audio.PEAudio;
import com.duckcraftian.upf.pe.rooms.PSXViewport;
import com.duckcraftian.upf.pe.script.ScriptVM;
import com.duckcraftian.upf.ps1.PSX;
import com.duckcraftian.upf.ps1.PSXMath;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.duckcraftian.upf.pe.ParasiteEve.PEAUDIO;
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

    // Map005 Program Variables
    public static final int RoomM005Program = 32;
    public static final int RoomM005Spawner = 0;
    public static final int RoomM005CrossFlare = 1;
    public static final int RoomM005Beacon = 2;
    public static final int RoomM005Orbiter = 8;
    public static final int RoomM005DriftingSprite = 9;

    public static byte[] roomM005BeamModel, roomM005SwirlModel;

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

    public static int modelTpage, modelClut, modelSemi;
    public static int modelDepthBias = 0;
    public static final int[] modelColorBackup = new int[0x100];

    public static int currentAge;
    public static int currentTask;

    public static int effectImageVariant;
    public static int renderPalette, renderCellLayout, renderTpage, renderShape, renderDepth, renderCellWidth, renderCellHeight;
    public static int renderUnitSize = 0, renderFrameStride = 0;

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
                sampleColorTrack(EffectAssets.emberColorTrack, 0, currentAge, color);
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

        effectCallbacks[RoomM005Program][RoomM005Orbiter] = (mode, orb, argument) -> {
            byte[] a = arena[currentProcess];
            if (mode == 1) {
                int state = s16LE(a, orb + 0xC);
                if (state == 0) {
                    int anchorX = programShort(0x80190B80L, 2);
                    int anchorZ = programShort(0x80190B80L, 4);
                    int phase = s16LE(a, orb + 8);
                    int radius = s16LE(a, orb + 0xA);
                    put16LE(a, orb, toS16(anchorX + PSXMath.rcos(phase) * radius / 4096));
                    put16LE(a, orb + 4, toS16(anchorZ + PSXMath.rsin(phase) * radius / 4096));
                    put16LE(a, orb + 8, phase + 0x18);

                    if (s16LE(a, orb + 6) < 0x81) put16LE(a, orb + 6, s16LE(a, orb + 6) + 8);

                    if (variables[currentProcess][0] == 1) {
                        put16LE(a, orb + 0xC, 1);
                        put16LE(a, orb + 0xE, 0);
                    }

                    return 0;
                }

                if (state == 1) {
                    int n = s16LE(a, orb + 0xE) + 1;
                    put16LE(a, orb + 0xE, n);
                    int[] here = {s16LE(a, orb), s16LE(a, orb + 2), s16LE(a, orb + 4)};
                    int[] target = {programShort(0x80190B80L, 6), programShort(0x80190B80L, 7), programShort(0x80190B80L, 8)};
                    int[] moved = PSXMath.averageShorts(here, target, 0x1000 - (n << 7), n << 7);
                    put16LE(a, orb, moved[0]);
                    put16LE(a, orb + 2, moved[1]);
                    put16LE(a, orb + 4, moved[2]);
                    if (s16LE(a, orb + 0xE) >= 0x20) return 1;
                    return 0;
                }

            }

            if (mode == 2) {
                int[] spin = {0, 0, s16LE(a, orb + 8), 0};
                byte[] sampled = new byte[4];
                EffectProgram program = ProcessManager.slotProgram[currentProcess];
                sampleColorTrack(program.progData, (int) (0x80190AD4L - program.progBase), (s16LE(a, orb + 2) + currentAge) & 0x3F, sampled);
                int[] color = {sampled[0] & 0xFF, sampled[1] & 0xFF, sampled[2] & 0xFF};
                int row = u16LE(EffectAssets.drawTable, 0x20 + 2 * renderPalette);
                if (renderPalette == 4 && effectImageVariant != 0) row += 4;
                int clut = PSX.getClut(0x20, row);
                int[] position = {s16LE(a, orb), s16LE(a, orb + 2), s16LE(a, orb + 4)};
                int size = s16LE(a, orb + 6);
                drawEffectShape(position, spin, 0x800, 0x800, renderFrameStride + 0xD8, clut, 3, size, color);
                drawEffectShape(position, spin, 0x1000, 0x1000, (renderFrameStride << 1) + 0xD8, clut, 3, size, color);
                return 0;
            }

            return 0;
        };

        effectCallbacks[RoomM005Program][RoomM005Spawner] = (mode, payload, argument) -> {
            byte[] a = arena[currentProcess];
            int pool = taskEnd[currentProcess][currentTask];
            if (mode == 0) {
                put16LE(a, payload + 8, 0);
                put16LE(a, payload + 0xA, 0);
                return poolOpen(pool, 0x10, 0x18, RoomM005Orbiter);
            }

            if (mode == 1) {

                if (currentAge == 7) {
                    roomSound(0x5AB, ownerSoundTag(), 0x80, 0x7F);
                    roomSound(0x5AC, 0x80, 0x80, 0x7F);
                }

                if (currentAge < 25) {
                    int[] target = ownerPoint(0);
                    int child = poolAlloc(pool);
                    if (child != -1) {
                        put16LE(a, child + 8, PSX.rand());
                        int spread = PSX.rand() & 0x1FF;
                        put16LE(a, child + 2, target[1] + spread - 0x100);
                        put16LE(a, child + 6, 0);
                        put16LE(a, child + 0xA, (PSX.rand() & 0xFF) + 0x100);
                        put16LE(a, child + 0xC, 0);
                        put16LE(a, child + 0xE, 0);
                    }
                }

                if (s16LE(a, payload + 8) == 1) {
                    put16LE(a, payload + 0xA, u16LE(a, payload + 0xA) + 1);
                    if (s16LE(a, payload + 0xA) >= 32) return 1;
                } else if (variables[currentProcess][0] != 0) {
                    put16LE(a, payload + 8, 1);
                }

                return 0;
            }

            if (mode == 2) {
                int[] here = ownerPoint(0);
                for (int k = 0; k < 3; k++) programPutShort(0x80190B84L, k, here[k]);
                int[] offset = {programShort(0x8018EFF4L, 0), programShort(0x8018EFF4L, 1), programShort(0x8018EFF4L, 2)};
                int[] joint = jointPoint(20, offset);
                for (int k = 0; k < 3; k++) programPutShort(0x80190B8CL, k, joint[k]);
                renderUnitSize = 16;
                renderFrameStride = 1;
                renderCellWidth = 16;
                renderCellHeight = 16;
                renderPalette = 2;
                renderCellLayout = 0;
                renderShape = 0;
                renderDepth = 8;
                renderTpage = texturePageWord[u16LE(EffectAssets.drawTable, 4)];
                return 0;
            }

            return 0;
        };

        effectCallbacks[RoomM005Program][RoomM005CrossFlare] = (mode, payload, argument) -> {

            if (mode == 0) {
                roomSound(0x5F8, ownerSoundTag(), 0x80, 0x7F);
                roomSound(0x5F9, 0x80, 0x80, 0x7F);
                return 0;
            }

            if (mode == 1) {
                if (currentAge >= 35) return 1;
                return 0;
            }

            if (mode == 2) {
                EffectProgram program = ProcessManager.slotProgram[currentProcess];
                renderDepth = 0x40;
                int[] position = jointPoint(9, new int[]{programShort(0x8018F004L, 0), programShort(0x8018F004L, 1), programShort(0x8018F004L, 2)});
                if (currentAge < 35) {
                    renderUnitSize = 0x20;
                    renderFrameStride = 2;
                    renderCellWidth = 0x20;
                    renderCellHeight = 0x20;
                    renderTpage = texturePageWord[u16LE(EffectAssets.drawTable, 6)];
                    renderPalette = 3;
                    renderCellLayout = 0;
                    renderShape = 0;
                    int[] spin = {0, 0, currentAge << 6, 0};
                    int level = PSXMath.rsin((currentAge << 11) / 34);
                    position[0] -= 0x10;
                    int row = u16LE(EffectAssets.drawTable, 0x20 + 2 * renderPalette);
                    drawEffectShape(position, spin, 0x1000, 0x1000, 2, PSX.getClut(0x10, row), 1, level, null);
                    position[0] += 0x20;
                    drawEffectShape(position, spin, 0x1000, 0x1000, 2, PSX.getClut(0x10, row), 1, level, null);
                }

                renderDepth = 0x18;
                position = jointPoint(0x14, new int[]{programShort(0x8018EFFCL, 0), programShort(0x8018EFFCL, 1), programShort(0x8018EFFCL, 2)});
                if (currentAge < 35) {
                    int step = currentAge - 18;
                    byte[] sampled = new byte[4];
                    int[] color = {0, 0, 0};
                    if (step >= 0 && step < 17) {
                        int size = PSXMath.rcos(step << 6);
                        int width = 600 - step * 100;
                        sampleColorTrack(program.progData, (int) (0x80190AF4L - program.progBase), step * 2, sampled);
                        color = new int[]{sampled[0] & 0xFF, sampled[1] & 0xFF, sampled[2] & 0xFF};
                        drawRingBand(position, width, 700, 0x18, null, size, size, null, color, 0x40, 1);
                        drawRingBand(position, 700, 800, 0x18, null, size, size, color, null, 0x40, 1);
                    }

                    sampleColorTrack(program.progData, (int) (0x80190AF4L - program.progBase), currentAge, sampled);
                    color = new int[]{sampled[0] & 0xFF, sampled[1] & 0xFF, sampled[2] & 0xFF};
                    renderUnitSize = 0x40;
                    renderFrameStride = 4;
                    renderCellWidth = 0x40;
                    renderCellHeight = 0x40;
                    renderTpage = texturePageWord[u16LE(EffectAssets.drawTable, 0x16)];
                    renderPalette = 3;
                    renderCellLayout = 1;
                    renderShape = 0;
                    int size = PSXMath.rsin((currentAge << 10) / 34);
                    int[] spin = {0, 0, currentAge << 6, 0};
                    int row = u16LE(EffectAssets.drawTable, 0x20 + 2 * renderPalette);
                    drawEffectShape(position, spin, size, size, 0x40, PSX.getClut(0, row + 2), 1, 0x60, color);
                }

                return 0;
            }

            return 0;
        };

        effectCallbacks[RoomM005Program][RoomM005DriftingSprite] = (mode, sprite, argument) -> {
            byte[] a = arena[currentProcess];
            if (mode == 1) {
                put16LE(a, sprite, s16LE(a, sprite) + s16LE(a, sprite + 8));
                put16LE(a, sprite + 2, s16LE(a, sprite + 2) + s16LE(a, sprite + 0xA));
                put16LE(a, sprite + 4, s16LE(a, sprite + 4) + s16LE(a, sprite + 0xC));
                if (currentAge >= 6) return 1;
                return 0;
            }

            if (mode == 2) {
                EffectProgram program = ProcessManager.slotProgram[currentProcess];
                int at = (int) (0x8018F00CL - program.progBase);
                int[] color = {
                        u8(program.progData, at),
                        u8(program.progData, at + 1),
                        u8(program.progData, at + 2)
                };

                int[] beacon = {
                        programShort(0x80190BA8L, 0),
                        programShort(0x80190BA8L, 1),
                        programShort(0x80190BA8L, 2)
                };

                int[] drift = {
                        programShort(0x80190BA0L, 0),
                        programShort(0x80190BA0L, 1),
                        programShort(0x80190BA0L, 2)
                };

                int[] facing = {
                        toS16(s16LE(a, sprite) + drift[0]),
                        toS16(s16LE(a, sprite + 2) + drift[1]),
                        toS16(s16LE(a, sprite + 4) + drift[2]),
                };

                int u = (currentAge & 1) << 7;
                int v = ((currentAge << 3) & 0x10) + 0x20;
                int scale = PSXMath.rcos((currentAge << 10) / 6) / 24;
                int row = u16LE(EffectAssets.drawTable, 0x20 + 2 * renderPalette);
                if (renderPalette == 4 && effectImageVariant != 0) row += 4;
                drawSpriteQuad(beacon, facing, 0x44C, 0x6E, u, v, 0x7F, 0xF, PSX.getClut(0, row), color, color, scale, 1);
                return 0;
            }

            return 0;
        };

        effectCallbacks[RoomM005Program][RoomM005Beacon] = (mode, beacon, argument) -> {
            byte[] a = arena[currentProcess];
            int pool = taskEnd[currentProcess][currentTask];

            if (mode == 0) {
                int[] offset = {
                        programShort(0x8018F010L, 0),
                        programShort(0x8018F010L, 1),
                        programShort(0x8018F010L, 2)
                };

                int[] p = jointPoint(0x14, offset);
                put16LE(a, beacon, p[0]);
                put16LE(a, beacon + 2, p[1]);
                put16LE(a, beacon + 4, p[2]);
                int[] facing = jointFacing(0);
                put16LE(a, beacon + 0x10, facing[0]);
                put16LE(a, beacon + 0x12, facing[1]);
                put16LE(a, beacon + 0x14, facing[2]);
                put16LE(a, beacon + 0x16, 1);
                put32LE(a, beacon + 0x18, 0);
                put16LE(a, beacon + 0x20, 0);
                put16LE(a, beacon + 0x22, 0);

                if (held[currentProcess] != 0) {
                    ScriptVM owner = ProcessManager.slotOwner[currentProcess];
                    if (owner.getAttackEffectState() == 1) owner.setAttackEffectState(2);
                }

                roomM005BeamModel = loadModel(0xC5462704L);
                roomM005SwirlModel = loadModel(0xC5862704L);
                modelScrollUV(roomM005BeamModel, 0, 0);
                modelScrollUV(roomM005SwirlModel, 0, 0);
                return poolOpen(pool, 0x10, 8, RoomM005DriftingSprite);
            }

            if (mode == 1) {
                if (currentAge < 10) {
                    int sprite = poolAlloc(pool);

                    if (sprite != -1) {
                        put16LE(a, sprite, (PSX.rand() & 0x3FF) - 0x200);
                        put16LE(a, sprite + 2, (PSX.rand() & 0x3FF) - 0x200);
                        put16LE(a, sprite + 4, 0);
                        put16LE(a, sprite + 8, (PSX.rand() & 7) - 3);
                        put16LE(a, sprite + 0xA, (PSX.rand() & 7) - 3);
                        put16LE(a, sprite + 0xC, 0);
                    }
                }

                int phase = s16LE(a, beacon + 0x20);
                if (phase == 0) {
                    put16LE(a, beacon + 0x22, s16LE(a, beacon + 0x22) + 1);
                    put32LE(a, beacon + 0x1C, 0x1000);
                    put32LE(a, beacon + 0x18, s32LE(a, beacon + 0x18) + 0x200);
                    if (s16LE(a, beacon + 0x22) >= 5) {
                        put16LE(a, beacon + 0x20, 1);
                        put16LE(a, beacon + 0x22, 0);
                    }
                } else if (phase == 1) {
                    put16LE(a, beacon + 0x22, s16LE(a, beacon + 0x22) + 1);
                    put32LE(a, beacon + 0x1C, 0x1000 - (s16LE(a, beacon + 0x22) << 9));
                    if (s16LE(a, beacon + 0x22) >= 8) return 1;
                }

                int[] angles = {
                        s16LE(a, beacon + 0x10),
                        s16LE(a, beacon + 0x12),
                        s16LE(a, beacon + 0x14),
                };

                int[] ahead = forwardPoint(angles, s32LE(a, beacon + 0x18));
                put16LE(a, beacon + 0x14, angles[2]);

                for (int k = 0; k < 3; k++) {
                    put16LE(a, beacon + 8 + 2 * k, s16LE(a, beacon + 2 * k) + ahead[k]);
                }

                int[] here = {
                        s16LE(a, beacon),
                        s16LE(a, beacon + 2),
                        s16LE(a, beacon + 4),
                };

                int[] there = {
                        s16LE(a, beacon + 8),
                        s16LE(a, beacon + 0xA),
                        s16LE(a, beacon + 0xC),
                };

                if (!contact(here, there, 100)) return 0;
                if (held[currentProcess] != 0) {
                    ScriptVM owner = ProcessManager.slotOwner[currentProcess];
                    int flags = owner.getCoreFlags();
                    if ((flags & 0x3F000000) != 0x01000000) return 0;
                    ScriptVM.getPlayer().orEntityFlags(0x4000);
                    flags = (flags & 0xC0FFFFFF) | 0x11000000;
                    flags = flags | 0x80000000;
                    owner.setCoreFlags(flags);
                }

                return 0;
            }

            if (mode == 2) {
                int[] position = {
                        s16LE(a, beacon),
                        s16LE(a, beacon + 2),
                        s16LE(a, beacon + 4),
                };

                int[] target = {
                        s16LE(a, beacon + 8),
                        s16LE(a, beacon + 0xA),
                        s16LE(a, beacon + 0xC),
                };

                int[] rotation = {
                        s16LE(a, beacon + 0x10),
                        s16LE(a, beacon + 0x12),
                        s16LE(a, beacon + 0x14),
                };

                int height = s32LE(a, beacon + 0x18);
                int width = s32LE(a, beacon + 0x1C);
                renderDepth = 8;
                int[] matrix = Arrays.copyOf(ProcessManager.slotOwner[currentProcess].currentPose()[0], ProcessManager.slotOwner[currentProcess].currentPose()[0].length);
                PSXMath.scaleMatrix(matrix, width / 40, width / 40, (height << 12) / 8680);
                matrix[9] = position[0];
                matrix[10] = position[1];
                matrix[11] = position[2];
                renderTpage = texturePageWord[u16LE(EffectAssets.drawTable, 6)];
                renderPalette = 3;
                renderCellLayout = 0;
                int row = u16LE(EffectAssets.drawTable, 0x20 + 2 * renderPalette);
                modelSetTexture(renderTpage | PSX.getTPage(0, 1, 0, 0), PSX.getClut(0x30, row));
                modelSetSemiTrans(1);

                modelSaveColors(roomM005BeamModel);
                modelScaleColors(roomM005BeamModel, 0x32);
                modelDraw(roomM005BeamModel, matrix);
                modelRestoreColors(roomM005BeamModel);
                modelSaveColors(roomM005BeamModel);
                PSXMath.scaleMatrix(matrix, 0x2000, 0x2000, 0x1000);
                modelShiftColors(roomM005BeamModel, -0xFF, -0x82, 0x30);
                modelDraw(roomM005BeamModel, matrix);
                modelRestoreColors(roomM005BeamModel);
                modelSaveColors(roomM005BeamModel);
                PSXMath.scaleMatrix(matrix, 0x400, 0x400, 0x1000);
                modelScaleColors(roomM005BeamModel, 0xC8);
                modelDraw(roomM005BeamModel, matrix);
                modelRestoreColors(roomM005BeamModel);
                modelSaveColors(roomM005BeamModel);
                matrix[10] = ScriptVM.getRoomFloorY();
                PSXMath.scaleMatrix(matrix, 0x2000, 0x2000, 0x1000);
                modelScaleColors(roomM005BeamModel, 0x18);
                modelDraw(roomM005BeamModel, matrix);
                modelRestoreColors(roomM005BeamModel);

                renderUnitSize = 0x20;
                renderFrameStride = 2;
                renderCellWidth = 0x20;
                renderCellHeight = 0x20;
                renderTpage = texturePageWord[u16LE(EffectAssets.drawTable, 0x16)];
                renderPalette = 3;
                renderCellLayout = 1;
                renderShape = 0;
                renderDepth = 0x20;
                int alpha = 0xA0;
                int size = width;

                if ((currentAge & 1) != 0) {
                    alpha = 0x80;
                    size = size * 3 / 2;
                }

                row = u16LE(EffectAssets.drawTable, 0x20 + 2 * renderPalette);
                int glow = PSX.getClut(0, row + 4);
                drawEffectShape(position, null, size, size, 2, glow, 1, alpha, null);
                drawEffectShape(target, null, width / 2, width / 2, 2, glow, 1, alpha, null);
                int[] onFloor = {
                        position[0],
                        ScriptVM.getRoomFloorY(),
                        position[2]
                };

                int[] flat = {0x400, 0, 0, 1};
                drawEffectShape(onFloor, flat, size, size, 2, glow, 3, alpha, null);

                if (currentAge < 0xD) {
                    alpha = PSXMath.rcos((currentAge << 10) / 12) / 32;
                    size = PSXMath.rsin((currentAge << 10) / 12);
                    renderTpage = texturePageWord[u16LE(EffectAssets.drawTable, 0x16)];
                    renderPalette = 3;
                    renderCellLayout = 1;
                    row = u16LE(EffectAssets.drawTable, 0x20 + 2 * renderPalette);
                    modelSetTexture(renderTpage | PSX.getTPage(0, 1, 0, 0), PSX.getClut(0, row + 3));
                    modelSetSemiTrans(1);
                    int[] spin = PSXMath.rotMatrixYXZ(rotation[0] + 0x800, rotation[1], currentAge << 7);
                    int[] spinMatrix = new int[12];

                    for (int i = 0; i < 9; i++) spinMatrix[i] = spin[i];
                    for (int i = 0; i < 3; i++) spinMatrix[i + 9] = position[i];
                    PSXMath.scaleMatrix(spinMatrix, size / 2, size / 2, size / 2);
                    modelSaveColors(roomM005SwirlModel);
                    modelScaleColors(roomM005SwirlModel, alpha / 2);
                    modelDraw(roomM005SwirlModel, spinMatrix);
                    modelRestoreColors(roomM005SwirlModel);
                }

                renderUnitSize = 0x20;
                renderFrameStride = 2;
                renderCellWidth = 0x20;
                renderCellHeight = 0x20;
                renderTpage = texturePageWord[u16LE(EffectAssets.drawTable, 6)];
                renderPalette = 3;
                renderCellLayout = 0;
                renderShape = 0;
                renderDepth = 4;

                for (int k = 0; k < 3; k++) programPutShort(0x80190BA8L, k, position[k]);
                programPutShort(0x80190BA0L, 0, 0);
                programPutShort(0x80190BA0L, 1, rotation[1]);
                programPutShort(0x80190BA0L, 2, 0);

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

            if (ProcessManager.slotCommand[slot] == 117) {
                Logger.debug("M005 Effect Trigger Slot: " + slot + ", arg2: " + arg2);
                if (arg2 == 1) variables[slot][0] = 1;
                argument[slot] = 0x80190B80;
                return 0;
            }

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
        if (held[slot] != 0) effectImageVariant = findEffectImageVariant(ProcessManager.slotCommand[slot]);

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
            if (counter >= 2) owner.setCoreFlags((flags & 0xC0FFFFFF) | (((counter - 1) & 0x3F) << 24));

            if (((owner.getCoreFlags() >> 24) & 0x3F) > 0) {
                if (owner.getAttackEffectState() == 0) {
                    if (owner.readCombatField(44) <= 0) ProcessManager.slotState[slot] = 4;
                }
            }

            if (owner.getAttackEffectState() == 0) {
                if ((owner.getCoreFlags() & 0x180E) != 0) ProcessManager.slotState[slot] = 4;

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

    public static int ownerSoundTag() {
        int value = ProcessManager.slotOwner[currentProcess].getSoundTag();
        if (value >= 0x41) value = 0x80;
        return value;
    }

    public static int roomSound(int key, int tag, int pan, int volume) {
        byte[] container = ScriptVM.getRoomContainer();
        if (container == null) return -1;
        return PEAUDIO.playSfx(container, key, tag, pan, volume);
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

    public static int[] jointPoint(int joint, int[] input) {
        int[][] pose = ProcessManager.slotOwner[currentProcess].currentPose();
        int[] row = pose[joint];
        int[] r = PSXMath.mulVector(row, input[0], input[1], input[2]);
        return new int[]{toS16(r[0] + row[9]), toS16(r[1] + row[10]), toS16(r[2] + row[11])};
    }

    public static int[] ownerPoint(int mode) {
        ScriptVM owner = ProcessManager.slotOwner[currentProcess];
        if (mode == 0) {
            int[][] pose = owner.currentPose();
            return new int[]{toS16(pose[0][9]), toS16(pose[0][10]), toS16(pose[0][11])};
        }
        return new int[]{toS16(owner.getPosX() >> 16), toS16(owner.getPosY() >> 16), toS16(owner.getPosZ() >> 16)};
    }

    public static int[] lookAngles(int[] from, int[] to) {
        int dx = to[0] - from[0];
        int dz = to[2] - from[2];
        int hd = PSXMath.squareRoot0((long) dx * dx + (long) dz * dz);
        int y = (-PSXMath.ratan2(dz, dx) + 1024) & 4095;
        int x = (-PSXMath.ratan2(to[1] - from[1], hd)) & 4095;
        return new int[]{x, y, 0};
    }

    public static int[] jointFacing(int joint) {
        int[][] pose = ProcessManager.slotOwner[currentProcess].currentPose();
        int[] r = PSXMath.mulVector(pose[joint], 0, 0, -16384);
        return lookAngles(new int[]{0, 0, 0}, new int[]{toS16(r[0]), toS16(r[1]), toS16(r[2])});
    }

    public static int[] forwardPoint(int[] angles, int distance) {
        angles[2] = 0;
        int[] rot = PSXMath.rotMatrixYXZ(angles[0], angles[1], 0);
        int[] r = PSXMath.mulVector(rot, 0, 0, toS16(distance));
        return new int[]{toS16(r[0]), toS16(r[1]), toS16(r[2])};
    }

    public static boolean insideTriangle(int[] p, int[] t0, int[] t1, int[] t2) {
        int c0 = (t1[1] - t0[1]) * (p[0] - t0[0]) - (t1[0] - t0[0]) * (p[1] - t0[1]);
        int c1 = (t2[1] - t1[1]) * (p[0] - t1[0]) - (t2[0] - t1[0]) * (p[1] - t1[1]);
        int c2 = (t0[1] - t2[1]) * (p[0] - t2[0]) - (t0[0] - t2[0]) * (p[1] - t2[1]);
        if (((c0 ^ c1) | (c1 ^ c2)) > 0) return true;
        return false;
    }

    public static boolean contact(int[] a, int[] b, int radius) {
        int dx = b[0] - a[0];
        int dz = b[2] - a[2];
        int angle = -PSXMath.ratan2(dz, dx);
        int x = (PSXMath.rsin(angle) * radius) / 4096;
        int z = (PSXMath.rcos(angle) * radius) / 4096;
        int[] v0 = {toS16(a[0] - x), toS16(a[2] - z)};
        int[] v1 = {toS16(a[0] + x), toS16(a[2] + z)};
        int[] v2 = {toS16(b[0] - x), toS16(b[2] - z)};
        int[] v3 = {toS16(b[0] + x), toS16(b[2] + z)};
        ScriptVM player = ScriptVM.getPlayer();
        int[] p = {toS16(player.getPosX() >> 16), toS16(player.getPosZ() >> 16)};
        return insideTriangle(p, v0, v1, v2) | insideTriangle(p, v1, v2, v3);
    }

    public static void drawSpriteQuad(int[] position, int[] rotation, int length, int breadth, int u, int v, int textureWidth, int textureHeight, int clut, int[] color0, int color1[], int intensity, int mode) {
        int scale = toS16(intensity);
        int tpageWord = renderTpage;
        int semi = 0;

        if (mode != 0xFF) {
            tpageWord = renderTpage | getTPage(0, mode, 0, 0);
            semi = 0x01000000;
        }

        GTE.loadRotation(currentView.rotation());
        GTE.loadTranslation(currentView.translation());
        GTE.writeData(0, ((position[1] << 16) | (position[0] & 0xFFFF)));
        GTE.writeData(1, position[2]);
        GTE.execute(CMD_MVMVA);
        int[] translation = {GTE.readData(25), GTE.readData(26), GTE.readData(27)};
        int half = breadth / 2;
        int[][] corners = {{-half, 0, 0}, {half, 0, 0}, {-half, 0, length}, {half, 0, length}};
        int[] matrix = PSXMath.rotMatrixYXZ(rotation[0], rotation[1], rotation[2]);
        PSXMath.mulRotMatrix(matrix);
        GTE.loadRotation(matrix);
        GTE.loadTranslation(translation);
        for (int k = 0; k < 3; k++) {
            GTE.writeData(2 * k, ((corners[k][1] << 16) | (corners[k][0] & 0xFFFF)));
            GTE.writeData(2 * k + 1, corners[k][2]);
        }
        GTE.execute(0x4A280030);
        if (GTE.readData(24) == 0) return;

        int sxy0 = GTE.readData(12);
        int sxy1 = GTE.readData(13);
        int sxy2 = GTE.readData(14);
        GTE.execute(CMD_AVSZ3);
        int depth = GTE.readData(7) - renderDepth;
        if (depth < 0 || depth >= 0x1000) return;

        GTE.writeData(0, ((corners[3][1] << 16) | (corners[3][0] & 0xFFFF)));
        GTE.writeData(1, corners[3][2]);
        GTE.execute(0x4A180001);
        int sxy3 = GTE.readData(14);
        int[] top = {0, 0, 0};
        int[] bottom = {0, 0, 0};
        if (color0 != null) top = new int[]{color0[0] * scale / 128, color0[1] * scale / 128, color0[2] * scale / 128};
        if (color1 != null)
            bottom = new int[]{color1[0] * scale / 128, color1[1] * scale / 128, color1[2] * scale / 128};

        int rgbTop = packRGBColor(top[0], top[1], top[2]) | semi;
        int rgbBottom = packRGBColor(bottom[0], bottom[1], bottom[2]) | semi;
        pushVertex(sxy0, u, v, tpageWord, clut, depth, rgbTop);
        pushVertex(sxy1, u, v + textureHeight - 1, tpageWord, clut, depth, rgbTop);
        pushVertex(sxy2, u + textureWidth - 1, v, tpageWord, clut, depth, rgbBottom);
        pushVertex(sxy1, u, v + textureHeight - 1, tpageWord, clut, depth, rgbTop);
        pushVertex(sxy3, u + textureWidth - 1, v + textureHeight - 1, tpageWord, clut, depth, rgbBottom);
        pushVertex(sxy2, u + textureWidth - 1, v, tpageWord, clut, depth, rgbBottom);
    }

    public static void drawRingBand(int[] position, int inner, int outer, int segments, int[] rotation, int scaleX, int scaleY, int[] color0, int[] color1, int intensity, int mode) {
        if (segments < 4) return;
        int[] innerColor = {0, 0, 0};
        int[] outerColor = {0, 0, 0};

        if (color0 != null)
            innerColor = new int[]{color0[0] * intensity / 128, color0[1] * intensity / 128, color0[2] * intensity / 128};
        if (color1 != null)
            outerColor = new int[]{color1[0] * intensity / 128, color1[1] * intensity / 128, color1[2] * intensity / 128};

        int semi = 0, blend = 0;
        if (mode != 0xFF) {
            semi = 0x01000000;
            blend = mode & 3;
        }
        int rgbOuter = packRGBColor(outerColor[0], outerColor[1], outerColor[2]) | semi;
        int rgbInner = packRGBColor(innerColor[0], innerColor[1], innerColor[2]) | semi;

        if (rotation == null) rotation = new int[]{0, 0, 0, 0};
        GTE.loadRotation(currentView.rotation());
        GTE.loadTranslation(currentView.translation());
        GTE.writeData(0, ((position[1] << 16) | (position[0] & 0xFFFF)));
        GTE.writeData(1, position[2]);
        GTE.execute(CMD_MVMVA);
        int[] translation = {GTE.readData(25), GTE.readData(26), GTE.readData(27)};
        int[] matrix = PSXMath.rotMatrixYXZ(rotation[0], rotation[1], rotation[2]);
        if (rotation[3] != 0) PSXMath.mulRotMatrix(matrix);
        PSXMath.scaleMatrix(matrix, scaleX, scaleY, 4096);
        GTE.loadRotation(matrix);
        GTE.loadTranslation(translation);
        for (int i = 0; i < segments; i++) {
            int angle = (i << 12) / segments;
            int next = ((i + 1) << 12) / segments;
            int[] o0 = {toS16(PSXMath.rcos(angle) * outer / 4096), toS16(PSXMath.rsin(angle) * outer / 4096), 0};
            int[] o1 = {toS16(PSXMath.rcos(next) * outer / 4096), toS16(PSXMath.rsin(next) * outer / 4096), 0};
            int[] i0 = {toS16(PSXMath.rcos(angle) * inner / 4096), toS16(PSXMath.rsin(angle) * inner / 4096), 0};
            int[] i1 = {toS16(PSXMath.rcos(next) * inner / 4096), toS16(PSXMath.rsin(next) * inner / 4096), 0};

            int[][] corners = {o0, o1, i0};
            for (int k = 0; k < 3; k++) {
                GTE.writeData(2 * k, ((corners[k][1] << 16) | (corners[k][0] & 0xFFFF)));
                GTE.writeData(2 * k + 1, corners[k][2]);
            }
            GTE.execute(0x4A280030);
            int sxy0 = GTE.readData(12);
            int sxy1 = GTE.readData(13);
            int sxy2 = GTE.readData(14);
            GTE.execute(CMD_AVSZ3);
            int depth = GTE.readData(7) - renderDepth;
            GTE.writeData(0, ((i1[1] << 16) | (i1[0] & 0xFFFF)));
            GTE.writeData(1, i1[2]);
            GTE.execute(0x4A180001);
            int sxy3 = GTE.readData(14);
            if (depth < 0 || depth >= 0x1000) return;
            pushVertex(sxy0, 0, 0, -1, blend, depth, rgbOuter);
            pushVertex(sxy1, 0, 0, -1, blend, depth, rgbOuter);
            pushVertex(sxy2, 0, 0, -1, blend, depth, rgbInner);
            pushVertex(sxy1, 0, 0, -1, blend, depth, rgbOuter);
            pushVertex(sxy3, 0, 0, -1, blend, depth, rgbInner);
            pushVertex(sxy2, 0, 0, -1, blend, depth, rgbInner);
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

    public static void sampleColorTrack(byte[] track, int at, int time, byte[] out) {
        int count = s32LE(track, at + 4);
        int keyAt = at;
        if (s32LE(track, at) == 0) {
            count = 0;
            int total = 0;
            keyAt = at + 8;
            while (true) {
                int length = u8(track, keyAt + 3);
                if (length == 0) break;
                put16LE(track, keyAt + 6, total);
                total += length;
                count += 1;
                put16LE(track, keyAt + 4, length);
                keyAt += 8;
            }
            put32LE(track, at, total);
            put32LE(track, at + 4, count);
        }

        if (time > s32LE(track, at)) time = s32LE(track, at);
        keyAt = at + 8 + (count * 8 - 8);
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

    public static byte[] loadModel(long key) {
        byte[] container = ScriptVM.getRoomContainer();
        int at = findKeyedAssetTable(container, key);
        if (at == -1) return null;
        int size = u16LE(container, at + 8) + 4 * u16LE(container, at + 0xA);
        return Arrays.copyOfRange(container, at, at + size);
    }

    public static void modelSetTexture(int tpage, int clut) {
        modelTpage = tpage;
        modelClut = clut;
    }

    public static void modelSetSemiTrans(int value) {
        modelSemi = value;
    }

    public static void modelSetDepthBias(int value) {
        modelDepthBias = value;
    }

    public static void modelScrollUV(byte[] model, int xOffset, int yOffset) {
        if (u16LE(model, 0xC) != xOffset || u16LE(model, 0xE) != yOffset) {
            put16LE(model, 0xC, xOffset);
            put16LE(model, 0xE, yOffset);
            int triCount = u16LE(model, 0) + u16LE(model, 2);
            int quadCount = u16LE(model, 4) + u16LE(model, 6);
            int at = 0x10;

            for (int i = 0; i < triCount; i++) {
                for (int k = 0; k < 3; k++) {
                    model[at + 6 + 2 * k] = (byte) ((u8(model, at + 6 + 2 * k) + xOffset) & 0xFF);
                    model[at + 7 + 2 * k] = (byte) ((u8(model, at + 7 + 2 * k) + yOffset) & 0xFF);
                }
                at += 12;
            }

            for (int i = 0; i < quadCount; i++) {
                for (int k = 0; k < 4; k++) {
                    model[at + 8 + 2 * k] = (byte) ((u8(model, at + 8 + 2 * k) + xOffset) & 0xFF);
                    model[at + 9 + 2 * k] = (byte) ((u8(model, at + 9 + 2 * k) + yOffset) & 0xFF);
                }
                at += 16;
            }
        }

        modelDepthBias = 0;
    }

    public static void modelSaveColors(byte[] model) {
        int at = u16LE(model, 8);
        for (int i = 0; i < u16LE(model, 0xA); i++) {
            modelColorBackup[i] = (int) u32LE(model, at + 4 * i);
        }
    }

    public static void modelRestoreColors(byte[] model) {
        int at = u16LE(model, 8);
        for (int i = 0; i < u16LE(model, 0xA); i++) {
            put32LE(model, at + 4 * i, modelColorBackup[i]);
        }
    }

    public static void modelScaleColors(byte[] model, int factor) {
        int at = u16LE(model, 8);
        for (int i = 0; i < u16LE(model, 0xA); i++) {
            for (int k = 0; k < 3; k++) {
                int product = u8(model, at + 4 * i + k) * factor;
                if (product > 0x7FFF) product = 0x7FFF;
                model[at + 4 * i + k] = (byte) (product >> 7);
            }
        }
    }

    public static void modelSetColors(byte[] model, int r, int g, int b) {
        int at = u16LE(model, 8);
        for (int i = 0; i < u16LE(model, 0xA); i++) {
            model[at + 4 * i] = (byte) r;
            model[at + 4 * i + 1] = (byte) g;
            model[at + 4 * i + 2] = (byte) b;
        }
    }

    public static void modelShiftColors(byte[] model, int rDelta, int gDelta, int bDelta) {
        int[] deltas = {rDelta, gDelta, bDelta};
        int at = u16LE(model, 8);
        for (int i = 0; i < u16LE(model, 0xA); i++) {
            for (int k = 0; k < 3; k++) {
                int value = u8(model, at + 4 * i + k) + deltas[k];
                if (value >= 0x100) value = 0xFF;
                if (value < 0) value = 0;
                model[at + 4 * i + k] = (byte) value;
            }
        }
    }

    public static int[] composeMatrix(int[] view, int[] viewT, int[] placement) {
        int[] r = PSXMath.mulMatrix(view, Arrays.copyOfRange(placement, 0, 9));
        int[] t = PSXMath.mulVector(view, placement[9], placement[10], placement[11]);

        int[] out = new int[12];
        for (int i = 0; i < 9; i++) out[i] = r[i];
        for (int i = 0; i < 3; i++) out[i + 9] = t[i] + viewT[i];

        return out;
    }

    public static void modelDraw(byte[] model, int[] placement) {
        int[] matrix = composeMatrix(currentView.rotation(), currentView.translation(), placement);
        GTE.loadRotation(Arrays.copyOfRange(matrix, 0, 9));
        GTE.loadTranslation(Arrays.copyOfRange(matrix, matrix.length - 3, matrix.length));
        int flatTris = u16LE(model, 0);
        int shadedTris = u16LE(model, 2);
        int flatQuads = u16LE(model, 4);
        int shadedQuads = u16LE(model, 6);
        int at = 0x10;

        for (int i = 0; i < flatTris + shadedTris; i++) {
            modelFace(model, at, 3, i >= flatTris);
            at += 12;
        }

        for (int i = 0; i < flatQuads + shadedQuads; i++) {
            modelFace(model, at, 4, i >= flatQuads);
            at += 16;
        }

    }

    public static void modelFace(byte[] model, int at, int corners, boolean shaded) {
        int uvAt = at + 2 * corners;
        int[] vertexAt = {0, 0, 0, 0};
        for (int k = 0; k < corners; k++) {
            vertexAt[k] = u16LE(model, at + 2 * k);
        }

        for (int k = 0; k < 3; k++) {
            int v = vertexAt[k];
            GTE.writeData(2 * k, ((s16LE(model, v + 2) << 16) | (s16LE(model, v) & 0xFFFF)));
            GTE.writeData(2 * k + 1, s16LE(model, v + 4));
        }
        GTE.execute(0x4A280030);
        GTE.execute(CMD_NCLIP);
        if (GTE.readData(24) <= 0) return;
        int[] sxy = {GTE.readData(12), GTE.readData(13), GTE.readData(14), 0};
        GTE.execute(CMD_AVSZ3);
        int depth = GTE.readData(7) + modelDepthBias;
        if (depth <= 0) return;
        if (corners == 4) {
            int v = vertexAt[3];
            GTE.writeData(0, ((s16LE(model, v + 2) << 16) | (s16LE(model, v) & 0xFFFF)));
            GTE.writeData(1, s16LE(model, v + 4));
            GTE.execute(0x4A180001);
            sxy[3] = GTE.readData(14);
        }

        int flag = 0;
        if (modelSemi != 0) flag = 0x01000000;
        int[] rgb = {0, 0, 0, 0};
        int[] u = {0, 0, 0, 0};
        int[] w = {0, 0, 0, 0};

        for (int k = 0; k < corners; k++) {
            int source = vertexAt[k];
            if (!shaded) source = vertexAt[0];
            long word = u32LE(model, u16LE(model, source + 6));
            rgb[k] = (int) ((word & 0xFFFFFF) | flag);
            int uv = u16LE(model, uvAt + 2 * k);
            u[k] = uv & 0xFF;
            w[k] = (uv >> 8) & 0xFF;
        }

        int[] order = {0, 1, 2};
        if (corners == 4) order = new int[]{0, 1, 2, 1, 3, 2};
        for (int k : order)
            pushVertex(sxy[k], u[k], w[k], modelTpage, modelClut, depth, rgb[k]);
    }

    public static int programShort(long address, int index) {
        EffectProgram program = ProcessManager.slotProgram[currentProcess];
        return s16LE(program.progData, (int) (address - program.progBase + 2 * index));
    }

    public static void programPutShort(long address, int index, int value) {
        EffectProgram program = ProcessManager.slotProgram[currentProcess];
        put16LE(program.progData, (int) (address - program.progBase + 2 * index), value);
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
                    } else poolUpdate(taskEnd[slot][i]);
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
                else put16LE(arena[currentProcess], entry + 2, s16LE(arena[currentProcess], entry + 2) + 1);
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