package com.duckcraftian.upf.pe;

import com.duckcraftian.upf.pe.script.ScriptVM;
import com.duckcraftian.upf.ps1.PSX;
import org.tinylog.Logger;

import static com.duckcraftian.upf.system.DataTypeHelper.*;

public class AyaStats {

    public static int maxHp, curHp, offense, defense, maxAtk;
    public static int atbRate, atbStep, atbAcc, atbGauge, atbTicks;
    public static int actionPowerBonus, actionRange, actionId, actionAttackWord, actionTurnWord;
    public static int hitTrigger;

    public static int[] animMode = new int[4];
    public static byte[] levelStats, dataPack, weaponEffectCodes, actionAnimRows;

    public static final int ITEM_TABLE_OFFSET = 0x35E0;
    public static final int ITEM_RECORD_SIZE = 0x20;

    // TODO STAND IN
    public static final int STARTING_WEAPON_ID = 0x44;

    public static void recalc() {
        int level = 0;
        int row = level * 24;
        maxHp = u16LE(levelStats, row + 0);

        if (curHp == 0) curHp = maxHp;
        if (curHp > maxHp) curHp = maxHp;

        offense = u16LE(levelStats, row + 2);
        defense = u16LE(levelStats, row + 4);
        maxAtk = (int) u32LE(levelStats, row + 8);
        atbRate = (int) u32LE(levelStats, row + 12);
        atbStep = (int) u32LE(levelStats, row + 16);
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
        atbTicks = 0;

        if (atbAcc <= 0) atbAcc = 0x10000;
        else if (atbAcc >= maxAtk) {
            atbAcc = maxAtk;
            atbGauge = 0xF0;
        }

        Logger.debug("Aya Stats | HP: " + curHp + "/" + maxHp + ", Offense: " + offense + ", Defense: " + defense + ", Max Attack: " + maxAtk + ", ATB Rate: " + atbRate + ", ATB Step: " + atbStep + ", ATB ACC: " + atbAcc + ", ATB Gauge: " + atbGauge);
        Logger.debug("Action Stats | Power Bonus: " + actionPowerBonus + ", Range: " + actionRange + ", Id: " + actionId + ", Attack Word: " + actionAttackWord + ", Turn Word: " + actionTurnWord);
    }

    public static void tickATB() {
        if ((ScriptVM.gameStateFlags & 0x100) == 0) {
            if (atbAcc < maxAtk) {
                atbRate -= 3;
                if (atbRate <= 0) atbRate = 1;
                atbStep = atbStep - maxAtk / (atbRate * 100);
                if (atbStep < 0x1999) atbStep = 0x1999;
                atbAcc += atbStep;
                atbTicks += 1;

                if (atbAcc >= maxAtk) {
                    atbGauge = 0xF0;
                    Logger.debug("ATB Ticks: " + atbTicks + ", ATB Rate: " + atbRate + ", ATB Step: " + atbStep + ", ATB ACC: " + atbAcc);
                }
            }

            if (atbGauge > 0) atbGauge -= 1;
        }

        if (atbAcc < 0) atbAcc = 0;
    }

}