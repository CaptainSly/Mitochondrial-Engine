package com.duckcraftian.upf.utils;

public class Input {

    public static enum MouseBtns {
        LEFT, MIDDLE, RIGHT, SIDE1, SIDE2,
    }

    private static byte[] currentKeys = new byte[512];
    private static byte[] previousKeys = new byte[512];

    private static byte[] currentBtns = new byte[10];
    private static byte[] previousBtns = new byte[10];

    public static float mouseDeltaX, mouseDeltaY, mouseX, mouseY, scrollDeltaX, scrollDeltaY;

    public static void update() {
        for (int i = 0; i < currentKeys.length; i++) {
            previousKeys[i] = currentKeys[i];
        }

        for (int i = 0; i < currentBtns.length; i++) {
            previousBtns[i] = currentBtns[i];
        }

        mouseDeltaX = 0;
        mouseDeltaY = 0;
        scrollDeltaX = 0;
        scrollDeltaY = 0;
    }

    public static boolean isKeyDown(int scancode) {
        return currentKeys[scancode] == 1;
    }

    public static boolean isKeyJustDown(int scancode) {
        return currentKeys[scancode] == 1 && previousKeys[scancode] == 0;
    }

    public static boolean isKeyJustUp(int scancode) {
        return currentKeys[scancode] == 0 && previousKeys[scancode] == 1;
    }

    public static boolean isMouseButtonDown(MouseBtns btn) {
        return currentBtns[btn.ordinal()] == 1;
    }

    public static boolean isMouseButtonJustDown(MouseBtns btn) {
        return currentBtns[btn.ordinal()] == 1 && previousBtns[btn.ordinal()] == 0;
    }

    public static boolean isMouseButtonJustUp(MouseBtns btn) {
        return currentBtns[btn.ordinal()] == 0 && previousBtns[btn.ordinal()] == 1;
    }

    public static void onKeyDown(int scancode, boolean down) {
        currentKeys[scancode] = (byte) (down ? 1 : 0);
    }

    public static void onMouseMotion(float dx, float dy, float mx, float my) {
        mouseDeltaX += dx;
        mouseDeltaY += dy;

        mouseX = mx;
        mouseY = my;
    }

    public static void onMouseButtonDown(int button, boolean down) {
        currentBtns[button] = (byte) (down ? 1 : 0);
    }

    public static void onMouseWheel(float x, float y) {
        scrollDeltaX += x;
        scrollDeltaY += y;
    }

    public static byte[] getCurrentKeys() {
        return currentKeys;
    }

    public static byte[] getPreviousKeys() {
        return previousKeys;
    }

    public static float getMouseX() {
        return mouseX;
    }

    public static float getMouseY() {
        return mouseY;
    }

    public static float getMouseDeltaX() {
        return mouseDeltaX;
    }

    public static float getMouseDeltaY() {
        return mouseDeltaY;
    }

}