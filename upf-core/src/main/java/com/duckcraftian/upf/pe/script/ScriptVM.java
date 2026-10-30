package com.duckcraftian.upf.pe.script;

import com.duckcraftian.upf.pe.disc.RoomParser;
import com.duckcraftian.upf.pe.gfx.PEAnimation;
import com.duckcraftian.upf.ps1.PSXMath;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.duckcraftian.upf.pe.ParasiteEve.*;
import static com.duckcraftian.upf.pe.ParasiteEve.SetSceneFlag2000;
import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;
import static com.duckcraftian.upf.system.DataTypeHelper.u8;

public class ScriptVM {

    public static final int GLOBAL_COUNT = 512;
    public static final int SCRATCH_COUNT = 16;
    public static final int NODE_COUNT = 64;
    public static final int ACTOR_COUNT = 56;
    public static final int HANDLER_COUNT = 241;

    public static int pad;

    // Entity Specific
    private int type;
    private int sub;

    private int posX, posY, posZ;
    private int rotX, rotY, rotZ;
    private int motionX, motionY, motionZ;
    private int accelX, accelY, accelZ;
    private int deltaX, deltaY, deltaZ;

    private int moveFactor;
    private int moveSpeed = 0x1000;
    private int entityFlags;

    private int entityMode;
    private int actionMode;

    private int animFrame;
    private int animStep = 0x10000;
    private int animStop;
    private int animLastFrame;

    private int streamStart;
    private int cursor;
    private int flags;
    private int active;

    private Map<Integer, PEAnimation> actions;

    private int[] mode = new int[16];
    private int[] raw = new int[16];

    private int[] actorVars = new int[ACTOR_COUNT];
    private Handler[] handlers = new Handler[HANDLER_COUNT];

    private static int moveLock;
    private static int sceneFlags;
    private static int current;

    public static int cameraYaw, cameraYaw2;

    private static final boolean[] WARNED = new boolean[HANDLER_COUNT];

    private static final int[] GLOBALS = new int[GLOBAL_COUNT];
    private static final int[] SCRATCH = new int[SCRATCH_COUNT];
    private static final int[] NODE_FLAGS = new int[NODE_COUNT];

    private static int[][] ANIM_RECORDS;
    private static int[][] BANK_ANIM_RECORDS;

    private static byte[] BANK_CONTAINER;
    private static byte[] CONTAINER;

    private static int[] STARTS;

    private static List<ScriptVM> actors = new ArrayList<>();

    public static ScriptVM spawn(int type, int sub) {
        var vm = new ScriptVM();
        vm.type = type;
        vm.moveFactor = (type == 0) ? 0x10000 : 0x50000;

        vm.sub = sub;
        vm.start(STARTS[type]);
        vm.actions = new HashMap<>();

        for (int[] a : ANIM_RECORDS)
            if (a[0] == type)
                vm.actions.put(a[1], RoomParser.parseAnimation(CONTAINER, a[2]));

        if (type == 0 && BANK_CONTAINER != null) {
            vm.entityMode = 21;

            for (int[] a : BANK_ANIM_RECORDS)
                if (a[0] == 0) vm.actions.put(a[1], RoomParser.parseAnimation(BANK_CONTAINER, a[2]));
        }

        if (!vm.actions.isEmpty()) {
            int start = (type == 0) ? 21 : 0;
            vm.setActionMode(start);
        }

        actors.add(current + 1, vm);
        return vm;
    }

    public static ScriptVM getPlayer() {
        for (ScriptVM vm : actors)
            if (vm.type == 0) return vm;

        return null;
    }

    public static void loadRoom(byte[] container, byte[] bankContainer) {
        CONTAINER = container;
        BANK_CONTAINER = bankContainer;
        STARTS = RoomParser.readScriptStreams(container);
        actors.clear();
        for (int i = 0; i < SCRATCH.length; i++)
            SCRATCH[i] = 0;

        for (int i = 0; i < NODE_FLAGS.length; i++)
            NODE_FLAGS[i] = 0;

        current = -1;

        ANIM_RECORDS = RoomParser.readAnimationRecords(container);

        if (bankContainer != null)
            BANK_ANIM_RECORDS = RoomParser.readAnimationRecords(bankContainer);

        int spawnList = RoomParser.readSpawnList(container);
        int n = u8(container, spawnList);
        for (int i = 0; i < n; i++) {
            int type = u8(container, spawnList + 1 + 2 * i);
            int sub = u8(container, spawnList + 2 + 2 * i);
            spawn(type, sub);
            current = actors.size() - 1;
        }
    }

    public static void runAll() {
        for (int i = 0; i < actors.size(); i++) {
            current = i;
            ScriptVM vm = actors.get(i);
            vm.runFrame();
            if ((vm.type == 0) && (moveLock & 1) == 0) vm.controller();
            vm.integrate();
        }
    }

    public static List<ScriptVM> getActors() {
        return actors;
    }

    public void start(int streamStart) {
        this.streamStart = streamStart;
        cursor = streamStart;
        active = 1;
        flags = 0;

        handlers[JumpToEntityOffset] = vm -> {
            cursor = streamStart + get(0) * 2;
            return true;
        };

        handlers[JumpIfZero] = vm -> {
            if (get(0) == 0) cursor = streamStart + get(1) * 2;
            return true;
        };

        handlers[AssignValue] = vm -> {
            set(0, get(1));
            return true;
        };

        handlers[SetCurrentNodeActive] = vm -> {
            active = get(0);
            return false;
        };

        handlers[MarkCurrentNodeFree] = vm -> {
            flags = flags | 0x10;
            return false;
        };

        handlers[SpawnEntityAt] = vm -> {
            ScriptVM a = spawn(get(0), get(1));
            a.posX = get(2);
            a.posY = get(3);
            a.posZ = get(4);
            return true;
        };

        handlers[SetGlobalFlag1] = vm -> {
            moveLock |= 1;
            return true;
        };

        handlers[ClearGlobalFlag1] = vm -> {
            moveLock &= ~1;
            return true;
        };

        handlers[GetGlobalFlags] = vm -> {
            set(0, moveLock);
            return true;
        };

        handlers[SetGlobalFlags] = vm -> {
            moveLock |= get(0);
            return true;
        };

        handlers[ClearGlobalFlags] = vm -> {
            moveLock &= ~get(0);
            return true;
        };

        handlers[SetSceneFlag2000] = vm -> {
            sceneFlags |= 0x2000;
            return true;
        };

        handlers[ClearSceneFlag2000] = vm -> {
            sceneFlags &= ~0x2000;
            return true;
        };

        handlers[SetCurrentFlag80] = vm -> {
            entityFlags |= 0x80;
            return true;
        };

        handlers[setCurrentFlag20] = vm -> {
            entityFlags |= 0x20;
            return true;
        };

        handlers[ClearCurrentFlag20] = vm -> {
            entityFlags &= ~0x20;
            return true;
        };

        handlers[ClearMotionVectors] = vm -> {
            motionX = 0;
            accelX = 0;
            motionY = 0;
            accelY = 0;
            motionZ = 0;
            accelZ = 0;
            return true;
        };

        handlers[SetEntityMoveSpeed] = vm -> {
            moveSpeed = get(0) >> 4;
            return true;
        };

        handlers[ScreenScrollPos] = vm -> {
            cameraYaw = get(0);
            cameraYaw2 = get(1);
            return true;
        };

        handlers[SetEntityVec3] = vm -> {
            switch (get(0)) {
                case 0:
                    posX = get(1);
                    posY = get(2);
                    posZ = get(3);
                    Logger.debug("type " + type + " pos " + (posX >> 16) + ", " + (posZ >> 16));
                case 5:
                    rotX = get(1);
                    rotY = get(2);
                    rotZ = get(3);
            }

            return true;
        };

        handlers[EvalExpr] = vm -> {
            int subOp = get(0);
            int a = get(2);
            int b = get(3);
            int result = switch (subOp) {
                case 0 -> a + b;
                case 1 -> a - b;
                case 2 -> a | b;
                case 3 -> a & b;
                case 4 -> a ^ b;
                case 5 -> (a != 0 || b != 0) ? 1 : 0;
                case 6 -> (a != 0 && b != 0) ? 1 : 0;
                case 7 -> (a == 0) ? 1 : 0;
                case 8 -> ~a;
                case 9 -> a > b ? 1 : 0;
                case 10 -> a < b ? 1 : 0;
                case 11 -> a == b ? 1 : 0;
                case 12 -> a >= b ? 1 : 0;
                case 13 -> a <= b ? 1 : 0;
                case 14 -> a != b ? 1 : 0;
                case 15 -> a * b;
                case 16 -> a / b;
                case 17 -> a << b;
                case 18 -> a >> b;
                case 19 -> a;
                case 20 -> PSXMath.fixedMul(a, b);
                case 21 -> PSXMath.fixedDiv(a, b);
                case 22 -> a % b;
                case 23 -> -a;
                default -> throw new IllegalArgumentException("Illegal Operation");
            };

            set(1, result);
            return true;
        };

        handlers[SetCurrentActionMode] = vm -> {
            setActionMode(get(0));
            entityFlags &= ~0x100;
            return true;
        };

        handlers[SetEntityAnimFrame] = vm -> {
            animStop = Math.min(get(0), animLastFrame);
            entityFlags |= 0x200;
            return true;
        };

        handlers[SetEntityAnimSpeed] = vm -> {
            animFrame = Math.min(get(0), animLastFrame) << 16;
            return true;
        };

        handlers[WaitAnimFrameMatch] = vm -> {
            active = 1;
            if ((animFrame >> 16) != animStop)
                cursor = cursor - 8;

            return false;
        };

    }

    public void integrate() {
        motionX += accelX;
        motionY += accelY;
        motionZ += accelZ;
        posX += motionX;
        posY += motionY;
        posZ += motionZ;
        posX += deltaX;
        posY += deltaY;
        posZ += deltaZ;
    }

    public int get(int i) {
        return switch (mode[i]) {
            case 1 -> actorVars[raw[i]];
            case 2 -> GLOBALS[raw[i]];
            case 3 -> SCRATCH[raw[i]];
            case 4 -> NODE_FLAGS[raw[i]];
            default -> raw[i];
        };
    }

    public void set(int i, int value) {
        switch (mode[i]) {
            case 1:
                actorVars[raw[i]] = value;
                break;
            case 2:
                GLOBALS[raw[i]] = value;
                break;
            case 3:
                SCRATCH[raw[i]] = value;
                break;
            case 4:
                NODE_FLAGS[raw[i]] = value;
                break;
            default:
                raw[i] = value;
                break;
        }
    }

    public void setActionMode(int mode) {
        PEAnimation anim = actions.get(mode);

        if (anim == null) {
            Logger.debug("No Action " + mode + " for type " + type);
            return;
        }

        actionMode = mode;
        animFrame = 0;
        entityFlags &= ~0x200;
        animLastFrame = anim.frameCount() - 1;
    }

    public void controller() {
        int dir = pad & 0x78;
        boolean run = (pad & 0x01) != 0;

        if (entityMode != 21 && entityMode != 22 && entityMode != 23)
            return;

        if (run && dir != 0)
            move(0x168000, 23);
        else if (dir != 0)
            move(0x50000, 22);
        else if (entityMode != 21) idle();
    }

    private void idle() {
        motionX = 0;
        motionY = 0;
        motionZ = 0;
        setActionMode(21);
        entityMode = 21;
    }

    private void move(int base, int newMode) {
        if (entityMode != newMode) {
            setActionMode(newMode);
            entityMode = newMode;
        }

        int s = PSXMath.fixedMul(base, moveFactor);
        s = PSXMath.fixedMul(s, moveSpeed << 4);

        int diag = PSXMath.fixedMul(s, 0xB504);

        int facing = cameraYaw;

        int mx = 0, mz = 0;


        if ((pad & 0x08) != 0) {
            if ((pad & 0x40) != 0) {
                facing += 0x600;
                mx = -diag;
                mz = diag;
            } else if ((pad & 0x10) != 0) {
                facing += 0xA00;
                mx = diag;
                mz = diag;
            } else {
                facing += 0x800;
                mx = 0;
                mz = s;
            }
        } else if ((pad & 0x20) != 0) {
            if ((pad & 0x40) != 0) {
                facing += 0x200;
                mx = -diag;
                mz = -diag;
            } else if ((pad & 0x10) != 0) {
                facing += 0xE00;
                mx = diag;
                mz = -diag;
            } else {
                mx = 0;
                mz = -s;
            }
        } else if ((pad & 0x40) != 0) {
            facing += 0x400;
            mx = -s;
            mz = 0;
        } else if ((pad & 0x10) != 0) {
            facing += 0xC00;
            mx = s;
            mz = 0;
        }

        rotY = facing & 0xFFF;

        int c = PSXMath.rcos(cameraYaw & 0xFFF);
        int sn = PSXMath.rsin(cameraYaw & 0xFFF);
        motionX = (int) (((long) c * mx + (long) sn * mz) >> 12);
        motionY = 0;
        motionZ = (int) (((long) -sn * mx + (long) c * mz) >> 12);
    }

    public void advanceAnim() {
        entityFlags &= ~8;

        if ((entityFlags & 0x100) != 0) return;
        if ((entityFlags & 0x200) != 0 && (animFrame >> 16) == animStop) return;
        int next = animFrame + animStep;

        if ((entityFlags & 0x200) != 0 && (next >> 16) > animStop)
            next = animStop << 16;

        if ((next >> 16) > animLastFrame) {
            next = ((next >> 16) % (animLastFrame + 1)) << 16;
            entityFlags |= 8;
        }

        animFrame = next;
    }

    public void runFrame() {
        if ((flags & 0x50) != 0) return;
        if (active == 0) return;

        active = active - 1;
        if (active != 0) return;

        while (true) {
            int header = (int) u32LE(CONTAINER, cursor);
            int tail = (int) u32LE(CONTAINER, cursor + 4);
            int op = header & 0x1FFF;
            int argc = (header >> 13) & 15;

            if (op > 240 || !Decoder.valid[op]) {
                Logger.error("Non Valid OpCode: " + op);
                flags |= 0x10;
                return;
            }

            int modes = header >> 17;
            for (int i = 0; i < argc; i++) {
                if (i == 5) modes = tail;
                mode[i] = modes & 7;
                raw[i] = (int) u32LE(CONTAINER, cursor + 8 + 4 * i);
                modes = modes >> 3;
            }

            cursor = cursor + 8 + 4 * argc;

            Handler h = handlers[op];
            if (h == null) {
                if (!WARNED[op]) {
                    WARNED[op] = true;
                    Logger.error("Op: " + op + " Unimplemented");
                }

                continue;
            }

            if (!h.run(this)) break;
        }
    }

    public PEAnimation getAnimation() {
        return actions.get(actionMode);
    }

    public int getFrame() {
        return animFrame >> 16;
    }

    public int getType() {
        return type;
    }

    public int getSub() {
        return sub;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public int getPosZ() {
        return posZ;
    }

    public int getRotX() {
        return rotX;
    }

    public int getRotY() {
        return rotY;
    }

    public int getRotZ() {
        return rotZ;
    }

    public int[] getGLOBALS() {
        return GLOBALS;
    }

    public int[] getScratch() {
        return SCRATCH;
    }

    public int[] getNodeFlags() {
        return NODE_FLAGS;
    }

    public int[] getActorVars() {
        return actorVars;
    }

    public byte[] getContainer() {
        return CONTAINER;
    }

    public int getStreamStart() {
        return streamStart;
    }

    public int getCursor() {
        return cursor;
    }

    public int getFlags() {
        return flags;
    }

    public int getActive() {
        return active;
    }

    public int[] getMode() {
        return mode;
    }

    public int[] getRaw() {
        return raw;
    }

    public Handler[] getHandlers() {
        return handlers;
    }

    public boolean[] getWarned() {
        return WARNED;
    }
}