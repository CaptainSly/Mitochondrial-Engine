package com.duckcraftian.upf.system;

import org.joml.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class Utils {

    public static String hash(byte[] data) throws NoSuchAlgorithmException {
        var md = MessageDigest.getInstance("SHA-1");
        return toHex(md.digest(data));
    }

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

    public static String intArrayToString(int[] arr) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < arr.length; i++) {
            String prepender = "";
            String appender = "";
            if (i < arr.length - 1)
                appender = ", ";
            else if (i == arr.length - 1)
                appender = " ]";

            if (i == 0)
                prepender = "[ ";
            else
                prepender = "";

            int num = arr[i];
            sb.append(prepender + num + appender);
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

    public static int[] convertVec2ArrayToIntArray(Vector2i[] array) {
        int[] arr = new int[array.length * 2];
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

    public static int[] convertVec3ArrayToIntArray(Vector3i[] array) {
        int[] arr = new int[array.length * 3];
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