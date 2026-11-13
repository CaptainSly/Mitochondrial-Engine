package com.duckcraftian.upf.pe;

import com.duckcraftian.upf.pe.script.ScriptVM;
import org.tinylog.Logger;

import static com.duckcraftian.upf.system.DataTypeHelper.u16LE;
import static com.duckcraftian.upf.system.DataTypeHelper.u32LE;

public class AyaStats {

    public static int maxHp, curHp, offense, defense, maxAtk;
    public static int atbRate, atbStep, atbAcc, atbGauge, atbTicks;
    public static byte[] levelStats;

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

    public static void battleStartReset() {
        if (atbAcc <= 0) atbAcc = 0x10000;
        else if (atbAcc >= maxAtk) {
            atbAcc = maxAtk;
            atbGauge = 0xF0;
        }

        Logger.debug("Aya Stats | HP: " + curHp + "/" + maxHp + ", Offense: " + offense + ", Defense: " + defense + ", Max Attack: " + maxAtk + ", ATB Rate: " + atbRate + ", ATB Step: " + atbStep + ", ATB ACC: " + atbAcc + ", ATB Gauge: " + atbGauge);
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