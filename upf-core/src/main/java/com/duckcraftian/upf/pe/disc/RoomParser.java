package com.duckcraftian.upf.pe.disc;

import com.duckcraftian.upf.pe.gfx.BackgroundAnimation;
import com.duckcraftian.upf.pe.gfx.PEAnimation;
import com.duckcraftian.upf.pe.gfx.PEModel;
import com.duckcraftian.upf.pe.rooms.BackgroundTile;
import com.duckcraftian.upf.pe.rooms.Collision;
import com.duckcraftian.upf.pe.rooms.PSXViewport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.duckcraftian.upf.system.DataTypeHelper.*;

public class RoomParser {

    public static byte[] readBackgroundBlock(byte[] roomContainer) {
        int directoryOffset = (int) u32LE(roomContainer, 4);
        long field = u32LE(roomContainer, directoryOffset + 0x1C);
        int tableOffset = (int) field & 0x3FFFFF;

        long word = u32LE(roomContainer, tableOffset + 4);
        int blockOffset = (int) (word & 0xFFFFFF);
        int blockSize = u16LE(roomContainer, blockOffset);

        byte[] backgroundBlock = new byte[blockSize];
        System.arraycopy(roomContainer, blockOffset, backgroundBlock, 0, blockSize);

        return backgroundBlock;
    }

    public static int readMessageBank(byte[] container) {
        int dir = (int) u32LE(container, 4);
        int field = (int) u32LE(container, dir + 0x20);
        int table = field & 0x3FFFFF;
        return (int) u32LE(container, table + 8 + 4) & 0xFFFFFF;
    }

    public static List<BackgroundTile> parseBackgroundBlock(byte[] block, int group) {
        List<BackgroundTile> tiles = new ArrayList<>();

        int field26 = u16LE(block, 0x26);
        int layerCount = u16LE(block, 0x06);
        int entryOffset = s32LE(block, 0x14);

        int outX = s16LE(block, 0x38);
        int outY = s16LE(block, 0x3A);


        for (int l = 0; l < layerCount; l++) {
            int entry = entryOffset + l * 0x38;
            int layerGroup = u8(block, entry + 0x24);

            if (layerGroup != group)
                continue;

            if ((BackgroundAnimation.entryFlags[l] & 2) == 0) continue;


            int layerDepth = BackgroundAnimation.entryDepth[l];
            int tileCount = u16LE(block, entry + 0x26);
            int posStart = entry + s32LE(block, entry + 0x28);
            int uvStart = entry + s32LE(block, entry + 0x2C);
            int scrX = BackgroundAnimation.entryScrX[l];
            int scrY = BackgroundAnimation.entryScrY[l];

            for (int t = 0; t < tileCount; t++) {
                long posWord = u32LE(block, posStart + t * 4);

                int x = (int) (posWord >> 22) + scrX + outX;
                int y = (int) ((posWord >> 12) & 0x3FF) + scrY + outY;
                int tileDepth = (int) (posWord & 0xFFF);

                long w0 = u32LE(block, uvStart + t * 8);
                long w1 = u32LE(block, uvStart + t * 8 + 4);

                int clutY = (int) (w0 & 511);
                int clutX = (int) ((w0 >> 5) & 0x3F0);
                int pageX = (int) ((w0 >> 10) & 0x3C0);
                int pageY = (int) ((w0 >> 7) & 256);
                int u = (int) (w1 & 0xFF);
                int v = (int) ((w0 >> 24) & 0xFF);
                int orderIndex = field26 + layerDepth + tileDepth;


                tiles.add(new BackgroundTile(x, y, clutX, clutY, pageX, pageY, u, v, orderIndex));
            }

        }

        tiles.sort(Comparator.comparingInt(BackgroundTile::orderIndex).reversed());
        return tiles;
    }

    public static int[] readScriptStreams(byte[] container) {
        int directoryOffset = (int) u32LE(container, 4);
        int field = (int) u32LE(container, directoryOffset + 0x14);
        int tableOffset = field & 0x3FFFFF;
        int payload = (int) (u32LE(container, tableOffset + 4) & 0xFFFFFF);
        int count = (int) u32LE(container, payload + 4);

        int[] start = new int[count];
        for (int i = 0; i < count; i++)
            start[i] = (int) (payload + u32LE(container, payload + 8 + 4 * i));

        return start;
    }

    public static int readKeyedOffset(byte[] container, long key) {
        long directoryOffset = u32LE(container, 4);
        long field = u32LE(container, (int) (directoryOffset + 0x08));
        long count = field >> 22;
        long tableOffset = field & 0x3FFFFF;
        for (int i = 0; i < count; i++) {
            long at = tableOffset + 12 * i;
            if (u32LE(container, (int) (at + 8)) == key) return (int) (u32LE(container, (int) (at + 4)) & 0xFFFFFF);
        }

        return -1;
    }

    public static Collision readCollision(byte[] container) {
        int directoryOffset = (int) u32LE(container, 4);
        int field = (int) u32LE(container, directoryOffset + 0x18);
        int tableOffset = field & 0x3FFFFF;
        int payload = (int) (u32LE(container, tableOffset + 4) & 0xFFFFFF);

        int regionCount = u16LE(container, payload + 2);
        int vOff = (int) u32LE(container, payload + 0x18);
        int tOff = (int) u32LE(container, payload + 0x1C);
        int planes = (int) u32LE(container, payload + 0x20);
        int rampOff = (int) u32LE(container, payload + 0x24);

        boolean sloped = planes != 0;
        int vStride = sloped ? 6 : 4;
        int vzAt = sloped ? 4 : 2;
        int tStride = sloped ? 28 : 22;
        int tvAt = sloped ? 8 : 2;
        int tEnd = sloped ? planes : rampOff;
        int rCountAt = sloped ? 4 : 2;
        int rIdsAt = sloped ? 6 : 4;
        int edgeAt = sloped ? 14 : 8;
        int neighAt = sloped ? 20 : 14;

        int vertexCount = (tOff - vOff) / vStride;

        int[] vx = new int[vertexCount];
        int[] vz = new int[vertexCount];

        for (int i = 0; i < vertexCount; i++) {
            vx[i] = s16LE(container, payload + vOff + vStride * i);
            vz[i] = s16LE(container, payload + vOff + vStride * i + vzAt);

        }

        int triangleCount = (tEnd - tOff) / tStride;

        int[] triRegion = new int[triangleCount];
        int[] triV = new int[triangleCount * 3];
        int[] triEdge = new int[triangleCount * 3];
        int[] triNeighbor = new int[triangleCount * 3];
        int[] triPlane = new int[triangleCount];
        int[] triDist = new int[triangleCount];
        int[] triKind = new int[triangleCount];
        for (int i = 0; i < triangleCount; i++) {
            int at = payload + tOff + tStride * i;
            triKind[i] = u8(container, at);
            triRegion[i] = u8(container, at + 1);
            triV[3 * i] = u16LE(container, at + tvAt);
            triV[3 * i + 1] = u16LE(container, at + tvAt + 2);
            triV[3 * i + 2] = u16LE(container, at + tvAt + 4);

            for (int k = 0; k < 3; k++) {
                triEdge[3 * i + k] = u16LE(container, at + edgeAt + 2 * k);
                triNeighbor[3 * i + k] = u16LE(container, at + neighAt + 2 * k);
            }

            if (sloped) {
                triPlane[i] = u16LE(container, at + 2);
                triDist[i] = (int) u32LE(container, at + 4);
            }

        }

        int edgeCount = u16LE(container, payload + 8);
        int[] edgeLength = new int[edgeCount];
        int[] edgeDirX = new int[edgeCount];
        int[] edgeDirZ = new int[edgeCount];
        for (int i = 0; i < edgeCount; i++) {
            int at = payload + rampOff + 12 * i;
            edgeLength[i] = (int) u32LE(container, at);
            edgeDirX[i] = (int) u32LE(container, at + 4);
            edgeDirZ[i] = (int) u32LE(container, at + 8);

        }

        int planeCount = sloped ? (rampOff - planes) / 12 : 0;

        int[] planeA = new int[planeCount];
        int[] planeInvB = new int[planeCount];
        int[] planeC = new int[planeCount];

        if (sloped) {
            for (int i = 0; i < planeCount; i++) {
                int at = payload + planes + 12 * i;
                planeA[i] = (int) u32LE(container, at);
                planeInvB[i] = (int) u32LE(container, at + 4);
                planeC[i] = (int) u32LE(container, at + 8);
            }
        }

        int[] regionHeight = new int[regionCount];
        int[][] regionTris = new int[regionCount][];
        for (int i = 0; i < regionCount; i++) {
            int offset = (int) u32LE(container, payload + 0x28 + 4 * i);
            int count = u16LE(container, payload + offset + rCountAt);
            regionTris[i] = new int[count];

            for (int j = 0; j < count; j++) {
                regionTris[i][j] = u16LE(container, payload + offset + rIdsAt + 2 * j);
            }
            if (!sloped) {
                regionHeight[i] = s16LE(container, payload + offset);

            }

        }

        return new Collision(vx, vz, triV, triRegion, triPlane, triDist, triKind, triEdge, triNeighbor, regionHeight, regionTris, planeA, planeInvB, planeC, edgeLength, edgeDirX, edgeDirZ);
    }

    public static int readEntityBank(byte[] container) {
        int directory = (int) u32LE(container, 4);
        return u8(container, directory + 1);
    }

    public static int readSpawnList(byte[] container) {
        int directoryOffset = (int) u32LE(container, 4);
        int field = (int) u32LE(container, directoryOffset + 0x14);
        int tableOffset = field & 0x3FFFFF;

        int payload = (int) (u32LE(container, tableOffset + 4) & 0xFFFFFF);

        return payload + (int) u32LE(container, payload);
    }

    public static List<PSXViewport> parseViewports(byte[] block) {
        int viewportOffset = s32LE(block, 0x1C);
        int count = (block.length - viewportOffset) / 52;

        List<PSXViewport> viewports = new ArrayList<>();
        for (int v = 0; v < count; v++) {
            int at = viewportOffset + v * 52;

            int projectionDistance = u16LE(block, at);
            int[] rotation = new int[9];
            for (int i = 0; i < rotation.length; i++)
                rotation[i] = s16LE(block, at + 0x02 + i * 2);

            int[] translation = new int[3];
            for (int i = 0; i < translation.length; i++)
                translation[i] = s32LE(block, at + 0x14 + i * 4);

            int width = u16LE(block, at + 0x28);
            int height = u16LE(block, at + 0x2A);
            int minX = s16LE(block, at + 0x2C);
            int maxX = s16LE(block, at + 0x2E);
            int minY = s16LE(block, at + 0x30);
            int maxY = s16LE(block, at + 0x32);

            viewports.add(new PSXViewport(projectionDistance, rotation, translation, width, height, minX, maxX, minY, maxY));
        }

        return viewports;
    }

    public static int[][] readModelRecords(byte[] container) {
        long directoryOffset = u32LE(container, 4);
        long field = u32LE(container, (int) (directoryOffset + 0x0C));
        int count = (int) (field >>> 22);
        long tableOffset = field & 0x3FFFFF;

        int[][] records = new int[count][3];
        for (int i = 0; i < count; i++) {
            long at = tableOffset + 12 * i;
            long word1 = u32LE(container, (int) (at + 4));
            records[i][0] = (int) (word1 >>> 24);
            records[i][1] = (int) (word1 & 0xFFFFFF);
            records[i][2] = (int) (u32LE(container, (int) (at + 8)));
        }

        return records;
    }

    public static PEModel parseModel(byte[] container, int offset) {
        int partCount = u8(container, offset + 0x02);
        int vertexCount = u16LE(container, offset + 0x06);
        int n34 = u16LE(container, offset + 0x08);
        int n28 = u16LE(container, offset + 0x0A);
        int n24 = u16LE(container, offset + 0x0C);
        int n1c = u16LE(container, offset + 0x0E);
        int matrixCommandBytes = u16LE(container, offset + 0x18);
        int primCount = n34 + n28 + n24 + n1c;

        int partsAt = offset + 0x1C;
        int verticesAt = partsAt + 12 * partCount;
        int colorsAt = verticesAt + 8 * vertexCount;
        int descriptionAt = colorsAt + 4 * vertexCount;
        int boundsAt = descriptionAt + 12 * primCount + 16;
        int matrixAt = boundsAt + 16 * partCount + 8;
        int quadTexturesAt = matrixAt + roundUp4(matrixCommandBytes);
        int triangleTexturesAt = quadTexturesAt + 16 * n34;

        int hitAt = descriptionAt + 12 * primCount;
        int bodyX = s16LE(container, hitAt);
        int bodyY = s16LE(container, hitAt + 2);
        int bodyZ = s16LE(container, hitAt + 4);
        int hitRadius = s16LE(container, hitAt + 6);

        PEModel model = new PEModel(partCount, vertexCount, primCount);
        model.setN34(n34);
        model.setN28(n28);
        model.setN24(n24);
        model.setN1c(n1c);
        model.setByteSize(triangleTexturesAt + 12 * n28 - offset);
        model.setAnchorY(s16LE(container, offset + 0x10));
        model.setBodyX(bodyX);
        model.setBodyY(bodyY);
        model.setBodyZ(bodyZ);
        model.setHitRadius(hitRadius);
        model.setAnchorMatrixIndex(u16LE(container, offset + 0x12));

        for (int p = 0; p < partCount; p++) {
            model.getPartVertexStart()[p] = u16LE(container, partsAt + 12 * p);
            model.getPartVertexCount()[p] = u16LE(container, partsAt + 12 * p + 2);
            model.getPartVisible()[p] = u8(container, partsAt + 12 * p + 4);
        }

        for (int v = 0; v < vertexCount; v++) {
            model.getVertices()[3 * v] = s16LE(container, verticesAt + 8 * v);
            model.getVertices()[3 * v + 1] = s16LE(container, verticesAt + 8 * v + 2);
            model.getVertices()[3 * v + 2] = s16LE(container, verticesAt + 8 * v + 4);
            model.getNormalIndex()[v] = s16LE(container, verticesAt + 8 * v + 6);
            model.getColors()[v] = (int) u32LE(container, colorsAt + 4 * v);
        }

        for (int i = 0; i < primCount; i++) {
            int at = descriptionAt + 12 * i;
            model.getPrimKind()[i] = u8(container, at + 3);

            for (int k = 0; k < 4; k++) {
                model.getPrimVertex()[4 * i + k] = u16LE(container, at + 4 + 2 * k);
            }
        }

        for (int i = 0; i < n34; i++) {
            int at = quadTexturesAt + 16 * i;
            model.getPrimUV()[8 * i] = u8(container, at);
            model.getPrimUV()[8 * i + 1] = u8(container, at + 1);
            model.getPrimUV()[8 * i + 2] = u8(container, at + 4);
            model.getPrimUV()[8 * i + 3] = u8(container, at + 5);
            model.getPrimUV()[8 * i + 4] = u8(container, at + 8);
            model.getPrimUV()[8 * i + 5] = u8(container, at + 9);
            model.getPrimUV()[8 * i + 6] = u8(container, at + 12);
            model.getPrimUV()[8 * i + 7] = u8(container, at + 13);
            model.getPrimClut()[i] = u16LE(container, at + 2);
            model.getPrimPageBits()[i] = u16LE(container, at + 6);
        }

        for (int i = 0; i < n28; i++) {
            int j = n34 + i;
            int at = triangleTexturesAt + 12 * i;
            model.getPrimUV()[8 * j] = u8(container, at);
            model.getPrimUV()[8 * j + 1] = u8(container, at + 1);
            model.getPrimUV()[8 * j + 2] = u8(container, at + 4);
            model.getPrimUV()[8 * j + 3] = u8(container, at + 5);
            model.getPrimUV()[8 * j + 4] = u8(container, at + 8);
            model.getPrimUV()[8 * j + 5] = u8(container, at + 9);
            model.getPrimClut()[j] = u16LE(container, at + 2);
            model.getPrimPageBits()[j] = u16LE(container, at + 6);
        }

        parseSkeleton(container, partsAt, matrixAt, matrixCommandBytes, model);
        return model;
    }

    public static int[][] readAnimationRecords(byte[] container) {
        long directoryOffset = u32LE(container, 4);
        long field = u32LE(container, (int) (directoryOffset + 0x10));
        long count = field >>> 22;
        long tableOffset = field & 0x3FFFFF;

        int[][] records = new int[(int) count][4];
        for (int i = 0; i < count; i++) {
            long at = tableOffset + 12 * i;
            long word0 = u32LE(container, (int) at);
            long word1 = u32LE(container, (int) (at + 4));
            long word2 = u32LE(container, (int) (at + 8));
            records[i][0] = (int) (word2 >>> 24);
            records[i][1] = (int) (word1 >>> 24);
            records[i][2] = (int) (word1 & 0xFFFFFF);
            records[i][3] = (int) (word0 & 0xFFFFFF);
        }

        return records;
    }

    public static void applyTextureRemap(PEModel model, int vramWord) {

        if (((vramWord >> 8) & 0xF) == 0) return;

        int x = (vramWord >> 6) & 0x3C0;
        int y = (vramWord >> 9) & 0x180;
        int paletteRow = ((vramWord >> 18) & 0xFF) + 0x1C0;
        int page = ((x >> 6) - 8) * 2 + (y >> 7) + (y >> 8) * 30;
        int pageOffset = page >> 1;
        int clutOffset = paletteRow * 64 - 0x7080;

        if (x == 960 && model.getPrimPageBits()[0] == 31) return;

        for (int i = 0; i < model.getN34() + model.getN28(); i++) {
            model.getPrimPageBits()[i] = model.getPrimPageBits()[i] + pageOffset;
            model.getPrimClut()[i] = (model.getPrimClut()[i] + clutOffset) & 0xFFFF;
        }
    }

    public static PEAnimation parseAnimation(byte[] container, int offset) {
        int encoding = u8(container, offset);
        int lastBone = u8(container, offset + 1);
        int frameCount = u8(container, offset + 2);
        int bodyX = s16LE(container, offset + 4);
        int bodyY = s16LE(container, offset + 6);
        int bodyZ = s16LE(container, offset + 8);
        int bodyRadius = s16LE(container, offset + 0x0A);
        int boneCount = lastBone + 1;
        int channelCount = 3 + 3 * boneCount;

        int[][] channels = new int[channelCount][frameCount];

        int cursor = offset + 0x0C;
        for (int c = 0; c < channelCount; c++) {
            boolean isShort = (c < 3) || ((encoding & 3) == 2);

            int marker;
            if (isShort) {
                marker = s16LE(container, cursor);
                for (int f = 0; f < frameCount; f++) {
                    if (marker != 0) {
                        channels[c][f] = s16LE(container, cursor + 2);
                    } else {
                        channels[c][f] = s16LE(container, cursor + 2 + 2 * f);
                    }
                }
            } else {
                marker = u8(container, cursor);
                for (int f = 0; f < frameCount; f++) {
                    if (marker != 0)
                        channels[c][f] = u8(container, cursor + 1) * 0x10;
                    else channels[c][f] = u8(container, cursor + 1 + f) * 0x10;
                }
            }

            if (marker != 0)
                cursor = cursor + 4;
            else if (isShort)
                cursor = cursor + 4 * ((frameCount >> 1) + 1);
            else
                cursor = cursor + 4 * ((frameCount >> 2) + 1);
        }

        return new PEAnimation(frameCount, boneCount, (cursor - offset), bodyX, bodyY, bodyZ, bodyRadius, channels);
    }

    public static void parseSkeleton(byte[] container, int partsAt, int matrixAt, int matrixCommandBytes, PEModel model) {
        model.setPartTranslationZ(new int[model.getPartCount()]);
        for (int p = 0; p < model.getPartCount(); p++)
            model.getPartTranslationZ()[p] = s32LE(container, partsAt + 12 * p + 8);

        model.setMatrixCommands(new int[matrixCommandBytes]);
        for (int i = 0; i < matrixCommandBytes; i++)
            model.getMatrixCommands()[i] = s8(container, matrixAt + i);
    }

    private static int roundUp4(int x) {
        return (x + 3) & ~3;
    }

}