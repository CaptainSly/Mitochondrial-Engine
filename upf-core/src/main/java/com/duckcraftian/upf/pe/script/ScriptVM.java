package com.duckcraftian.upf.pe.script;

import com.duckcraftian.upf.pe.disc.RoomParser;
import com.duckcraftian.upf.pe.gfx.PEAnimation;
import com.duckcraftian.upf.pe.gfx.PECamera;
import com.duckcraftian.upf.pe.gfx.PEModel;
import com.duckcraftian.upf.pe.gfx.PEText;
import com.duckcraftian.upf.pe.rooms.Collision;
import com.duckcraftian.upf.ps1.PSXMath;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.*;
import java.util.function.IntConsumer;

import static com.duckcraftian.upf.pe.ParasiteEve.*;
import static com.duckcraftian.upf.pe.gfx.BackgroundAnimation.*;
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

    public static boolean backgroundDirty = false;

    public static int pad;
    public static int cameraYaw, cameraYaw2;

    public static int movieElapsed = -1;
    public static IntConsumer movieStarter = null;

    public static int[] fadeLevel = new int[3];
    public static int[] fadeStart = new int[3];
    public static int[] fadeTarget = new int[3];
    public static int fadeFrame = 0;
    public static int fadeDuration = 0;

    public static int viewportIndex = 0;
    public static boolean viewportChanged = false;

    public static boolean textboxSharedBackdrop = false;

    public static int roomLightR = 0xFF;
    public static int roomLightG = 0xFF;
    public static int roomLightB = 0xFF;

    public static int padTrigger, padRelease, padOld;

    public static int moveLock;
    public static int gameStateFlags = 0;
    public static int battleModeState = 0;
    public static int battleEscapeFlag = 0;

    public static int menuDimLevel;

    public static int messageBank;
    public static int menuChoice = -1;

    public static boolean cameraFrozen = false;
    public static boolean fadeAutoIn = true;
    public static boolean justLoaded = true;

    public static int[] playerName = {0x10, 0x48, 0x30};
    public static int[] pendingScroll = null;

    public static int serialCounter;

    public static Map<Integer, PEModel> models;
    public static Map<Integer, PEModel> sceneObjectModels;
    public static List<Integer> openMessages = new ArrayList<>();
    public static Map<Integer, List<int[]>> messageSections = new HashMap<>();
    public static Map<Integer, Integer> messageSection = new HashMap<>();
    public static Map<Integer, List<Integer>> messageDelays = new HashMap<>();
    public static Map<Integer, Integer> messageTimer = new HashMap<>();
    public static Map<Integer, List<int[]>> messageChoices = new HashMap<>();
    public static Map<Integer, Integer> messageCursor = new HashMap<>();

    // Entity Specific
    private int type;
    private int sub;

    private int baseX, baseY, baseZ;
    private int posX, posY, posZ;
    private int rotX, rotY, rotZ, savedRotY;
    private int bodyX, bodyY, bodyZ, bodyRadius;
    private int cylinderX, cylinderZ, cylinderRadius;
    private int motionX, motionY, motionZ;
    private int accelX, accelY, accelZ;
    private int deltaX, deltaY, deltaZ;

    private int moveFactor;
    private int moveSpeed = 0x1000;

    private int shade = 0x80;
    private int lightNegativeY = 0xC;
    private int lightPositiveY = 0x18;
    private int shadowMatrixIndex = 0;
    private int shadowIntensity = 0x80;
    private int shadowRadius;

    private int entityFlags;

    private int entityMode;
    private int actionMode;

    private int coreFlags;
    private int slot;
    private int combatantId;

    private int[] effects;
    private int[] effectParams;

    private int animPrev;
    private int animFrame;
    private int animStep = 0x10000;
    private int animStop;
    private int animLastFrame;

    private int face = -1;
    private int faceMirror = -1;
    private int hitRadius;
    private boolean hasBody;

    private int streamStart;

    private Node node;
    private int msgCursor;
    private int contactCursor;

    private int[] fields;

    private int serial;

    private ScriptVM parent = null;

    private List<Node> nodes;
    private Map<Integer, PEAnimation> actions;

    private int[] mode = new int[16];
    private int[] raw = new int[16];

    private int[] actorVars = new int[ACTOR_COUNT];
    private Handler[] handlers = new Handler[HANDLER_COUNT];

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

    // Combat
    private static boolean[] slotUsed = new boolean[7];
    private static int combatantCounter;
    private static int enemyCount;
    private static int roomFloorY;

    private static List<ScriptVM> actors = new ArrayList<>();
    private static List<int[]> queue = new ArrayList<>();

    public static int debugTick = 0;

    // TODO REMOVE THIS ONCE RNJESUS COMES TO TAKE HIS THRONE!
    public static final Random rnJesus = new Random();

    public static ScriptVM spawn(int type, int sub) {
        var vm = new ScriptVM();
        vm.type = type;
        vm.serial = serialCounter;
        vm.fields = new int[256];
        vm.effects = new int[54];
        vm.effectParams = new int[24];

        serialCounter++;

        PEModel m = models.get(type);
        if (m != null) vm.hitRadius = m.getHitRadius();
        vm.moveFactor = (type == 0) ? 0x10000 : 0x50000;
        vm.shadowRadius = vm.hitRadius * 2;
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
            int start = (type == 0) ? 21 : Collections.min(vm.actions.keySet());
            vm.setActionMode(start);
        }

        actors.add(current + 1, vm);
        return vm;
    }

    public static ScriptVM getPlayer() {
        for (ScriptVM vm : actors)
            if (vm.type == 0) {
                return vm;
            }

        return null;
    }

    public static int takePendingRoom() {
        int t = pendingRoom;
        pendingRoom = 0;
        return t;
    }

    public static int[] takePendingScroll() {
        int[] s = pendingScroll;
        pendingScroll = null;
        return s;
    }

    public static void loadRoom(byte[] container, byte[] bankContainer) {
        CONTAINER = container;
        BANK_CONTAINER = bankContainer;
        STARTS = RoomParser.readScriptStreams(container);
        actors.clear();

        try {
            PEAUDIO.loadRoomSfx(container);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Arrays.fill(SCRATCH, 0);
        Arrays.fill(NODE_FLAGS, 0);

        justLoaded = true;
        fadeAutoIn = true;
        current = -1;
        moveLock = 0;
        menuDimLevel = 0x60;
        viewportIndex = 0;
        cameraFrozen = false;

        serialCounter = 1;
        roomLightR = 0xFF;
        roomLightG = 0xFF;
        roomLightB = 0xFF;

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
        debugTick++;
        deliverMessages();

        padTrigger = pad & ~padOld;
        padRelease = padOld & ~pad;
        padOld = pad;

        for (int i = 0; i < actors.size(); i++) {
            ScriptVM vm = actors.get(i);
            if ((vm.entityFlags & 0x10) == 0) continue;
            if (vm.type == 0) moveLock &= ~0xD;
            actors.remove(i);
        }

        for (int i = 0; i < actors.size(); i++) {
            current = i;
            ScriptVM vm = actors.get(i);

            vm.baseX = vm.posX;
            vm.baseY = vm.posY;
            vm.baseZ = vm.posZ;
            vm.savedRotY = vm.rotY;
            vm.runFrame();

            if ((vm.type == 0) && (moveLock & 1) == 0) vm.controller();
            vm.integrate();
        }

        for (ScriptVM actor : actors)
            actor.updatePosition();

        for (ScriptVM actor : actors)
            actor.updateBodySphere();

        resolveContacts();

        for (ScriptVM actor : actors)
            if (actor.parent != null && (actor.entityFlags & 0x400000) != 0) {
                actor.posX = actor.parent.posX;
                actor.posY = actor.parent.posY;
                actor.posZ = actor.parent.posZ;
                actor.rotX = actor.parent.rotX;
                actor.rotY = actor.parent.rotY;
                actor.rotZ = actor.parent.rotZ;
            }


    }

    public static List<ScriptVM> getActors() {
        return actors;
    }

    public int[] modelMatrix() {
        int[] m = PSXMath.rotMatrixYXZ(0, rotY, 0);
        for (int i = 0; i < 9; i++) {
            m[i] = (m[i] * moveSpeed) >> 12;
        }

        return m;
    }

    public static void dumpActors(PECamera camera) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(
                "-=- Actors Size: " + actors.size() + " | "
                        + "Viewport Index: " + viewportIndex + " | "
                        + "Camera Position: " + camera.getCamX() + ", " + camera.getCamY() + " | "
                        + "Open Messages: " + openMessages.size() + " -=-="
        );

        // Actors
        for (ScriptVM actor : actors) {
            PEModel model = models.get(actor.type);
            PEAnimation anim = actor.getAnimation();
            int frame = actor.getFrame();
            sb.append(
                    "-=- Actor Type: " + actor.type + "/" + actor.sub + " #" + actor.serial + " | "
                            + "Position: " + (actor.posX >> 16) + ", " + (actor.posY >> 16) + ", " + (actor.posZ >> 16) + " | "
                            + "Yaw: " + actor.rotY + " (" + (actor.rotY * 360.0 / 4096) + " deg) | "
                            + "Action Mode: " + actor.actionMode + " f " + frame + "/" + actor.animLastFrame + " step " + (actor.animStep / 65536.0) + " | "
                            + "Entity Flags: " + Integer.toHexString(actor.entityFlags) + " | "
                            + "Node Size: " + actor.nodes.size() + "-=-="
            );

            if (model == null || anim == null) {
                String no = "";
                if (model == null) no = "model";
                else if (anim == null) no = "anim";
                else if (model == null && anim == null) no = "model or anim";

                sb.append("-=- there is no " + no + " -=-=");
            } else {
                int[] rot = actor.modelMatrix();
                int[] at = {actor.posX >> 16, actor.posY >> 16, actor.posZ >> 16};
                int[][] pose = model.pose(anim, frame, rot, at);
                int[] root = pose[0];
                int[] screen = camera.toScreen(root[9], root[10], root[11]);
                sb.append(
                        "-=- Root Offset: " + anim.channels()[0][frame] + ", " + anim.channels()[1][frame] + ", " + anim.channels()[2][frame] + " | "
                                + "Root World: " + root[9] + ", " + root[10] + ", " + root[11] + " | "
                                + "Screen: " + screen[0] + ", " + screen[1] + "-=-="
                );
            }

            if (actor == getPlayer() && model != null)
                sb.append("-=- Aya Slot: " + model.getMinSlot() + "min / " + model.getMaxSlot() + "max -=-");
        }

        Logger.debug(sb.toString());
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
                }
            }
        }

        queue.clear();
    }

    public void linkContact(ScriptVM other) {
        if (contactCursor == 0) return;

        for (Node node : nodes)
            if ((node.flags & 1) != 0 && (node.flags & 0x10) == 0 && (node.trigger == other.serial)) return;

        Node newNode = new Node();
        newNode.cursor = contactCursor;
        newNode.active = 1;
        newNode.flags = 1;
        newNode.trigger = other.serial;
        newNode.target18 = other.type;
        newNode.target1c = other.sub;
        nodes.addFirst(newNode);
    }

    public void slide(int[] wall, int r) {
        int dirX = COLLISION.edgeDirX()[wall[4]];
        int dirZ = COLLISION.edgeDirZ()[wall[4]];

        int deltaX = posX - baseX;
        int deltaZ = posZ - baseZ;
        int along = fixedMul(dirX, deltaX) + fixedMul(dirZ, deltaZ);

        int x = fixedMul(dirX, along) + baseX;
        int z = fixedMul(dirZ, along) + baseZ;

        if (COLLISION.wallDistance(x, z, wall) > r) {
            posX = x;
            posZ = z;
            return;
        }

        for (int step = 0; ; step++) {
            for (int dir = 0; dir < 4; dir++) {
                int cx = x;
                int cz = z;

                if (dir == 0) cx += step << 16;
                if (dir == 1) cx -= step << 16;
                if (dir == 2) cz += step << 16;
                if (dir == 3) cz -= step << 16;
                if (COLLISION.wallDistance(cx, cz, wall) > r) {
                    posX = cx;
                    posZ = cz;
                    return;
                }
            }
        }

    }

    public void updatePosition() {
        if ((entityFlags & 0x80) != 0) return;
        if (COLLISION == null) return;
        if ((posX >> 16) == (baseX >> 16) && (posZ >> 16) == (baseZ >> 16) && posY == baseY) return;
        if (face < 0) return;
        faceMirror = face;

        int x = posX >> 16;
        int z = posZ >> 16;
        int r = hitRadius * moveSpeed / 4096;
        int[] wall = new int[5];

        if (!COLLISION.floorClear(face, x, z, r, wall)) {
            if (type != 0) {
                posX = baseX;
                posY = baseY;
                posZ = baseZ;
                return;
            }

            slide(wall, r);
            x = posX >> 16;
            z = posZ >> 16;
            int saved = wall[4];

            if (!COLLISION.floorClear(face, x, z, r, wall)) {
                if (wall[4] == saved) {
                    if (COLLISION.floorClear(face, x, z, r, wall, saved)) {
                        posX = baseX;
                        posY = baseY;
                        posZ = baseZ;
                        return;
                    }
                }

                slide(wall, r);
                x = posX >> 16;
                z = posZ >> 16;
                if (!COLLISION.floorClear(face, x, z, r, wall)) {
                    posX = baseX;
                    posY = baseY;
                    posZ = baseZ;
                    return;
                }
            }
        }

        int oldX = baseX >> 16;
        int oldZ = baseZ >> 16;
        x = posX >> 16;
        z = posZ >> 16;

        int t = face;
        if (!COLLISION.pointInTri(face, x, z))
            t = COLLISION.walkToFace(face, -1, x, z, oldX, oldZ);

        if (t < 0) {
            posX = baseX;
            posY = baseY;
            posZ = baseZ;
            return;
        }

        face = t;
        if ((entityFlags & 2) == 0) posY = COLLISION.floorY(t, posX, posZ);
    }

    public void start(int streamStart) {
        this.streamStart = streamStart;

        Node main = new Node();
        main.cursor = streamStart;
        main.active = 1;
        nodes = new ArrayList<>();
        nodes.add(main);

        handlers[SetBackgroundDimLevel] = _ -> {
            menuDimLevel = get(0);
            return true;
        };

        handlers[MakeCombatant] = _ -> {
            int index = 0;
            while (index < 7 && slotUsed[index]) {
                index = index + 1;
            }

            if (index == 7) return true;
            slotUsed[index] = true;
            slot = index;
            combatantCounter = (combatantCounter + 1) & 0xFF;
            combatantId = combatantCounter;
            for (int i = 0; i < 256; i++)
                fields[i] = 0;

            fields[110] = 10;
            coreFlags = 0;
            if ((entityFlags & 0x2000) == 0) {
                setActionMode(2);
                enemyCount = enemyCount + 1;
            }

            return true;
        };

        handlers[GetField] = _ -> {
            int id = get(0) & 0xFF;
            set(1, readCombatField(id));
            return true;
        };

        handlers[SetField] = _ -> {
            int id = get(0) & 0xFF;
            writeCombatField(id, get(1));
            return true;
        };

        handlers[FindByTypeId] = _ -> {
            ScriptVM target = getActor(get(0), get(1));
            if (target != null) {
                int result = target.readCombatField(get(2) & 0xFF);
                set(3, result);
            }

            return true;
        };

        handlers[SetCombatantOrActorField] = _ -> {
            ScriptVM target = getActor(get(0), get(1));
            if (target != null)
                target.writeCombatField(get(2) & 0xFF, get(3));

            return true;
        };

        handlers[DispatchScriptCmd] = _ -> {
            int sub = get(0);
            switch (sub) {
                case 2601:
                    shadowIntensity = get(1) & 0xFF;
                    break;
                case 2500:
                    spawnAt(get(1), get(2), get(3), get(4), get(5), false);
                    break;
                case 2600:
                    shadowMatrixIndex = get(1) & 0xFF;
                    break;
                case 2300:
                    textboxSharedBackdrop = (get(1) != 0);
                    break;
                case 2700:
                    roomLightR = get(1);
                    roomLightG = get(2);
                    roomLightB = get(3);
                    break;
                case 2100, 2101, 2900, 3100, 3200, 2201:
                    Logger.debug("Just keep rolling rolling rolling");
                    break/* Stuff*/; // BACK UP, BACK UP! WHATCHYA GONNA DO NOW!
                default:
                    Logger.error("Op 237 sub-command " + sub + " unimplemented");
                    break;
            }

            return true;
        };

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
            if ((node.flags & 3) != 0)
                nodes.addFirst(child);
            else
                nodes.add(nodes.indexOf(node) + 1, child);

            return true;
        };

        handlers[GetMenuChoice] = _ -> {
            set(0, menuChoice);
            return true;
        };

        handlers[GetDistanceComponents] = _ -> {
            ScriptVM other = getActor(get(0), get(1));
            if (other == null) {
                set(2, -1);
                return true;
            }

            int dist = Math.abs(posX - other.posX) + Math.abs(posY - other.posY) + Math.abs(posZ - other.posZ);
            set(2, dist);
            return true;
        };

        handlers[QueryField] = _ -> {
            int sel = get(0);
            ScriptVM other = getActor(get(1), get(2));
            if (other == null) {
                set(3, -1);
                return true;
            }

            if (sel == 0) set(3, other.actionMode);
            else if (sel == 1) set(3, other.entityFlags);
            else if (sel == 2) set(3, other.getFrame());
            else if (sel == 3) set(3, other.animLastFrame);

            return true;
        };

        handlers[OpenNameEntryMenu] = _ -> {
            // EVEN HANK WAS SCARED!
            // Hank: I don't wanna lose my dong.
            playerName = new int[]{code("H"), code("a"), code("n"), code("k")};
            return true;
        };

        handlers[ScrollCamera] = _ -> {
            pendingScroll = new int[]{get(0), get(1), get(2), 0};
            return true;
        };

        handlers[SwapViewports] = _ -> {
            viewportIndex = get(0);
            viewportChanged = true;
            return true;
        };

        handlers[JumpIfZero] = _ -> {
            int a = get(0);
            int target = get(1);

            if (a == 0)
                node.cursor = streamStart + target * 2;

            return true;
        };

        handlers[ClearBit] = _ -> {
            int current = get(0);
            int bit = get(1);

            set(0, current & ~(1 << bit));
            return true;
        };

        handlers[SetBit] = _ -> {
            int current = get(0);
            int bit = get(1);
            set(0, current | (1 << bit));
            return true;
        };

        handlers[Random] = _ -> {
            int start = get(1);
            int stop = get(2);
            int word = 0;
            int r = 0;

            if (start == stop)
                word = rnJesus.nextInt();
            else {
                r = rnJesus.nextInt() & 0xFFFF;
                word = (int) (((((long) r) * ((long) (stop - start))) >> 16) + start);
            }

            set(0, word);
            return true;
        };

        handlers[SetGameStateFlags] = _ -> {
            gameStateFlags |= get(0);
            return true;
        };

        handlers[TestGameStateFlags] = _ -> {
            set(1, (gameStateFlags & get(0)) != 0 ? 1 : 0);
            return true;
        };

        handlers[ClearGameStateFlags] = _ -> {
            gameStateFlags &= ~get(0);
            return true;
        };

        handlers[SetField] = _ -> {
            int id = get(0) & 0xFF;
            int value = get(1);

            fields[id] = value;
            return true;
        };

        handlers[GetField] = _ -> {
            int id = get(0) & 0xFF;
            set(1, fields[id]);
            return true;
        };

        handlers[JumpIfNotEqual] = _ -> {
            int a = get(0);
            int b = get(1);
            int target = get(2);

            if (a != b)
                node.cursor = streamStart + target * 2;

            return true;
        };

        handlers[SetBackgroundAnimFlags] = _ -> {
            int c = get(0);
            ctrlFlags[c] = ctrlFlags[c] | (get(1) & 0x30);
            return true;
        };

        handlers[SetEntityParent] = _ -> {
            ScriptVM target = (get(0) == 0) ? getPlayer() : getActor(get(0), get(1));
            if (target == null) return true;
            parent = target;
            target.entityFlags |= 0x100000;
            entityFlags |= 0x600000;
            return true;
        };

        handlers[ScrollCameraAlt] = _ -> {
            pendingScroll = new int[]{get(0), get(1), get(2), 1};
            return true;
        };

        handlers[RoomChange] = _ -> {
            pendingRoom = get(0);
            return false;
        };

        handlers[BlockCollisionTriangle] = _ -> {
            COLLISION.triKind()[get(0)] |= 0x80;
            return true;
        };

        handlers[UnblockCollisionTriangle] = _ -> {
            COLLISION.triKind()[get(0)] &= 0x7F;
            return true;
        };

        handlers[SelectFieldSetter] = _ -> {
            int pos = (get(1) < 0) ? 0 : streamStart + get(1) * 2;
            if (get(0) == 2) msgCursor = pos;
            else if (get(0) == 1) contactCursor = pos;
            return true;
        };

        handlers[GetTouchingActor] = _ -> {
            set(0, node.target18);
            set(1, node.target1c);
            return true;
        };

        handlers[GetAngleToEntity] = _ -> {
            ScriptVM target = null;
            if (get(0) == 0)
                target = getPlayer();
            else target = getActor(get(0), get(1));

            if (target == null) {
                set(2, -1);
                return true;
            }

            int dx = (this.posX - target.posX) >> 16;
            int dz = (this.posZ - target.posZ) >> 16;
            int units = (int) Math.round(Math.atan2(dz, dx) * 2048 / Math.PI);

            int angle = 0x1400 - units;
            if (angle >= 0x1001)
                angle -= 0x1000;

            angle -= rotY;
            if (angle < 0)
                angle += 0x1000;

            set(2, angle);
            return true;
        };

        handlers[IsButtonDown] = _ -> {
            int mode = get(0);
            int mask = get(1);

            int state = 0;
            if (mode == 0) state = pad;
            else if (mode == 1) state = padTrigger;
            else if (mode == 2) state = padRelease;
            else return true;

            if ((state & mask) == mask) set(2, 1);
            else set(2, 0);

            return true;
        };

        handlers[GetNodeTarget14] = _ -> {
            set(0, node.target14);
            return true;
        };

        handlers[SetHitRadius] = _ -> {
            hitRadius = get(0) >> 16;
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
            startFade(255, 255, 255, get(0));
            return true;
        };

        handlers[FadeSceneIn] = _ -> {
            startFade(0, 0, 0, get(0));
            return true;
        };

        handlers[ActorSnapToAnimAnchor] = _ -> {
            PEModel model = models.get(type);
            if (model == null || getAnimation() == null)
                return true;

            int[] rot = modelMatrix();
            int[] at = {posX >> 16, posY >> 16, posZ >> 16};
            int[][] pose = model.pose(getAnimation(), getFrame(), rot, at);

            int[] bone = pose[model.getAnchorMatrixIndex()];
            int[] boneMatrix = Arrays.copyOfRange(bone, 0, 9);
            int[] p = PSXMath.mulVector(boneMatrix, 0, model.getAnchorY(), 0);

            posX = bone[9] << 16;
            posY = bone[10] << 16;
            posZ = bone[11] << 16;

            return true;
        };

        handlers[ClearEntityFlag40] = _ -> {
            fadeAutoIn = false;
            return true;
        };

        handlers[SetBattleModeState6] = _ -> {
            battleModeState = 6;
            return true;
        };

        handlers[GetBattleModeState] = _ -> {
            set(0, battleModeState);
            return true;
        };
        handlers[ClearBattleModeState] = _ -> {
            battleModeState = 0;
            return true;
        };

        handlers[SetMoveLockBit2] = _ -> {
            moveLock |= 4;
            return true;
        };

        handlers[SetNodeFlag80] = _ -> {
            node.flags |= 0x80;
            return true;
        };

        handlers[SetEntityFlag1000000] = _ -> {
            entityFlags |= 0x1000000;
            return true;
        };

        handlers[SetEscapeFlag] = _ -> {
            battleEscapeFlag = 1;
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
            List<Integer> delays = new ArrayList<>();
            List<int[]> choices = new ArrayList<>();
            openMessages.add(id);
            messageSections.put(id, PEText.layoutMessage(CONTAINER, messageBank, id, delays, choices));
            messageChoices.put(id, choices);
            messageCursor.put(id, 0);
            messageDelays.put(id, delays);
            messageSection.put(id, 0);
            messageTimer.put(id, 0);

            menuChoice = -1;
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
            } else if (get(0) == 5) {
                set(3, target.rotX);
                set(4, target.rotY);
                set(5, target.rotZ);
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
            startFade(get(1), get(2), get(3), get(0));
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
            spawnAt(get(0), get(1), get(2), get(3), get(4), true);
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

        handlers[UnfreezeCamera] = _ -> {
            cameraFrozen = false;
            return true;
        };

        handlers[FreezeCamera] = _ -> {
            cameraFrozen = true;
            return true;
        };

        handlers[EntityMarkDead] = _ -> {
            entityFlags |= 0x10;
            return false;
        };

        handlers[SetBackgroundAnimFrame] = _ -> {
            int c = get(0);
            ctrlElapsed[c] = 0;
            ctrlPos[c] = (ctrlPos[c] & 0xFF) | (get(1) << 16);
            ctrlFlags[c] |= 2;
            return true;
        };

        handlers[SetBackgroundAnimRunning] = _ -> {
            int c = get(0);
            if (get(1) != 0) ctrlFlags[c] |= 6;
            else ctrlFlags[c] &= 0xF9;

            return true;
        };

        handlers[SetBackgroundAnimStep] = _ -> {
            ctrlStep[get(0)] = (short) (get(1) >> 8);
            return true;
        };

        handlers[SetEntityMoveFactor] = _ -> {
            moveFactor = get(0);
            return true;
        };

        handlers[PlayBackgroundAnimFromTo] = _ -> {
            int c = get(0), from = get(1), to = get(2);
            ctrlElapsed[c] = 0;
            ctrlPos[c] = (ctrlPos[c] & 0xFF) | (from << 16);
            ctrlFlags[c] |= 6;
            slotDuration[c][to] = -1;

            if ((ctrlStep[c] > 0 && to < from) || (ctrlStep[c] < 0 && from < to))
                ctrlStep[c] = -ctrlStep[c];

            return true;
        };


        handlers[136] = _ -> true;

        handlers[WaitForSequenceFrame] = _ -> {
            if (movieElapsed == get(0)) return true;
            if (movieElapsed > 0) {
                node.active = 1;
                node.cursor = node.cursor - 12;
                return false;
            }

            return true;
        };
        handlers[WaitWhileSequenceRunning] = _ -> {
            if (movieElapsed > 0) {
                node.active = 1;
                node.cursor = node.cursor - 8;
                return false;
            }
            return true;
        };

        handlers[Noop145] = _ -> true;

        handlers[ScaleAnimValue] = _ -> {
            shadowRadius = (hitRadius * 2 * get(0)) >> 16;
            return true;
        };

        handlers[PlayFMV] = _ -> {
            movieStarter.accept(get(0));
            return true;
        };


        handlers[166] = _ -> true;

        handlers[SoundCommand] = _ -> {
            int sub = get(0);
            if (sub == 0xC8 || sub == 0x198) {
                try {
                    PEAUDIO.startMusic(CONTAINER, get(1));
                    set(5, 0);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (sub == 0x12C) {
                set(5, PEAUDIO.playSfx(CONTAINER, get(1), get(2), get(3), get(4)));
            } else if (sub == 0x190 || sub == 0x191 || sub == 0x192 || sub == 0x193 || sub == 0x194) {
            } else if (sub == 0x15F) {
                set(5, PEAUDIO.playSfxAt(get(1), getPosX() >> 16, getPosY() >> 16, getPosZ() >> 16));
            } else if (sub == 0x160) {
                ScriptVM target = null;
                if (get(2) == 0) target = getPlayer();
                else {
                    for (ScriptVM actor : getActors()) {
                        if (actor.type == get(2) && actor.sub == get(3) && (actor.entityFlags & 0x10) == 0) {
                            target = actor;
                            break;
                        }
                    }
                }
                if (target != null) {
                    set(5, PEAUDIO.playSfxAt(get(1), target.getPosX() >> 16, target.getPosY() >> 16, target.getPosZ() >> 16));
                }
            } else if (sub == 0x161) {
                set(5, PEAUDIO.playSfxAt(get(1), get(2) >> 16, get(3) >> 16, get(4) >> 16));
            } else if (sub == 0x195) {
                PEAUDIO.setStepSlot();
            } else if (sub == 0x196) {
                PEAUDIO.addActorSound(type, this.sub, get(1), get(2), get(3), get(3));
            } else if (sub == 0x197) {
                PEAUDIO.addActorSound(type, this.sub, get(1), get(2), get(3), get(4));
            } else if (sub == 0x199) {
                PEAUDIO.setCDVolume(get(1));
            } else if (sub == 0xC9) {
                PEAUDIO.stopMusic(get(1));
            } else if (sub == 0xCD) {
                PEAUDIO.setMusicVolume(get(1), get(2));
            } else if (sub == 0xCE) {
                PEAUDIO.slideMusicVolume(get(1), get(2) * 2, get(3));
            } else if (sub == 0xCF) {
                PEAUDIO.slideMusicVolumeFromTo(get(1), get(2) * 2, get(3), get(4));
            } else if (sub == 0xD9) {
                PEAUDIO.resetMusic();
            } else {
                Logger.error("Op 234 Sub Command 0x" + Integer.toHexString(sub) + " unimplemented");
            }

            return true;
        };

        handlers[DrawActorLit] = _ -> {
            shade = get(0);
            lightNegativeY = get(1);
            lightPositiveY = get(2);

            // Yeah! Scie...err Software Engineering. BITCH!
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
                case 2:
                    motionX = get(1);
                    motionY = get(2);
                    motionZ = get(3);
                    break;
                case 3:
                    accelX = get(1);
                    accelY = get(2);
                    accelZ = get(3);
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


        handlers[FindByTypeId] = _ -> {
            int wantType = get(0);
            int wantSub = get(1);
            int id = get(2) & 0xFF;

            ScriptVM found = null;
            for (ScriptVM actor : actors)
                if (wantType == 0) {
                    if (actor.type == 0) {
                        found = actor;
                        break;
                    }
                } else if (actor.type == wantType && actor.sub == wantSub && (actor.entityFlags & 0x10) == 0) {
                    found = actor;
                    break;
                }

            if (found != null)
                set(3, found.fields[id]);

            return true;
        };

        handlers[SetCurrentActionMode] = _ -> {
            setActionMode(get(0));
            entityFlags &= ~0x100;
            return true;
        };

        handlers[ClearInputFlagBit2] = _ -> {
            moveLock = moveLock & ~4;
            return true;
        };

        handlers[GetEntityVec3] = _ -> {
            int sel = get(0);
            if (sel == 0) {
                set(1, posX);
                set(2, posY);
                set(3, posZ);
            } else if (sel == 1) {
                set(1, baseX);
                set(2, baseY);
                set(3, baseZ);
            } else if (sel == 2) {
                set(1, motionX);
                set(2, motionY);
                set(3, motionZ);
            } else if (sel == 3) {
                set(1, accelX);
                set(2, accelY);
                set(3, accelZ);
            } else if (sel == 5) {
                set(1, rotX);
                set(2, rotY);
                set(3, rotZ);
            } else if (sel == 6) {
                set(1, deltaX);
                set(2, deltaY);
                set(3, deltaZ);
            }

            return true;
        };

        handlers[SetAttackEffect] = _ -> {
            int index = get(0) & 0xFF;
            int base = index * 9;

            effects[base + 0] = 0;
            effects[base + 1] = get(1) & 0xFF;
            effects[base + 2] = get(2) & 0xFF;
            effects[base + 3] = get(3) & 0xFF;
            effects[base + 6] = get(4) & 0xFFFF;
            return true;
        };

        handlers[SetAttackEffectFull] = _ -> {
            int index = get(0) & 0xFF;
            int base = index * 9;

            effects[base + 0] = 0;
            effects[base + 1] = get(1) & 0xFF;
            effects[base + 2] = get(2) & 0xFF;
            effects[base + 3] = get(3) & 0xFF;
            effects[base + 6] = get(4) & 0xFFFF;

            for (int i = 0; i < 4; i++) {
                effectParams[index * 4 + i] = get(5 + i) & 0xFF;
            }

            effects[base + 7] = get(9) & 0xFF;
            effects[base + 8] = get(10) & 0xFF;
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

        handlers[SetRenderEntryEnabled] = _ -> {
            int layer = get(0);
            int enabled = get(1);

            if (enabled != 0)
                entryFlags[layer] |= 2;
            else
                entryFlags[layer] &= ~2;

            return true;
        };

        handlers[SetBackgroundLayerPosition] = _ -> {
            int l = get(0);
            int x = (short) get(1);
            int y = (short) get(2);
            int depth = get(3);

            int sx = ((short) (entryBaseX[l] + x));
            if (sx < entryMinX[l])
                sx = entryMinX[l];
            else if (sx > entryMaxX[l])
                sx = entryMaxX[l];

            int sy = ((short) (entryBaseY[l] + y));
            if (sy < entryMinY[l])
                sy = entryMinY[l];
            else if (sy > entryMaxY[l])
                sy = entryMaxY[l];

            entryScrX[l] = sx;
            entryScrY[l] = sy;
            entryDepth[l] = (entryBaseDepth[l] + depth) & 0xFFF;
            backgroundDirty = true;
            return true;
        };

        handlers[ClearCurrentFlag80] = _ -> {
            entityFlags &= ~0x80;
            return true;
        };

        handlers[GetBackgroundAnimFrame] = _ -> {
            int c = get(0);
            set(1, ctrlPos[c] >> 16);
            return true;
        };

        handlers[SetBackgroundAnimFlagsInverted] = _ -> {
            int c = get(0);
            ctrlFlags[c] |= (~get(1)) & 0x30;
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

    public void updateBodySphere() {
        PEModel model = models.get(type);
        PEAnimation anim = getAnimation();

        if (model == null || anim == null) {

            if (model == null)
                hasBody = false;

            bodyX = posX >> 16;
            bodyY = posY >> 16;
            bodyZ = posZ >> 16;
            cylinderRadius = 0;
            bodyRadius = 0;
            return;
        }

        hasBody = true;

        int[] rot = PSXMath.rotMatrixYXZ(0, rotY, 0);
        int[] at = new int[]{posX >> 16, posY >> 16, posZ >> 16};
        int[][] pose = model.pose(anim, getFrame(), rot, at);
        int[] anchor = pose[model.getAnchorMatrixIndex()];
        int[] root = pose[0];

        int offX = (short) (model.getBodyX() + anim.bodyX());
        int offY = (short) (model.getBodyY() + anim.bodyY());
        int offZ = (short) (model.getBodyZ() + anim.bodyZ());

        int[] bodyTurned = mulVector(Arrays.copyOfRange(root, 0, 9), offX, offY, offZ);
        bodyX = bodyTurned[0] + root[9];
        bodyY = bodyTurned[1] + root[10];
        bodyZ = bodyTurned[2] + root[11];
        bodyRadius = anim.bodyRadius() * moveSpeed / 4096;

        int[] cylTurned = mulVector(Arrays.copyOfRange(anchor, 0, 9), 0, model.getAnchorY(), 0);
        cylinderX = cylTurned[0] + anchor[9];
        cylinderZ = cylTurned[2] + anchor[11];
        cylinderRadius = hitRadius * moveSpeed / 4096;

    }

    public static boolean takeBackgroundDirty() {
        boolean dirty = backgroundDirty;
        backgroundDirty = false;
        return dirty;
    }

    public static void checkAutoFadeIn() {
        if (!justLoaded) return;
        justLoaded = false;
        if (fadeAutoIn && fadeTarget[0] == 255 && fadeTarget[1] == 255 && fadeTarget[2] == 255)
            startFade(0, 0, 0, 15);
    }


    public int readCombatField(int id) {
        if (type == 0) {
            if ((id >= 0 && id <= 4) || id == 6 || id == 7)
                return fields[id];
            else if (id >= 10 && id <= 14)
                return fields[id];
            return -1000;
        }

        switch (id) {
            case 41:
                int low = fields[id] & 0xFF;
                if (low >= 128) low = low - 256;
                return low;
            case 61, 43:
                return fields[id] & 0xFFFF;
            case 44, 60:
                if (fields[id] < 0) return 0;
                return fields[id];
            case 77:
                return (coreFlags >> 13) & 3;
            case 82:
                return (coreFlags >> 24) & 0x3F;
            case 48, 130:
                return 0;
        }

        return -1000;
    }

    public void writeCombatField(int id, int value) {
        if (type == 0) {
            if (id == 255)
                roomFloorY = value;
            else
                fields[id] = value;

            return;
        }

        switch (id) {
            case 74:
                coreFlags = (coreFlags & ~0x10) | ((value & 1) << 4);
                break;
            case 81:
                coreFlags = (coreFlags & ~0x100000) | ((value & 1) << 20);
                break;
            default:
                fields[id] = value;
        }
    }

    public static void resolveContacts() {

        for (ScriptVM actor : actors) {
            actor.entityFlags &= ~0x40000;
            actor.entityFlags &= ~0x02000000;
        }

        for (int i = 0; i < actors.size(); i++) {
            ScriptVM self = actors.get(i);
            if ((self.entityFlags & 0x20) != 0) continue;

            for (int j = i + 1; j < actors.size(); j++) {
                ScriptVM other = actors.get(j);

                if (self.parent == other || other.parent == self) continue;
                if ((self.entityFlags & 0x20000) != 0 && other != getPlayer()) continue;
                if ((other.entityFlags & 0x20000) != 0 && self != getPlayer()) continue;
                if ((other.entityFlags & 0x20) != 0) continue;

                int dx = self.bodyX - other.bodyX;
                int dy = self.bodyY - other.bodyY;
                int dz = self.bodyZ - other.bodyZ;
                int reach = self.bodyRadius + other.bodyRadius;

                int bodyDist = dx * dx + dy * dy + dz * dz;
                int reachSq = reach * reach;

                if (!self.hasBody || !other.hasBody) {
                    if (bodyDist <= reachSq) {
                        self.linkContact(other);
                        other.linkContact(self);
                    }

                    continue;
                }

                if (bodyDist > reachSq) continue;
                if (self.cylinderRadius == 0 || other.cylinderRadius == 0) continue;

                int cx = self.cylinderX - other.cylinderX;
                int cz = self.cylinderZ - other.cylinderZ;
                int side = self.cylinderRadius + other.cylinderRadius;

                int cylDist = cx * cx + cz * cz;
                int sideSq = side * side;

                if (cylDist > sideSq) {
                    if (cylDist <= sideSq + 10000) {
                        self.linkContact(other);
                        other.linkContact(self);
                    }

                    continue;
                }

                if (self != getPlayer() && (self.entityFlags & 0x01000000) == 0)
                    self.rotY = self.savedRotY;

                if (other != getPlayer() && (other.entityFlags & 0x01000000) == 0)
                    other.rotY = other.savedRotY;

                int selfHeadX = (self.posX - self.baseX) >> 16;
                int selfHeadZ = (self.posZ - self.baseZ) >> 16;

                if (selfHeadX * cx + selfHeadZ * cz < 0) rollBack(self);

                int otherHeadX = (other.posX - other.baseX) >> 16;
                int otherHeadZ = (other.posZ - other.baseZ) >> 16;

                if (otherHeadX * cx + otherHeadZ * cz > 0) rollBack(other);

                self.linkContact(other);
                other.linkContact(self);
            }
        }

    }

    public static void rollBack(ScriptVM actor) {
        actor.posX = actor.baseX;
        actor.posY = actor.baseY;
        actor.posZ = actor.baseZ;
        actor.face = actor.faceMirror;
        actor.entityFlags |= 0x40000;
    }

    public static void openMessage(int id) {
        List<Integer> delays = new ArrayList<>();
        List<int[]> choices = new ArrayList<>();
        openMessages.add(id);
        messageSections.put(id, PEText.layoutMessage(CONTAINER, messageBank, id, delays, choices));
        messageChoices.put(id, choices);
        messageCursor.put(id, 0);
        messageDelays.put(id, delays);
        messageSection.put(id, 0);
        messageTimer.put(id, 0);
        menuChoice = -1;
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

    public void spawnAt(int type, int sub, int x, int y, int z, boolean snapToFloor) {
        ScriptVM a = spawn(type, sub);
        a.posX = x;
        a.posY = y;
        a.posZ = z;

        if (snapToFloor)
            a.findFloor();
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
        messageDelays.remove(id);
        messageSection.remove(id);
        messageTimer.remove(id);
        messageChoices.remove(id);
        messageCursor.remove(id);
    }

    public static void advanceMessage(int id) {
        int next = messageSection.get(id) + 1;
        if (next < messageSections.get(id).size()) {
            messageSection.put(id, next);
            messageCursor.put(id, 0);
            messageTimer.put(id, 0);
        } else closeMessage(id);
    }

    public static void stepMessages() {
        List<Integer> messages = new ArrayList<>(openMessages);
        for (int id : messages) {
            int delay = messageDelays.get(id).get(messageSection.get(id));
            if (delay > 0) {
                int t = messageTimer.get(id) + 1;
                if (t >= delay)
                    advanceMessage(id);
                else
                    messageTimer.put(id, t);
            }
        }
    }

    public static boolean stepBackgroundAnimations(int activeGroup) {
        for (int c = 0; c < ctrlFlags.length; c++) {
            if ((ctrlFlags[c] & 2) == 0 || (ctrlFlags[c] & 0x14) == 0) continue;
            if ((ctrlPos[c] & 0xFF) != activeGroup) continue;

            for (int j = 0; j < ctrlLength[c]; j++)
                entryFlags[slotEntry[c][j]] &= ~2;

            int value = ctrlPos[c] >> 8;
            int frame = value >> 8;
            entryFlags[slotEntry[c][frame]] |= 2;

            if (slotDuration[c][frame] < 0) {
                slotDuration[c][frame] = 0;
                ctrlElapsed[c] = 0;
                ctrlFlags[c] &= ~4;
                break;
            }

            ctrlElapsed[c]++;
            if (ctrlElapsed[c] >= slotDuration[c][frame]) {
                ctrlElapsed[c] = 0;
                value += ctrlStep[c];
                int length = ctrlLength[c];
                if ((value >> 8) >= length) {
                    value = (ctrlFlags[c] & 0x20) != 0 ? value % (length << 8) : 0;
                    ctrlFlags[c] &= ~4;
                } else if ((value >> 8) < 0) {
                    value = (ctrlFlags[c] & 0x20) != 0 ? (length << 8) - ((-value) % (length << 8)) : (length - 1) << 8;
                    ctrlFlags[c] &= ~4;
                }
                ctrlPos[c] = (ctrlPos[c] & 0xFF) | (value << 8);
            }
        }

        boolean changed = !Arrays.equals(lastFlags, entryFlags);
        lastFlags = Arrays.copyOf(entryFlags, entryFlags.length);
        return changed;
    }

    public static void moveChoice(int dir) {
        for (int id : openMessages) {
            int[] choice = messageChoices.get(id).get(messageSection.get(id));
            if (choice[0] > 0) {
                int c = messageCursor.get(id) + dir;
                c = Math.max(0, Math.min(c, choice[0] - 1));
                messageCursor.put(id, c);
            }
        }
    }

    public static void confirm() {
        List<Integer> messages = new ArrayList<>(openMessages);
        for (int id : messages) {
            int step = messageSection.get(id);
            int[] choice = messageChoices.get(id).get(step);
            if (choice[0] > 0)
                menuChoice = messageCursor.get(id);

            int delay = messageDelays.get(id).get(messageSection.get(id));
            if (delay == 0)
                advanceMessage(id);
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
        if ((entityFlags & 0x100000) != 0) {
            for (ScriptVM actor : actors)
                if (actor.parent == this && (actor.entityFlags & 0x200000) != 0)
                    actor.setActionMode(mode);
        }
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

    public static void startFade(int r, int g, int b, int duration) {
        fadeAutoIn = false;
        fadeStart[0] = fadeTarget[0];
        fadeStart[1] = fadeTarget[1];
        fadeStart[2] = fadeTarget[2];
        fadeTarget[0] = r;
        fadeTarget[1] = g;
        fadeTarget[2] = b;
        fadeDuration = duration;
        fadeFrame = 0;
    }

    public static void stepFade() {
        if (fadeFrame >= fadeDuration) {
            fadeLevel[0] = fadeTarget[0];
            fadeLevel[1] = fadeTarget[1];
            fadeLevel[2] = fadeTarget[2];
            return;
        }

        int divisor = Math.max(fadeDuration - 1, 1);
        fadeLevel[0] = fadeStart[0] + (fadeTarget[0] - fadeStart[0]) * fadeFrame / divisor;
        fadeLevel[1] = fadeStart[1] + (fadeTarget[1] - fadeStart[1]) * fadeFrame / divisor;
        fadeLevel[2] = fadeStart[2] + (fadeTarget[2] - fadeStart[2]) * fadeFrame / divisor;

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

    public void advanceAnim() {
        if (animStep > 0)
            PEAUDIO.actorSounds(type == 0, type, sub, actionMode, animPrev >> 16, animFrame >> 16, getPosX() >> 16, getPosY() >> 16, getPosZ() >> 16);
        animPrev = animFrame;
        entityFlags &= ~8;
        if (parent != null && (entityFlags & 0x200000) != 0) {
            setActionMode(parent.actionMode);
            animFrame = parent.animFrame;
            return;
        }

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
        if ((entityFlags & 0x80) != 0) return;


        int t = COLLISION.findTriangle(posX >> 16, posZ >> 16);
        if (t < 0) return;
        face = t;

        if ((entityFlags & 2) == 0)
            posY = COLLISION.floorY(t, posX, posZ);
    }

    public PEAnimation getAnimation() {
        return actions.get(actionMode);
    }

    public ScriptVM getActor(int type, int sub) {
        if (type == 0) return getPlayer();

        for (ScriptVM vm : actors) {
            if (vm.getType() == type && vm.getSub() == sub && (vm.entityFlags & 0x10) == 0) return vm;
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

    public int getShade() {
        return shade;
    }

    public int getLightNegativeY() {
        return lightNegativeY;
    }

    public int getLightPositiveY() {
        return lightPositiveY;
    }

    public int getShadowRadius() {
        return shadowRadius;
    }

    public int getEntityFlags() {
        return entityFlags;
    }

    public int getShadowMatrixIndex() {
        return shadowMatrixIndex;
    }

    public int getShadowIntensity() {
        return shadowIntensity;
    }

    public static List<int[]> getQueue() {
        return queue;
    }

    public int[] getMode() {
        return mode;
    }

}