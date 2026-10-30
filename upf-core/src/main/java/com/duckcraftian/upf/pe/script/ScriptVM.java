package com.duckcraftian.upf.pe.script;

import com.duckcraftian.upf.pe.ParasiteEve;
import com.duckcraftian.upf.pe.disc.RoomParser;
import com.duckcraftian.upf.pe.gfx.PEAnimation;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;
import static com.duckcraftian.upf.system.DataTypeHelper.u8;

public class ScriptVM {

    public static final int GLOBAL_COUNT = 512;
    public static final int SCRATCH_COUNT = 16;
    public static final int NODE_COUNT = 64;
    public static final int ACTOR_COUNT = 56;
    public static final int HANDLER_COUNT = 241;

    private int[] actorVars = new int[ACTOR_COUNT];

    // Entity Specific
    private int type;
    private int sub;
    private int posX, posY, posZ;
    private int rotX, rotY, rotZ;
    private int entityFlags;
    private int actionMode;
    private int animFrame;
    private int animStep;
    private int animStop;
    private int animLastFrame;
    private Map<Integer, PEAnimation> actions;

    private int streamStart;
    private int cursor;
    private int flags;
    private int active;

    private int[] mode = new int[16];
    private int[] raw = new int[16];

    private Handler[] handlers = new Handler[HANDLER_COUNT];

    private static final boolean[] WARNED = new boolean[HANDLER_COUNT];
    private static final int[] GLOBALS = new int[GLOBAL_COUNT];
    private static final int[] SCRATCH = new int[SCRATCH_COUNT];
    private static final int[] NODE_FLAGS = new int[NODE_COUNT];

    private static int[][] ANIM_RECORDS;

    private static byte[] CONTAINER;
    private static int[] STARTS;
    private static List<ScriptVM> actors = new ArrayList<>();
    private static int current;

    public static ScriptVM spawn(int type, int sub) {
        var vm = new ScriptVM();
        vm.type = type;
        vm.sub = sub;
        vm.start(STARTS[type]);
        vm.actions = new HashMap<>();

        for (int[] a : ANIM_RECORDS)
            if (a[0] == type)
                vm.actions.put(a[1], RoomParser.parseAnimation(CONTAINER, a[2]));

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

    public static void loadRoom(byte[] container) {
        CONTAINER = container;
        STARTS = RoomParser.readScriptStreams(container);
        actors.clear();
        for (int i = 0; i < SCRATCH.length; i++)
            SCRATCH[i] = 0;

        for (int i = 0; i < NODE_FLAGS.length; i++)
            NODE_FLAGS[i] = 0;

        current = -1;

        ANIM_RECORDS = RoomParser.readAnimationRecords(container);

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
            actors.get(i).runFrame();
        }
    }

    public void start(int streamStart) {
        this.streamStart = streamStart;
        cursor = streamStart;
        active = 1;
        flags = 0;

        handlers[ParasiteEve.JumpToEntityOffset] = vm -> {
            cursor = streamStart + get(0) * 2;
            return true;
        };

        handlers[ParasiteEve.JumpIfZero] = vm -> {
            if (get(0) == 0) cursor = streamStart + get(1) * 2;
            return true;
        };

        handlers[ParasiteEve.AssignValue] = vm -> {
            set(0, get(1));
            return true;
        };

        handlers[ParasiteEve.SetCurrentNodeActive] = vm -> {
            active = get(0);
            return false;
        };

        handlers[ParasiteEve.MarkCurrentNodeFree] = vm -> {
            flags = flags | 0x10;
            return false;
        };

        handlers[ParasiteEve.SpawnEntityAt] = vm -> {
            ScriptVM a = spawn(get(0), get(1));
            a.posX = get(2);
            a.posY = get(3);
            a.posZ = get(4);
            return true;
        };

        handlers[ParasiteEve.SetEntityVec3] = vm -> {
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

        handlers[ParasiteEve.EvalExpr] = vm -> {
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
                case 20 -> throw new IllegalArgumentException("Needs Fixed Multiplication");
                case 21 -> throw new IllegalArgumentException("Needs Fixed Division");
                case 22 -> a % b;
                case 23 -> -a;
                default -> throw new IllegalArgumentException("Illegal Operation");
            };

            set(1, result);
            return true;
        };

        handlers[ParasiteEve.SetCurrentActionMode] = vm -> {
            setActionMode(get(0));
            entityFlags &= ~0x100;
            return true;
        };

        handlers[ParasiteEve.SetEntityAnimFrame] = vm -> {
            animStop = Math.min(get(0), animLastFrame);
            entityFlags |= 0x200;
            return true;
        };

        handlers[ParasiteEve.SetEntityAnimSpeed] = vm -> {
            animFrame = Math.min(get(0), animLastFrame) << 16;
            return true;
        };

        handlers[ParasiteEve.WaitAnimFrameMatch] = vm -> {
            active = 1;
            if ((animFrame >> 16) != animStop)
                cursor = cursor - 8;

            return false;
        };

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
            Logger.debug("No Action " + mode + " for Type T");
            return;
        }

        actionMode = mode;
        animFrame = 0;
        entityFlags &= ~0x200;
        animLastFrame = anim.frameCount() - 1;
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

    public static byte[] getCONTAINER() {
        return CONTAINER;
    }

    public static int[] getSTARTS() {
        return STARTS;
    }

    public static List<ScriptVM> getActors() {
        return actors;
    }

    public static int getCurrent() {
        return current;
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