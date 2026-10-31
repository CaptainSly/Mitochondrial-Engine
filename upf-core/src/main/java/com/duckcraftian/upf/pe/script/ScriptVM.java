package com.duckcraftian.upf.pe.script;

import com.duckcraftian.upf.pe.disc.RoomParser;
import com.duckcraftian.upf.pe.gfx.PEAnimation;
import com.duckcraftian.upf.pe.gfx.PEModel;
import com.duckcraftian.upf.pe.gfx.PEText;
import com.duckcraftian.upf.pe.rooms.Collision;
import com.duckcraftian.upf.ps1.PSX;
import com.duckcraftian.upf.ps1.PSXMath;
import com.duckcraftian.upf.system.Utils;
import org.tinylog.Logger;

import java.util.*;

import static com.duckcraftian.upf.pe.ParasiteEve.*;
import static com.duckcraftian.upf.ps1.PSXMath.*;
import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;
import static com.duckcraftian.upf.system.DataTypeHelper.u8;

public class ScriptVM {

    public static final int GLOBAL_COUNT = 512;
    public static final int SCRATCH_COUNT = 16;
    public static final int NODE_COUNT = 64;
    public static final int ACTOR_COUNT = 56;
    public static final int HANDLER_COUNT = 241;

    public static Collision COLLISION;

    public static int pad;
    public static int cameraYaw, cameraYaw2;

    public static int fadeLevel = 0;
    public static int fadeStart = 0;
    public static int fadeTarget = 0;
    public static int fadeFrame = 0;
    public static int fadeDuration = 0;

    public static int messageBank;

    public static int[] playerName = {0x10, 0x48, 0x30};
    public static Map<Integer, PEModel> models;
    public static Map<Integer, List<int[]>> messageSections = new HashMap<>();
    public static Map<Integer, Integer> messageSection = new HashMap<>();
    public static List<Integer> openMessages = new ArrayList<>();

    // Entity Specific
    private int type;
    private int sub;

    private int baseX, baseY, baseZ;
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

    private Node node;
    private int msgCursor;
    private int contactCursor;

    private List<Node> nodes;
    private Map<Integer, PEAnimation> actions;

    private int[] mode = new int[16];
    private int[] raw = new int[16];

    private int[] actorVars = new int[ACTOR_COUNT];
    private Handler[] handlers = new Handler[HANDLER_COUNT];

    private static int moveLock;
    private static int sceneFlags;
    private static int current;
    private static int pendingRoom = 0;

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
    private static List<int[]> queue = new ArrayList<>();

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

    public static int takePendingRoom() {
        int t = pendingRoom;
        pendingRoom = 0;
        return t;
    }

    public static void loadRoom(byte[] container, byte[] bankContainer) {
        CONTAINER = container;
        BANK_CONTAINER = bankContainer;
        STARTS = RoomParser.readScriptStreams(container);
        actors.clear();

        Arrays.fill(SCRATCH, 0);
        Arrays.fill(NODE_FLAGS, 0);

        current = -1;
        moveLock = 0;

        ANIM_RECORDS = RoomParser.readAnimationRecords(container);

        messageBank = RoomParser.readMessageBank(container);

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

        COLLISION = RoomParser.readCollision(container);
    }

    public static void runAll() {
        deliverMessages();

        for (int i = 0; i < actors.size(); i++) {
            current = i;
            ScriptVM vm = actors.get(i);

            vm.baseX = vm.posX;
            vm.baseY = vm.posY;
            vm.baseZ = vm.posZ;
            vm.runFrame();

            if ((vm.type == 0) && (moveLock & 1) == 0) vm.controller();
            vm.integrate();
        }

        for (ScriptVM actor : actors)
            actor.updatePosition();

    }

    public static List<ScriptVM> getActors() {
        return actors;
    }

    public static void deliverMessages() {
        for (int[] e : queue) {
            for (ScriptVM vm : actors) {
                if (vm.type == e[2] && vm.sub == e[1] && vm.msgCursor != 0) {
                    Node n = new Node();
                    n.cursor = vm.msgCursor;
                    n.active = 1;
                    n.target14 = e[0];
                    vm.nodes.addFirst(n);
                    Logger.debug("Command: " + e[0] + " Sub: " + e[1] + " Type: " + e[2]);
                }
            }
        }

        queue.clear();
    }

    public void updatePosition() {
        if (COLLISION == null) return;
        if ((posX >> 16) == (baseX >> 16) && (posZ >> 16) == (baseZ >> 16) && posY == baseY) return;

        int t = COLLISION.findTriangle(posX >> 16, posZ >> 16);
        if (t < 0) {
            posX = baseX;
            posY = baseY;
            posZ = baseZ;
            return;
        }

        posY = COLLISION.floorY(t, posX, posZ);
    }

    public void start(int streamStart) {
        this.streamStart = streamStart;

        Node main = new Node();
        main.cursor = streamStart;
        main.active = 1;
        nodes = new ArrayList<>();
        nodes.add(main);

        handlers[JumpToEntityOffset] = _ -> {
            node.cursor = streamStart + get(0) * 2;
            return true;
        };

        handlers[JumpIfZero] = _ -> {
            if (get(0) == 0) node.cursor = streamStart + get(1) * 2;
            return true;
        };

        handlers[AssignValue] = _ -> {
            set(0, get(1));
            return true;
        };

        handlers[SpawnChildNode] = _ -> {
            Node child = new Node();
            child.cursor = streamStart + get(0) * 2;
            child.active = 1;
            child.flags = 0;
            nodes.add(nodes.indexOf(node) + 1, child);
            return true;
        };

        handlers[OpenNameEntryMenu] = _ -> {
            // EVEN HANK WAS SCARED!
            playerName = new int[]{PSX.code("H"), PSX.code("a"), PSX.code("n"), PSX.code("k")};
            // Hank: I don't wanna lose my dong.
            return true;
        };

        handlers[RoomChange] = _ -> {
            pendingRoom = get(0);
            return false;
        };

        handlers[SelectFieldSetter] = _ -> {
            int pos = (get(1) < 0) ? 0 : streamStart + get(1) * 2;
            if (get(0) == 2) msgCursor = pos;
            else if (get(0) == 1) contactCursor = pos;
            return true;
        };

        handlers[GetNodeTarget14] = _ -> {
            set(0, node.target14);
            return true;
        };

        handlers[MarkNodeFree] = _ -> {
            for (Node n : nodes)
                if (n != node) n.flags |= 0x10;

            return true;
        };

        handlers[WaitForFade] = _ -> {
            if (fadeFrame < fadeDuration) {
                node.active = 1;
                node.cursor = node.cursor - 8;
                return false;
            }
            return true;
        };

        handlers[FadeSceneToBlack] = _ -> {
            startFade(255, get(0));
            return true;
        };

        handlers[FadeSceneIn] = _ -> {
            startFade(0, get(0));
            return true;
        };

        handlers[ActorSnapToAnimAnchor] = _ -> {
            PEModel model = models.get(type);
            if (model == null || getAnimation() == null)
                return true;

            int[] rot = PSXMath.rotMatrixYXZ(0, rotY, 0);
            int[] at = {posX >> 16, posY >> 16, posZ >> 16};
            int[][] pose = model.pose(getAnimation(), getFrame(), rot, at);

            int[] bone = pose[model.getAnchorMatrixIndex()];
            int[] boneMatrix = Arrays.copyOfRange(bone, 0, 9);
            int[] p = PSXMath.mulVector(boneMatrix, 0, model.getAnchorY(), 0);

            posX = bone[9] << 16;
            posY = bone[10] << 16;
            posZ = bone[11] << 16;

            Logger.debug("P: " + Utils.arrayToString(p) + " bone3x3: " + Utils.arrayToString(boneMatrix));
            Logger.debug("205 type=" + type
                    + " rotY=" + rotY
                    + " frame=" + getFrame()
                    + " rootOff=" + getAnimation().channels()[0][getFrame()] + ","
                    + getAnimation().channels()[1][getFrame()] + ","
                    + getAnimation().channels()[2][getFrame()]
                    + " rootRot=" + getAnimation().channels()[3][getFrame()] + ","
                    + getAnimation().channels()[4][getFrame()] + ","
                    + getAnimation().channels()[5][getFrame()]);


            return true;
        };

        handlers[PlayFieldSFX] = _ -> {
            queue.add(new int[]{
                    get(2) & 0xFF, get(1) & 0xFF, get(0) & 0xFFFF
            });

            return true;
        };

        handlers[TextboxOpen] = _ -> {
            int id = get(0);
            openMessages.add(id);
            messageSections.put(id, PEText.layoutMessage(CONTAINER, messageBank, id));
            messageSection.put(id, 0);
            return true;
        };

        handlers[TextboxWait] = _ -> {
            if (openMessages.contains(get(0))) {
                node.active = 1;
                node.cursor = node.cursor - 12;
                return false;
            }

            return true;
        };

        handlers[TextboxClose] = _ -> {
            closeMessage(get(0));
            return true;
        };

        handlers[GetPlayerPosition] = _ -> {
            int selector = get(1);
            ScriptVM target = (selector == 0) ? getPlayer() : getActor(selector, get(2));
            if (target == null) {
                set(6, -1);
                return true;
            }

            set(6, 1);

            if (get(0) == 0) {
                set(3, target.posX);
                set(4, target.posY);
                set(5, target.posZ);
            }

            return true;
        };

        handlers[IsInRectangle] = _ -> {
            int[] px = new int[4], pz = new int[4];
            for (int k = 0; k < 4; k++) {
                px[k] = get(2 * k) >> 16;
                pz[k] = get(2 * k + 1) >> 16;
            }
            set(10, Collision.pointInPoly(get(8) >> 16, get(9) >> 16, px, pz) ? 1 : 0);
            return true;
        };

        handlers[FadeSceneToColor] = _ -> {
            int mode = get(4);
            int level = get(1);

            if (mode == 2) startFade(level, get(0));
            else if (mode == 1) setFade(level);
            else setFade(0);

            return true;
        };

        handlers[SetCurrentNodeActive] = _ -> {
            node.active = get(0);
            return false;
        };

        handlers[MarkCurrentNodeFree] = _ -> {
            node.flags = node.flags | 0x10;
            return false;
        };

        handlers[SetCurrentFlag40] = _ -> {
            entityFlags |= 0x40;
            return true;
        };

        handlers[SpawnEntityAt] = _ -> {
            ScriptVM a = spawn(get(0), get(1));
            a.posX = get(2);
            a.posY = get(3);
            a.posZ = get(4);
            findFloor();
            return true;
        };

        handlers[SetGlobalFlag1] = _ -> {
            moveLock |= 1;
            return true;
        };

        handlers[ClearGlobalFlag1] = _ -> {
            moveLock &= ~1;
            return true;
        };

        handlers[GetGlobalFlags] = _ -> {
            set(0, moveLock);
            return true;
        };

        handlers[SetGlobalFlags] = _ -> {
            moveLock |= get(0);
            return true;
        };

        handlers[ClearGlobalFlags] = _ -> {
            moveLock &= ~get(0);
            return true;
        };

        handlers[SetSceneFlag2000] = _ -> {
            sceneFlags |= 0x2000;
            return true;
        };

        handlers[ClearSceneFlag2000] = _ -> {
            sceneFlags &= ~0x2000;
            return true;
        };

        handlers[SetCurrentFlag80] = _ -> {
            entityFlags |= 0x80;
            return true;
        };

        handlers[setCurrentFlag20] = _ -> {
            entityFlags |= 0x20;
            return true;
        };

        handlers[ClearCurrentFlag20] = _ -> {
            entityFlags &= ~0x20;
            return true;
        };

        handlers[ClearMotionVectors] = _ -> {
            motionX = 0;
            accelX = 0;
            motionY = 0;
            accelY = 0;
            motionZ = 0;
            accelZ = 0;
            return true;
        };

        handlers[SetEntityMoveSpeed] = _ -> {
            moveSpeed = get(0) >> 4;
            return true;
        };

        handlers[ScreenScrollPos] = _ -> {
            cameraYaw = get(0);
            cameraYaw2 = get(1);
            return true;
        };

        handlers[SetEntityVec3] = _ -> {
            switch (get(0)) {
                case 0:
                    posX = get(1);
                    posY = get(2);
                    posZ = get(3);
                    findFloor();
                    baseX = posX;
                    baseY = posY;
                    baseZ = posZ;
                    break;
                case 5:
                    rotX = get(1);
                    rotY = get(2);
                    rotZ = get(3);
                    break;
            }

            return true;
        };

        handlers[EvalExpr] = _ -> {
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
                case 20 -> fixedMul(a, b);
                case 21 -> PSXMath.fixedDiv(a, b);
                case 22 -> a % b;
                case 23 -> -a;
                default -> throw new IllegalArgumentException("Illegal Operation");
            };

            set(1, result);
            return true;
        };

        handlers[SetCurrentActionMode] = _ -> {
            setActionMode(get(0));
            entityFlags &= ~0x100;
            return true;
        };

        handlers[ActorTurnToPoint] = _ -> {
            if ((node.flags & 0x20) == 0) {
                node.target14 = get(0);
                node.targetZ = get(1);
                node.turnSpeed = get(2);
                node.flags = node.flags | 0x20;
            }

            if (((posX - node.target14) >> 16) == 0 && ((posZ - node.targetZ) >> 16) == 0) {
                node.flags = node.flags & ~0x20;
                return true;
            }

            int goal = angleToPoint(node.target14, node.targetZ);
            rotY = stepAngle(rotY, goal, node.turnSpeed);

            if (rotY != goal) {
                node.active = 1;
                node.cursor = node.cursor - 0x14;
                return false;
            }

            node.flags = node.flags & ~0x20;
            return true;
        };

        handlers[ActorWalkToPoint] = _ -> {
            if ((node.flags & 0x20) == 0) {
                if (posX == get(0) && posZ == get(1))
                    return true;

                node.target14 = get(0);
                node.targetZ = get(1);
                node.turnSpeed = get(2);
                node.flags = node.flags | 0x20;
            }

            int base = (type == 0) ? 0x50000 : 0x10000;
            int speed = fixedMul(fixedMul(base, moveFactor), moveSpeed << 4);

            int angle = angleToPoint(node.target14, node.targetZ);
            if (node.turnSpeed != 0) {
                angle = stepAngle(rotY, angle, node.turnSpeed);
                rotY = angle;
            }

            motionX = fixedMul(-speed, rsin(angle) << 4);
            motionZ = fixedMul(-speed, rcos(angle) << 4);

            int remX = (node.target14 - posX) >> 16;
            int remZ = (node.targetZ - posZ) >> 16;
            int stepX = motionX >> 16;
            int stepZ = motionZ >> 16;

            if (stepX * stepX + stepZ * stepZ >= remX * remX + remZ * remZ) {
                posX = node.target14;
                posZ = node.targetZ;
                motionX = 0;
                motionZ = 0;
                node.flags = node.flags & ~0x20;
                return true;
            }

            node.active = 1;
            node.cursor = node.cursor - 0x14;
            return false;
        };

        handlers[ActorFaceActor] = _ -> {
            if ((node.flags & 0x20) == 0) {
                ScriptVM target;
                if (get(0) == 0) target = getPlayer();
                else target = getActor(get(0), get(1));

                if (target == null)
                    return true;

                node.targetActor = target;
                node.target14 = get(2);
                node.flags = node.flags | 0x20;
            }

            int goal = facingAngle(this, node.targetActor);
            rotY = stepAngle(rotY, goal, node.target14);

            if (rotY != goal) {
                node.active = 1;
                node.cursor = node.cursor - 0x14;
                return false;
            }

            node.flags = node.flags & ~0x20;
            return true;
        };

        handlers[SetEntityAnimFrame] = _ -> {
            animStop = Math.min(get(0), animLastFrame);
            entityFlags |= 0x200;
            return true;
        };

        handlers[SetEntityAnimSpeed] = _ -> {
            animFrame = Math.min(get(0), animLastFrame) << 16;
            return true;
        };

        handlers[WaitAnimFrameMatch] = _ -> {
            node.active = 1;
            if ((animFrame >> 16) != animStop)
                node.cursor = node.cursor - 8;

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

    public static void closeMessage(int id) {
        openMessages.remove(Integer.valueOf(id));
        messageSections.remove(id);
        messageSection.remove(id);
    }

    public static void confirm() {
        List<Integer> messages = new ArrayList<>(openMessages);
        for (int id : messages) {
            int next = messageSection.get(id) + 1;
            if (next < messageSections.get(id).size())
                messageSection.put(id, next);
            else closeMessage(id);
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

        Logger.debug("action " + mode + " type=" + type
                + " frame0 rootOff=" + anim.channels()[0][0] + ","
                + anim.channels()[1][0] + ","
                + anim.channels()[2][0]
                + " rootRot=" + anim.channels()[3][0] + ","
                + anim.channels()[4][0] + ","
                + anim.channels()[5][0]);

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

    public int angleToPoint(int tx, int tz) {
        double dx = (long) posX - tx;
        double dz = (long) posZ - tz;
        int units = (int) Math.round(Math.atan2(dz, dx) * 2048 / Math.PI);
        return (0x1400 - units) & 0xFFF;
    }

    public static void startFade(int target, int duration) {
        fadeStart = fadeLevel;
        fadeTarget = target;
        fadeDuration = duration;
        fadeFrame = 0;
    }

    public static void stepFade() {
        if (fadeFrame >= fadeDuration) return;

        int divisor = Math.max(fadeDuration - 1, 1);
        fadeLevel = fadeStart + (fadeTarget - fadeStart) * fadeFrame / divisor;
        fadeFrame = fadeFrame + 1;
    }

    private void move(int base, int newMode) {
        if (entityMode != newMode) {
            setActionMode(newMode);
            entityMode = newMode;
        }

        int s = fixedMul(base, moveFactor);
        s = fixedMul(s, moveSpeed << 4);

        int diag = fixedMul(s, 0xB504);

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
                mz = -s;
            }
        } else if ((pad & 0x40) != 0) {
            facing += 0x400;
            mx = -s;
        } else if ((pad & 0x10) != 0) {
            facing += 0xC00;
            mx = s;
        }

        rotY = facing & 0xFFF;

        int c = rcos(cameraYaw & 0xFFF);
        int sn = rsin(cameraYaw & 0xFFF);
        motionX = (int) (((long) c * mx + (long) sn * mz) >> 12);
        motionY = 0;
        motionZ = (int) (((long) -sn * mx + (long) c * mz) >> 12);
    }

    public void setFade(int level) {
        fadeLevel = level;
        fadeStart = level;
        fadeTarget = level;
        fadeDuration = 0;
        fadeFrame = 0;
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
        for (int j = 0; j < nodes.size(); j++) {
            node = nodes.get(j);

            if ((node.flags & 0x50) != 0) continue;
            if (node.active == 0) continue;

            node.active = node.active - 1;
            if (node.active != 0) continue;

            if ((node.flags & 0x10) != 0) {
                nodes.remove(node);
                continue;
            }

            while (true) {
                int header = (int) u32LE(CONTAINER, node.cursor);
                int tail = (int) u32LE(CONTAINER, node.cursor + 4);
                int op = header & 0x1FFF;
                int argc = (header >> 13) & 15;

                if (op > 240 || !Decoder.valid[op]) {
                    Logger.error("Non Valid OpCode: " + op);
                    node.flags |= 0x10;
                    return;
                }

                int modes = header >> 17;
                for (int i = 0; i < argc; i++) {
                    if (i == 5) modes = tail;
                    mode[i] = modes & 7;
                    raw[i] = (int) u32LE(CONTAINER, node.cursor + 8 + 4 * i);
                    modes = modes >> 3;
                }

                node.cursor = node.cursor + 8 + 4 * argc;

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
    }

    public int facingAngle(ScriptVM from, ScriptVM to) {
        int dx = ((from.posX - to.posX) >> 16);
        int dz = ((from.posZ - to.posZ) >> 16);
        int units = (int) Math.round(Math.atan2(dz, dx) * 2048 / Math.PI);
        return (0x1400 - units) & 0xFFF;
    }

    public int stepAngle(int angle, int target, int speed) {
        int diff = ((target - angle + 2048) & 0xFFF) - 2048;
        if (Math.abs(diff) <= speed)
            return target;

        if (diff > 0)
            return (angle + speed) & 0xFFF;

        return (angle - speed) & 0xFFF;
    }

    public void findFloor() {
        if (COLLISION == null) return;
        if ((entityFlags & 2) != 0) return;

        int t = COLLISION.findTriangle(posX >> 16, posZ >> 16);
        if (t < 0) return;

        posY = COLLISION.floorY(t, posX, posZ);
    }

    public PEAnimation getAnimation() {
        return actions.get(actionMode);
    }

    public ScriptVM getActor(int type, int sub) {
        for (ScriptVM vm : actors) {
            if (vm.getType() == type && vm.getSub() == sub)
                return vm;
        }

        return null;
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