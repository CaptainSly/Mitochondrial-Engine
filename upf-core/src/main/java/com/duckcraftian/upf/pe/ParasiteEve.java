package com.duckcraftian.upf.pe;

public class ParasiteEve {

    public static final String NAMESPACE = "parasite";

    public static final String DISC_1_PROGRAM = "SLUS_006.62";
    public static final String DISC_2_PROGRAM = "SLUS_006.68";

    public static final int ROOM_COUNT = 450;

    public static final long DISC_1_PROG_TBL_RAM = 0x80093378;
    public static final long RESIDENT_TABLE_RAM = 0x800930D8;

    public static final long PLAYER_TEXTURE_WORD = 0x2F200;

    // Sector Counts
    public static final int SCRATCH_SECTOR_COUNT = 33;
    public static final int TEXTURE_SECTOR_COUNT = 74;
    public static final int ROOM_SECTOR_COUNT = 82;

    // SHA1 Hashes
    public static final String SCRATCH_HASH = "c237413c907d3b9f63e1570e7c95e93b45499db2";
    public static final String TEXTURE_HASH = "c4ed11629222c7977df0f15542d9e48af819baa1";
    public static final String ROOM_HASH = "9f90b96a6b9b4c791b0283c14b428f7fd0d4a041";

    // ScriptVM OpCodes

    public static final int EvalExpr = 9;
    public static final int AssignValue = 10;
    public static final int JumpIfZero = 5;
    public static final int JumpToEntityOffset = 0;
    public static final int SetField = 90;
    public static final int SetCurrentNodeActive = 2;
    public static final int MarkCurrentNodeFree = 32;
    public static final int CondBranch = 29;
    public static final int SetCurrentActionMode = 46;
    public static final int PlayFieldSFX = 28;
    public static final int SpawnEntityAt = 8;
    public static final int SetEntityVec3 = 11;
    public static final int SetEntityAnimFrame = 47;
    public static final int SetEntityAnimSpeed = 78;
    public static final int WaitAnimFrameMatch = 48;
    public static final int RoomChange = 49;
    public static final int IsInRectangle = 119;
    public static final int GetPlayerPosition = 94;
    public static final int SetGlobalFlag1 = 64;
    public static final int ClearGlobalFlag1 = 63;
    public static final int SetGlobalFlags = 45;
    public static final int ClearGlobalFlags = 43;
    public static final int GetGlobalFlags = 44;
    public static final int SetSceneFlag2000 = 170;
    public static final int ClearSceneFlag2000 = 171;
    public static final int SetCurrentFlag80 = 30;
    public static final int SetCurrentFlag40 = 65;
    public static final int setCurrentFlag20 = 121;
    public static final int ClearCurrentFlag20 = 120;
    public static final int ClearMotionVectors = 101;
    public static final int SetEntityMoveSpeed = 27;
    public static final int ScreenScrollPos = 132;
    public static final int SelectFieldSetter = 20;
    public static final int GetNodeTarget14 = 31;
    public static final int MarkNodeFree = 4;
    public static final int PlayFieldSfx = 28;
    public static final int FadeSceneToBlack = 133;
    public static final int FadeSceneIn = 134;
    public static final int WaitForFade = 156;
    public static final int ActorFaceActor = 75;
    public static final int SpawnChildNode = 18;
    public static final int ActorWalkToPoint = 68;
    public static final int ActorTurnToPoint = 184;
    public static final int StopEntityAnim = 13;
    public static final int YieldWhileTableEntrySet = 34;
    public static final int SetFogParams = 3;
    public static final int SwapViewports = 130;
    public static final int EntityMarkDead = 1;
    public static final int TextboxOpen = 13;
    public static final int TextboxWait = 34;
    public static final int TextboxClose = 35;
    public static final int FadeSceneToColor = 135;
    public static final int OpenNameEntryMenu = 239;
    public static final int ActorSnapToAnimAnchor = 205;
    public static final int GetMenuChoice = 67;
    public static final int FreezeCamera = 91;
    public static final int UnfreezeCamera = 92;
    public static final int PlayFMV = 53;
    public static final int ClearCurrentFlag80 = 50;
    public static final int ScrollCamera = 3;
    public static final int ScrollCameraAlt = 70;

    public static int mapFromToken(int t) {
        return ((t >> 17) & 31) * 100 + ((t >> 12) & 31) * 10 + ((t >> 7) & 31);
    }

}