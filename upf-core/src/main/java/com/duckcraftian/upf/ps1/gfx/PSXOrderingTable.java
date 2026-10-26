package com.duckcraftian.upf.ps1.gfx;

public class PSXOrderingTable {

    private int length;
    private int[] heads;
    private int[] next;

    public PSXOrderingTable(int length, int capacity) {
        this.length = length;
        heads = new int[length];
        next = new int[capacity];
        clear();
    }

    public void clear() {
        for (int i = 0; i < length; i++)
            heads[i] = -1;
    }

    public void add(int slot, int prim) {
        next[prim] = heads[slot];
        heads[slot] = prim;
    }

    public int getLength() {
        return length;
    }

    public int[] getHeads() {
        return heads;
    }

    public int[] getNext() {
        return next;
    }
}