package com.duckcraftian.upf.pe.gfx;

import com.duckcraftian.upf.ps1.PSX;
import com.duckcraftian.upf.ps1.PSXMath;
import com.duckcraftian.upf.ps1.gfx.PSXOrderingTable;
import com.duckcraftian.upf.system.Utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class PEModel {

    private int partCount;
    private int vertexCount;
    private int n34;
    private int n28;
    private int n24;
    private int n1c;
    private int byteSize;
    private int AnchorY;
    private int anchorMatrixIndex;
    private int hitRadius;


    private int[] partVertexStart;
    private int[] partVertexCount;
    private int[] partVisible;
    private int[] vertices;
    private int[] colors;
    private int[] primKind;
    private int[] primVertex;
    private int[] primUV;
    private int[] primClut;
    private int[] primPageBits;
    private int[] partTranslationZ;
    private int[] matrixCommands;

    public PEModel(int partCount, int vertexCount, int primCount) {
        this.partCount = partCount;
        this.vertexCount = vertexCount;

        partVertexStart = new int[partCount];
        partVertexCount = new int[partCount];
        partVisible = new int[partCount];
        vertices = new int[vertexCount * 3];
        colors = new int[vertexCount];
        primKind = new int[primCount];
        primClut = new int[primCount];
        primPageBits = new int[primCount];
        primVertex = new int[primCount * 4];
        primUV = new int[primCount * 8];
    }

    public int[][] pose(PEAnimation anim, int frame, int[] worldRot, int[] worldTrans) {
        int[][] result = new int[partCount][12];
        int[] curRot = worldRot;
        int[] curTrans = worldTrans;
        ArrayDeque<int[]> stack = new ArrayDeque<>();

        for (int cmd : matrixCommands) {
            if (cmd == -1)
                stack.push(Utils.combineIntArray(curRot, curTrans));
            else if (cmd == -2) {
                int[][] parts = Utils.splitIntArray(stack.pop(), 9);
                curRot = parts[0];
                curTrans = parts[1];
            } else {
                int ax = anim.channels()[3 + 3 * cmd][frame];
                int ay = anim.channels()[3 + 3 * cmd + 1][frame];
                int az = anim.channels()[3 + 3 * cmd + 2][frame];
                int[] rot = PSXMath.mulMatrix(curRot, PSXMath.rotMatrixYXZ(ax, ay, az));

                int offX = 0;
                int offY = 0;
                int offZ = partTranslationZ[cmd];

                if (cmd == 0) {
                    offX = anim.channels()[0][frame];
                    offY = anim.channels()[1][frame];
                    offZ = anim.channels()[2][frame];
                }
                int[] t = PSXMath.mulVector(curRot, offX, offY, offZ);

                int[] trans = new int[3];
                trans[0] = t[0] + curTrans[0];
                trans[1] = t[1] + curTrans[1];
                trans[2] = t[2] + curTrans[2];

                for (int i = 0; i < rot.length; i++)
                    result[cmd][i] = rot[i];

                for (int i = 0; i < trans.length; i++)
                    result[cmd][rot.length + i] = trans[i];

                curRot = rot;
                curTrans = trans;
            }
        }

        return result;
    }

    public int[] project(int[][] pose, int[] viewRot, int[] viewTrans) {
        int[] screen = new int[vertexCount * 3];

        for (int p = 0; p < partCount; p++) {
            int[] boneRot = new int[9];
            int[] boneTrans = new int[3];

            for (int i = 0; i < boneRot.length; i++) {
                boneRot[i] = pose[p][i];
            }

            for (int i = 0; i < boneTrans.length; i++) {
                int idx = boneRot.length;
                boneTrans[i] = pose[p][idx + i];
            }

            int[] rot = PSXMath.mulMatrix(viewRot, boneRot);
            int[] t = PSXMath.mulVector(viewRot, boneTrans[0], boneTrans[1], boneTrans[2]);

            t[0] = t[0] + viewTrans[0];
            t[1] = t[1] + viewTrans[1];
            t[2] = t[2] + viewTrans[2];
            PSX.GTE.loadRotation(rot);
            PSX.GTE.loadTranslation(t);

            int first = partVertexStart[p];
            int last = partVertexStart[p] + partVertexCount[p] - 1;

            for (int v = first; v <= last; v = v + 3) {
                int v1 = Math.min(v + 1, last);
                int v2 = Math.min(v + 2, last);

                PSX.GTE.writeData(0, (vertices[3 * v + 1] << 16) | (vertices[3 * v] & 0xFFFF));
                PSX.GTE.writeData(1, vertices[3 * v + 2]);
                PSX.GTE.writeData(2, (vertices[3 * v1 + 1] << 16) | (vertices[3 * v1] & 0xFFFF));
                PSX.GTE.writeData(3, vertices[3 * v1 + 2]);
                PSX.GTE.writeData(4, (vertices[3 * v2 + 1] << 16) | (vertices[3 * v2] & 0xFFFF));
                PSX.GTE.writeData(5, vertices[3 * v2 + 2]);
                PSX.GTE.execute(PSX.CMD_RTPT);

                for (int k = 0; k < 3; k++) {
                    int index = Math.min(v + k, last);
                    int sxy = PSX.GTE.readData(12 + k);
                    screen[3 * index] = (short) sxy;
                    screen[3 * index + 1] = (short) (sxy >> 16);
                    screen[3 * index + 2] = PSX.GTE.readData(17 + k);
                }

            }
        }

        return screen;
    }

    public void sortIntoOT(int[] screen, PSXOrderingTable ot) {
        for (int i = 0; i < n34 + n28 + n24 + n1c; i++) {
            boolean isQuad = (i < n34) || (i >= n34 + n28 && i < n34 + n28 + n24);
            int a = primVertex[4 * i];
            int b = primVertex[4 * i + 1];
            int c = primVertex[4 * i + 2];
            int d = primVertex[4 * i + 3];

            PSX.GTE.writeData(12, (screen[3 * a + 1] << 16) | (screen[3 * a] & 0xFFFF));
            PSX.GTE.writeData(13, (screen[3 * b + 1] << 16) | (screen[3 * b] & 0xFFFF));
            PSX.GTE.writeData(14, (screen[3 * c + 1] << 16) | (screen[3 * c] & 0xFFFF));
            PSX.GTE.execute(PSX.CMD_NCLIP);

            int winding = PSX.GTE.readData(24);
            if (winding < 0)
                continue;

            PSX.GTE.writeData(17, screen[3 * a + 2]);
            PSX.GTE.writeData(18, screen[3 * b + 2]);
            PSX.GTE.writeData(19, screen[3 * c + 2]);
            if (isQuad) {
                PSX.GTE.writeData(16, screen[3 * a + 2]);
                PSX.GTE.writeData(17, screen[3 * b + 2]);
                PSX.GTE.writeData(18, screen[3 * c + 2]);
                PSX.GTE.writeData(19, screen[3 * d + 2]);
                PSX.GTE.execute(PSX.CMD_AVSZ4);
            } else PSX.GTE.execute(PSX.CMD_AVSZ3);

            int slot = Math.min(PSX.GTE.readData(7), ot.getLength() - 1);
            ot.add(slot, i);
        }

    }

    public int[] buildTriangles(PSXOrderingTable ot, int[] screen, List<Integer> semiOut) {
        ArrayList<Integer> out = new ArrayList<>();

        for (int s = ot.getLength() - 1; s >= 0; s--) {
            for (int p = ot.getHeads()[s]; p != -1; p = ot.getNext()[p]) {
                boolean isQuad = (p < n34) || (p >= n34 + n28 && p < n34 + n28 + n24);
                int[] corner = {primVertex[4 * p], primVertex[4 * p + 1], primVertex[4 * p + 2], primVertex[4 * p + 3]};

                int kind = primKind[p];
                boolean isSemi = (kind == 11 || kind == 16 || kind == 21 || kind == 26);
                List<Integer> target = isSemi ? semiOut : out;

                emit((ArrayList<Integer>) target, p, corner, 0, 1, 2, screen, s);
                if (isQuad) {
                    emit((ArrayList<Integer>) target, p, corner, 1, 3, 2, screen, s);
                }

            }
        }

        int[] result = new int[out.size()];
        for (int i = 0; i < out.size(); i++)
            result[i] = out.get(i);

        return result;
    }

    public void emit(ArrayList<Integer> out, int p, int[] corner, int i, int j, int k, int[] screen, int s) {
        int[] arr = {i, j, k};
        boolean textured = p < n34 + n28;
        for (int c : arr) {
            int v = corner[c];
            out.add(screen[3 * v]);
            out.add(screen[3 * v + 1]);
            out.add(primUV[8 * p + 2 * c]);
            out.add(primUV[8 * p + 2 * c + 1]);
            out.add(textured ? primPageBits[p] : -1);
            out.add(textured ? primClut[p] : 0);
            out.add(s);
            out.add(textured ? 0 : colors[v] & 0xFFFFFF);
        }
    }

    public int[] getPartVisible() {
        return partVisible;
    }

    public int getPartVertexCount(int idx) {
        return partVertexCount[idx];
    }

    public int[] getMatrixCommands() {
        return matrixCommands;
    }

    public void setMatrixCommands(int[] matrixCommands) {
        this.matrixCommands = matrixCommands;
    }

    public int[] getPartTranslationZ() {
        return partTranslationZ;
    }

    public void setPartTranslationZ(int[] partTranslationZ) {
        this.partTranslationZ = partTranslationZ;
    }

    public int getPartCount() {
        return partCount;
    }

    public int getAnchorY() {
        return AnchorY;
    }

    public void setAnchorY(int anchorY) {
        AnchorY = anchorY;
    }

    public int getAnchorMatrixIndex() {
        return anchorMatrixIndex;
    }

    public void setAnchorMatrixIndex(int anchorMatrixIndex) {
        this.anchorMatrixIndex = anchorMatrixIndex;
    }

    public void setPartCount(int partCount) {
        this.partCount = partCount;
    }

    public void setVertexCount(int vertexCount) {
        this.vertexCount = vertexCount;
    }

    public void setN34(int n34) {
        this.n34 = n34;
    }

    public void setN28(int n28) {
        this.n28 = n28;
    }

    public void setN24(int n24) {
        this.n24 = n24;
    }

    public void setN1c(int n1c) {
        this.n1c = n1c;
    }

    public void setByteSize(int byteSize) {
        this.byteSize = byteSize;
    }

    public int getN34() {
        return n34;
    }

    public int getN28() {
        return n28;
    }

    public int getN24() {
        return n24;
    }

    public int getN1c() {
        return n1c;
    }

    public int getByteSize() {
        return byteSize;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getHitRadius() {
        return hitRadius;
    }

    public void setHitRadius(int hitRadius) {
        this.hitRadius = hitRadius;
    }

    public int[] getPartVertexStart() {
        return partVertexStart;
    }

    public int[] getPartVertexCount() {
        return partVertexCount;
    }

    public int[] getVertices() {
        return vertices;
    }

    public int[] getColors() {
        return colors;
    }

    public int[] getPrimKind() {
        return primKind;
    }

    public int[] getPrimVertex() {
        return primVertex;
    }

    public int[] getPrimUV() {
        return primUV;
    }

    public int[] getPrimClut() {
        return primClut;
    }

    public int[] getPrimPageBits() {
        return primPageBits;
    }
}