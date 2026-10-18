package com.duckcraftian.upf.assets;

import org.joml.Vector4f;

public record Color(float r, float g, float b, float a) {

    public Vector4f toVector() {
        return new Vector4f(r, g, b, a);
    }

    public static Color fromHex(String hex) {
        if (hex.startsWith("#")) hex = hex.substring(1);

        int alpha = 0;

        if (hex.length() == 6) alpha = 255;
        else if (hex.length() == 8) alpha = Integer.parseInt(hex.substring(6, 8), 16);
        else
            throw new IllegalArgumentException("Bad Hex Color: " + hex);

        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);

        return new Color(r / 255.0f, g / 255.0f, b / 255.0f, alpha / 255.0f);
    }

    public int toPacked() {
        int ri = channelToByte(r);
        int gi = channelToByte(g);
        int bi = channelToByte(b);
        int ai = channelToByte(a);
        return (ai << 24) | (bi << 16) | (gi << 8) | ri;
    }

    private static int channelToByte(float value) {
        float clamped = Math.min(1.0f, Math.max(0.0f, value));
        return Math.round(clamped * 255.0f);
    }

}