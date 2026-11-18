package com.duckcraftian.upf.pe;

import com.duckcraftian.upf.pe.gfx.PEHud;
import com.duckcraftian.upf.pe.script.ScriptVM;
import com.duckcraftian.upf.ps1.PSX;
import org.tinylog.Logger;

import static com.duckcraftian.upf.system.DataTypeHelper.*;

public class AyaStats {

    public static int maxHp, curHp, offense, defense, maxAtk;
    public static int peRate, peStep, peAcc, peReadyFlash, peTicks;
    public static int atbGauge, atbTicks, atbRechargeRate, atbPerTick;

    public static int stateFlags, level;

    public static int actionPowerBonus, actionRange, actionId, actionAttackWord, actionTurnWord;
    public static int hitTrigger;

    public static int[] animMode = new int[4];
    public static byte[] levelStats, dataPack, weaponEffectCodes, actionAnimRows, actionOffenseScale;

    public static final int ITEM_TABLE_OFFSET = 0x35E0;
    public static final int ITEM_RECORD_SIZE = 0x20;

    // TODO STAND IN
    public static final int STARTING_WEAPON_ID = 0x44;

    public static void recalc() {
        int row = level * 24;
        maxHp = u16LE(levelStats, row + 0);

        if (curHp == 0) curHp = maxHp;
        if (curHp > maxHp) curHp = maxHp;

        offense = u16LE(levelStats, row + 2);
        defense = u16LE(levelStats, row + 4);
        atbRechargeRate = u8(levelStats, row + 6);
        maxAtk = (int) u32LE(levelStats, row + 8);
        peRate = (int) u32LE(levelStats, row + 12);
        peStep = (int) u32LE(levelStats, row + 16);
    }

    public static void buildWeaponAction() {
        int row = ITEM_TABLE_OFFSET + (STARTING_WEAPON_ID - 1) * ITEM_RECORD_SIZE;
        int kind = u8(dataPack, row + 6);
        int attack = Math.min(u8(dataPack, row + 7) + s16LE(dataPack, row + 0x0E), 999);
        int rangeStat = Math.min(u8(dataPack, row + 8) + s16LE(dataPack, row + 0x10), 999);
        int power = Math.min(u8(dataPack, row + 9) + s16LE(dataPack, row + 0x12), 999);
        int ammo = u16LE(dataPack, row + 0x0A);
        int tailCount = u8(dataPack, row + 0x14);

        int category = 0;

        if (kind >= 1 && kind <= 7) {
            category = kind - 4;
            if (category <= 0) category = 1;
        } else if (kind >= 19) category = kind - 18;

        actionPowerBonus = attack;
        actionRange = (int) Math.sqrt(rangeStat * 22500);
        actionId = kind;
        actionAttackWord = (ammo & 0x3FF) | ((power & 0x3FF) << 10) | ((category & 3) << 20);

        int turn = getTurn(tailCount, row);

        actionTurnWord = turn;

        int rowAt = actionId * 5;
        for (int i = 0; i < 4; i++)
            animMode[i] = u8(actionAnimRows, rowAt + i);

        hitTrigger = u8(actionAnimRows, rowAt + 4);
    }

    private static int getTurn(int tailCount, int row) {
        int turn = 0x11;
        for (int i = 0; i < tailCount; i++) {
            int effect = u8(dataPack, row + 0x15 + i) & 31;
            if (effect >= 1 && effect <= 5) turn = (turn & ~0x5) | u8(weaponEffectCodes, effect);
            else if (effect >= 6 && effect <= 8) turn = (turn & ~0xC0) | ((effect - 5) << 6);
            else if (effect >= 9 && effect <= 13) turn = turn | (1 << (effect - 1));
            else if (effect == 14) turn = turn | (1 << 15);
            else if (effect == 15) turn = turn | (1 << 16);
            else if (effect == 16 || effect == 17) turn = (turn & ~0x6000) | ((effect - 15) << 13);
            else if (effect == 18) turn = turn | (1 << 17);
            else if (effect == 19 || effect == 20) turn = (turn & ~0x30) | ((effect - 17) << 4);
        }
        return turn;
    }

    public static void battleStartReset() {
        PSX.srand(ScriptVM.debugTick);
        buildWeaponAction();
        peTicks = 0;
        atbGauge = 0;
        atbTicks = 0;
        peReadyFlash = 0;
        atbPerTick = (atbRechargeRate * u8(actionOffenseScale, actionId)) / 10;
        ScriptVM.battleFrame = 0;
        ScriptVM.battleEscapeFlag = 0;
        PEHud.resetGaugeColors();

        if (peAcc <= 0) peAcc = 0x10000;
        else if (peAcc >= maxAtk) {
            peAcc = maxAtk;
            peReadyFlash = 0xF0;
        }

        Logger.debug("Aya Stats | HP: " + curHp + "/" + maxHp + ", Offense: " + offense + ", Defense: " + defense + ", Max Attack: " + maxAtk + ", ATB Rate: " + atbRechargeRate + ", ATB Per Tick: " + atbPerTick + ", ATB Gauge: " + atbGauge);
        Logger.debug("Action Stats | Power Bonus: " + actionPowerBonus + ", Range: " + actionRange + ", Id: " + actionId + ", Attack Word: " + actionAttackWord + ", Turn Word: " + actionTurnWord);
    }

    public static void tickATB() {
        if ((ScriptVM.gameStateFlags & 0x100) == 0) {
            atbGauge += atbPerTick;
            int group = stateFlags & 0xC0;
            if (group == 0x40 || group == 0x80) atbGauge -= (atbPerTick * 2) / 5;
            else if ((stateFlags & 0x100) != 0) atbGauge += (atbPerTick & 0xFFFF) >> 1;

            atbGauge &= 0xFFFF;
            atbTicks += 1;
        }
    }

    public static void tickPE() {
        if ((ScriptVM.gameStateFlags & 0x100) == 0) {
            if (peAcc < maxAtk && (stateFlags & 0x2600) == 0) {
                peRate -= 3;
                if (peRate <= 0) peRate = 1;
                peStep = peStep - maxAtk / (peRate * 100);
                if (peStep < 0x1999) peStep = 0x1999;
                peAcc += peStep;
                peTicks += 1;

                if (peAcc >= maxAtk) {
                    peReadyFlash = 0xF0;
                    ParasiteEve.PEAUDIO.playCommonSfx(0x455, 0, 0x80, 0x7F);
                    Logger.debug("PE Ticks: " + peTicks + ", PE Rate: " + peRate + ", PE Step: " + peStep + ", ATB ACC: " + peAcc);
                }
            }

            if (peReadyFlash > 0) peReadyFlash -= 1;
        }

        if (peAcc < 0) peAcc = 0;
    }

}