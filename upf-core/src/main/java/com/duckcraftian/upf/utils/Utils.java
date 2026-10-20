package com.duckcraftian.upf.utils;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HexFormat;

public class Utils {

    public static String toHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    public static String stringMultiplier(String str, int multiplier) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < multiplier; i++) {
            sb.append(str);
        }

        return sb.toString();
    }


    public static float[] convertVec2ArrayToFloatArray(Vector2f[] array) {
        float[] arr = new float[array.length * 2];
        for (int i = 0; i < array.length; i++) {
            int offset = i * 2;
            arr[offset] = array[i].x;
            arr[offset + 1] = array[i].y;
        }

        return arr;
    }

    public static float[] convertVec3ArrayToFloatArray(Vector3f[] array) {
        float[] arr = new float[array.length * 3];
        for (int i = 0; i < array.length; i++) {
            int offset = i * 3;
            arr[offset] = array[i].x;
            arr[offset + 1] = array[i].y;
            arr[offset + 2] = array[i].z;
        }

        return arr;
    }

    public static float[] convertVec4ArrayToFloatArray(Vector4f[] array) {
        float[] arr = new float[array.length * 4];
        for (int i = 0; i < array.length; i++) {
            int offset = i * 4;
            arr[offset] = array[i].x;
            arr[offset + 1] = array[i].y;
            arr[offset + 2] = array[i].z;
            arr[offset + 3] = array[i].w;
        }

        return arr;
    }
}