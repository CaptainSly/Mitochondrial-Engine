package com.duckcraftian.upf.pe.effects;

import static com.duckcraftian.upf.pe.disc.SquareImageParser.findKeyedAssetTable;

public class EffectAssets {

    public static byte[] startupResourceBlob;
    public static final int[] startupResourceTables = new int[2];

    public static void load(byte[] blob) {
        startupResourceBlob = blob;
        startupResourceTables[0] = findKeyedAssetTable(blob, 0x57D40D84L);
        startupResourceTables[1] = findKeyedAssetTable(blob, 0x57D41D84L);
    }

}