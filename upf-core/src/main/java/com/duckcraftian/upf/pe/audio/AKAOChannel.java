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
    int tag;
    int handle;
    int streamVolume;
    int basePeriod;
    int noteUsed;
    int detune;
    int slideAcc;
    int slideStep;
    int slideSteps;
    int slideDuration;
    int slideDelta;
    int lfoDelay, lfoDuration, lfoSelector;
    int lfoCounter, lfoPhase;
    int lfoTarget, lfoValue, lfoDepth, lfoPos;
    int lfoSlideSteps, lfoSlideDelta;
    int vlfoTarget, vlfoDelay, vlfoDuration, vlfoSelector;
    int vlfoCounter, vlfoPhase, vlfoPos, vlfoValue;
    int panSlideSteps, panSlideDelta;
    boolean vlfoOn;
    boolean lfoOn;
    boolean reverb;
    boolean drumMode;
    boolean rangePending;
    boolean ended;
    boolean fm;
    boolean pairSecond;

    public AKAOChannel() {
        loopPcs = new int[4];
        loopCounts = new int[4];

        rangeTable = -1;
        rangePending = false;
        currentNote = 0;
        attackOverride = -1;
        sustainRateOverride = -1;
        sustainModeOverride = -1;

        detune = 0;

        reverb = false;
        fm = false;
        pairSecond = false;

        lfoSlideSteps = 0;
        lfoSlideDelta = 0;

        panSlideSteps = 0;
        panSlideDelta = 0;

        tag = 0;
        handle = 0;
        streamVolume = 127;

        drumMode = false;
        drumTable = -1;
        drumVolume = 0;
    }

}