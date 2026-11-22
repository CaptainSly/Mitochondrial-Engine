package com.duckcraftian.upf.pe.effects;

import com.duckcraftian.upf.ps1.data_types.PSXImage;

import java.util.Arrays;

import static com.duckcraftian.upf.pe.disc.SquareImageParser.findKeyedAssetTable;

public class EffectAssets {

    public static byte[] startupResourceBlob;
    public static final int[] startupResourceTables = new int[2];

    public static int currentEffectPage;
    public static PSXImage pendingPage;

    public static byte[] drawTable, fxConstants, emberColorTrack;

    public static void load(byte[] blob) {
        startupResourceBlob = blob;
        startupResourceTables[0] = findKeyedAssetTable(blob, 0x57D40D84L);
        startupResourceTables[1] = findKeyedAssetTable(blob, 0x57D41D84L);
    }

    public static void uploadEffectPage(int index) {
        int pageAt = startupResourceTables[index];
        byte[] pixels = Arrays.copyOfRange(startupResourceBlob, pageAt, pageAt + 32768);
        pendingPage = new PSXImage(0x380, 0x100, 0x40, 0x100, pixels);
        currentEffectPage = index;
    }

    public static void selectEffectPage(int index) {
        if (currentEffectPage != index) uploadEffectPage(index);
    }

    public static PSXImage takePageUpload() {
        PSXImage image = pendingPage;
        pendingPage = null;
        return image;
    }

}