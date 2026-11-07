package com.duckcraftian.upf.pe.audio;

public class AKAOChannel {

    int pc;
    int noteLength;
    int octave;
    int transpose;
    int instrument;
    int volumeBase;
    int volume;
    int pan;
    int releaseRate;
    int[] loopPcs;
    int[] loopCounts;
    int loopTop;
    int events;
    int gateLength;
    int voice;
    int expressionDuration;
    int expressionDelta;
    int volumeLeft;
    int volumeRight;
    int rangeTable;
    int currentNote;
    int attackOverride;
    int sustainRateOverride;
    int sustainModeOverride;
    int drumTable;
    int drumVolume;
    boolean drumMode;
    boolean rangePending;
    boolean ended;

    public AKAOChannel() {
        loopPcs = new int[4];
        loopCounts = new int[4];

        rangeTable = -1;
        rangePending = false;
        currentNote = 0;
        attackOverride = -1;
        sustainRateOverride = -1;
        sustainModeOverride = -1;

        drumMode = false;
        drumTable = -1;
        drumVolume = 0;
    }

}