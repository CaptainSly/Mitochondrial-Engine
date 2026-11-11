package com.duckcraftian.upf.pe;

import com.duckcraftian.upf.pe.audio.PEAudio;

public class ParasiteEve {

    public static final String NAMESPACE = "parasite";

    public static PEAudio PEAUDIO;

    public static final String DISC_1_PROGRAM = "SLUS_006.62";
    public static final String DISC_2_PROGRAM = "SLUS_006.68";

    public static final int ROOM_COUNT = 450;

    public static int STAND_IN_ACTION_SET = 1;

    // Ram Locations
    public static final long DISC_1_PROG_TBL_RAM = 0x80093378;
    public static final long RESIDENT_TABLE_RAM = 0x800930D8;
    public static final long NORMAL_TABLE_RAM = 0x80091A58;
    public static final long SOUND_DIR_RAM = 0x8009317C;
    public static final long AKAO_LENGTHS_RAM = 0x8009B85C;
    public static final long AKAO_NOTE_LENGTHS_RAM = 0x8009B8DC;
    public static final long AKAO_PAN_TABLE_RAM = 0x8009B8F8;
    public static final long SCENE_OBJECT_MODELKEY = 0xC4B5BA04L;

    public static final long NORMAL_TABLE_BYTES = 240 * 8;
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
    public static final int JumpToEntityOffset = 0;
    public static final int EntityMarkDead = 1;
    public static final int SetCurrentNodeActive = 2;
    public static final int ScrollCamera = 3;
    public static final int MarkNodeFree = 4;
    public static final int JumpIfZero = 5;
    public static final int SpawnEntityAt = 8;
    public static final int EvalExpr = 9;
    public static final int AssignValue = 10;
    public static final int SetEntityVec3 = 11;
    public static final int GetEntityVec3 = 12;
    public static final int TextboxOpen = 13;
    public static final int QueryField = 14;
    public static final int IsButtonDown = 17;
    public static final int SpawnChildNode = 18;
    public static final int SelectFieldSetter = 20;
    public static final int YieldWhileMoving = 22;
    public static final int ComputeVelocityToTarget = 23;
    public static final int StopScriptById = 24;
    public static final int GetCurrentScriptId = 25;
    public static final int Random = 26;
    public static final int SetEntityMoveSpeed = 27;
    public static final int PlayFieldSFX = 28;
    public static final int JumpIfNotEqual = 29;
    public static final int SetCurrentFlag80 = 30;
    public static final int GetNodeTarget14 = 31;
    public static final int MarkCurrentNodeFree = 32;
    public static final int TextboxWait = 34;
    public static final int TextboxClose = 35;
    public static final int GetTouchingActor = 36;
    public static final int ClearBit = 40;
    public static final int SetBit = 42;
    public static final int ClearGlobalFlags = 43;
    public static final int GetGlobalFlags = 44;
    public static final int SetGlobalFlags = 45;
    public static final int SetCurrentActionMode = 46;
    public static final int SetEntityAnimFrame = 47;
    public static final int WaitAnimFrameMatch = 48;
    public static final int RoomChange = 49;
    public static final int ClearCurrentFlag80 = 50;
    public static final int PlayFMV = 53;
    public static final int SetHitRadius = 62;
    public static final int ClearGlobalFlag1 = 63;
    public static final int SetGlobalFlag1 = 64;
    public static final int SetCurrentFlag40 = 65;
    public static final int GetMenuChoice = 67;
    public static final int ActorWalkToPoint = 68;
    public static final int GetAngleToEntity = 69;
    public static final int ScrollCameraAlt = 70;
    public static final int ActorFaceActor = 75;
    public static final int SetAnimationSpeed = 77;
    public static final int SetEntityAnimSpeed = 78;
    public static final int TestGameStateFlags = 81;
    public static final int SetGameStateFlags = 82;
    public static final int ClearGameStateFlags = 83;
    public static final int GetDistanceComponents = 84;
    public static final int InitiateBattle = 85;
    public static final int GetField = 89;
    public static final int SetField = 90;
    public static final int FreezeCamera = 91;
    public static final int UnfreezeCamera = 92;
    public static final int SetEntityMoveFactor = 93;
    public static final int GetActorVector = 94;
    public static final int RunEntityCommand = 100;
    public static final int ClearMotionVectors = 101;
    public static final int StartEffectProcess = 106;
    public static final int SendEffectProcessCommand = 107;
    public static final int MakeCombatant = 111;
    public static final int SetAttackEffectFull = 112;
    public static final int SetBackgroundLayerPosition = 113;
    public static final int SetRenderEntryEnabled = 114;
    public static final int SetBackgroundAnimFrame = 116;
    public static final int SetBackgroundAnimRunning = 117;
    public static final int IsInRectangle = 119;
    public static final int ClearCurrentFlag20 = 120;
    public static final int setCurrentFlag20 = 121;
    public static final int GetBackgroundAnimFrame = 122;
    public static final int SetBackgroundAnimStep = 123;
    public static final int SetBackgroundAnimFlags = 126;
    public static final int SetBackgroundAnimFlagsInverted = 127;
    public static final int BlockCollisionTriangle = 128;
    public static final int UnblockCollisionTriangle = 129;
    public static final int SwapViewports = 130;
    public static final int ScreenScrollPos = 132;
    public static final int FadeSceneToBlack = 133;
    public static final int FadeSceneIn = 134;
    public static final int FadeSceneToColor = 135;
    public static final int ClearEntityFlag40 = 136;
    public static final int SetBattleModeState6 = 137;
    public static final int FindByTypeId = 139;
    public static final int WaitForSequenceFrame = 140;
    public static final int WaitWhileSequenceRunning = 143;
    public static final int Noop145 = 145;
    public static final int GetBattleModeState = 148;
    public static final int ClearBattleModeState = 149;
    public static final int PlayBackgroundAnimFromTo = 154;
    public static final int DrawActorLit = 155;
    public static final int WaitForFade = 156;
    public static final int ScaleAnimValue = 157;
    public static final int SetSceneFlag2000 = 170;
    public static final int ClearSceneFlag2000 = 171;
    public static final int SetWallPolygon = 172;
    public static final int ClearInputFlagBit2 = 173;
    public static final int SetMoveLockBit2 = 174;
    public static final int SetAttackEffect = 183;
    public static final int ActorTurnToPoint = 184;
    public static final int WaitForAnimationLoop = 195;
    public static final int SetActionAndWait = 198;
    public static final int SetNodeFlag80 = 199;
    public static final int SelectBoundsEntry = 202;
    public static final int GetAngleToPoint = 203;
    public static final int ActorSnapToAnimAnchor = 205;
    public static final int SetCombatantOrActorField = 206;
    public static final int SetEscapeFlag = 207;
    public static final int SetEntityParent = 212;
    public static final int WalkStraightToPoint = 219;
    public static final int SetEntityFlag1000000 = 220;
    public static final int EnableActorGravity = 223;
    public static final int DisableActorGravity = 224;
    public static final int SoundCommand = 234;
    public static final int SetBackgroundDimLevel = 225;
    public static final int SetFloorPolygon = 227;
    public static final int DispatchScriptCmd = 237;
    public static final int OpenNameEntryMenu = 239;

    public static int mapFromToken(int t) {
        return ((t >> 17) & 31) * 100 + ((t >> 12) & 31) * 10 + ((t >> 7) & 31);
    }

    public static String glyph(int b) {
        if (b <= 0x09) return String.valueOf(b);
        if (b == 0x0F) return " ";
        if (b >= 0x10 && b <= 0x29) return String.valueOf((char) ('A' + (b - 0x10)));
        if (b >= 0x30 && b <= 0x49) return String.valueOf((char) ('a' + (b - 0x30)));
        if (b == 0x2A) return "&";
        if (b == 0x2B) return "!";
        if (b == 0x2C) return "?";
        if (b == 0x2D) return "\"";
        if (b == 0x2E) return "'";
        if (b == 0x2F) return ".";
        if (b == 0x4A) return ":";
        if (b == 0x4B) return ",";
        return "";
    }

    public static int code(String s) {
        if (s == null || s.length() != 1) return -1;
        char c = s.charAt(0);

        if (c >= '0' && c <= '9') return c - '0';          // 0x00-0x09
        if (c >= 'A' && c <= 'Z') return 0x10 + (c - 'A'); // 0x10-0x29
        if (c >= 'a' && c <= 'z') return 0x30 + (c - 'a'); // 0x30-0x49

        switch (c) {
            case ' ':
                return 0x0F;
            case '&':
                return 0x2A;
            case '!':
                return 0x2B;
            case '?':
                return 0x2C;
            case '"':
                return 0x2D;
            case '\'':
                return 0x2E;
            case '.':
                return 0x2F;
            case ':':
                return 0x4A;
            case ',':
                return 0x4B;
            default:
                return -1; // no mapping
        }
    }


}