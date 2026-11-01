package com.duckcraftian.upf.pe.rooms;

import static com.duckcraftian.upf.ps1.PSXMath.fixedMul;

public record Collision(int[] vx, int[] vz, int[] triV, int[] triRegion, int[] triPlane, int[] triDist, int[] triKind,
                        int[] triEdge,
                        int[] triNeighbor, int[] regionHeight, int[][] regionTris, int[] planeA, int[] planeInvB,
                        int[] planeC, int[] edgeLength, int[] edgeDirX, int[] edgeDirZ) {

    public boolean pointInTri(int t, int x, int z) {
        int px = vx[triV[3 * t + 2]];
        int pz = vz[triV[3 * t + 2]];
        boolean inside = false;

        for (int k = 0; k < 3; k++) {
            int nx = vx[triV[3 * t + k]];
            int nz = vz[triV[3 * t + k]];

            if ((z >= nz && z < pz) || (z < nz && z >= pz)) {
                if (x < nx && x < px) {
                    inside = !inside;
                } else if (x < nx || x < px) {
                    int dz = pz - nz;
                    int side = (px - nx) * (z - nz);
                    if (dz < 0 && side < dz * (x - nx))
                        inside = !inside;
                    else if (dz >= 0 && dz * (x - nx) < side)
                        inside = !inside;

                }
            }

            px = nx;
            pz = nz;
        }

        return inside;
    }

    public boolean edgesTouchCircle(int t, int slot, int px, int pz, int r) {
        int nextIdx = triV[3 * t + slot];
        int prevIdx = triV[3 * t + (slot + 2) % 3];
        int nextX = vx[nextIdx], nextZ = vz[nextIdx];
        int prevX = vx[prevIdx], prevZ = vz[prevIdx];
        int e = triEdge[3 * t + slot];

        if (nextX < px - r && prevX < px - r) return false;
        if (nextX > px + r && prevX > px + r) return false;
        if (nextZ < pz - r && prevZ < pz - r) return false;
        if (nextZ > pz + r && prevZ > pz + r) return false;

        int dx = px - nextX;
        int dz = pz - nextZ;
        int len = edgeLength[e] >> 16;
        int dist = Math.abs((dz * (prevX - nextX) - dx * (prevZ - nextZ)) / len);
        if (dist > r) return false;

        boolean farFromNext = (dx * dx + dz * dz) > r * r;

        int along, ex, ez;
        if (nextIdx < prevIdx) {
            along = fixedMul(edgeDirX[e], dx << 16) + fixedMul(edgeDirZ[e], dz << 16);
            if (along < 0 && farFromNext) return false;
            if (edgeLength[e] < along) {
                ex = px - prevX;
                ez = pz - prevZ;
                if (ex * ex + ez * ez > r * r) return false;
            }
        } else {
            ex = px - prevX;
            ez = pz - prevZ;
            along = fixedMul(edgeDirX[e], ex << 16) + fixedMul(edgeDirZ[e], ez << 16);
            if (along < 0 && ex * ex + ez * ez > r * r) return false;
            if (edgeLength[e] < along && farFromNext) return false;
        }

        return true;
    }

    public boolean floorClear(int face, int px, int pz, int r, int[] wall) {
        return floorClear(face, px, pz, r, wall, -1);
    }

    public boolean floorClear(int face, int px, int pz, int r, int[] wall, int ignoreEdge) {
        boolean[] visited = new boolean[edgeLength.length];
        if (ignoreEdge >= 0) visited[ignoreEdge] = true;
        return clip(face, px, pz, r, visited, wall);
    }

    public int wallDistance(int x, int z, int[] wall) {
        int edgeX = wall[2] - wall[0];
        int edgeZ = wall[3] - wall[1];
        int numerator = ((z >> 16) - wall[1]) * edgeX - ((x >> 16) - wall[0]) * edgeZ;
        int len = edgeLength[wall[4]] >> 16;
        return Math.abs(numerator / len);
    }

    public int walkToFace(int t, int previous, int x0, int z0, int x1, int z1) {
        int deltaX = x1 - x0;
        int deltaZ = z1 - z0;

        int maxX = 0, minX = 0, maxZ = 0, minZ = 0;
        if (deltaX < 0) {
            maxX = x0;
            minX = x1;
        } else {
            maxX = x1;
            minX = x0;
        }

        if (deltaZ < 0) {
            maxZ = z0;
            minZ = z1;
        } else {
            maxZ = z1;
            minZ = z0;
        }

        for (int edge = 0; edge < 3; edge++) {
            int nextIdx = triV[3 * t + edge];
            int prevIdx = triV[3 * t + (edge + 2) % 3];
            int nextX = vx[nextIdx];
            int nextZ = vz[nextIdx];
            int prevX = vx[prevIdx];
            int prevZ = vz[prevIdx];

            int edgeX = nextX - prevX;
            if (edgeX > 0) {
                if (maxX < prevX || minX >= nextX) continue;
            } else {
                if (maxX < nextX || minX >= prevX) continue;
            }

            int edgeZ = nextZ - prevZ;
            if (edgeZ > 0) {
                if (maxZ < prevZ || minZ >= nextZ) continue;
            } else {
                if (maxZ < nextZ || minZ >= prevZ) continue;
            }

            int startX = x0 - nextX;
            int startZ = z0 - nextZ;
            int denominator = deltaZ * edgeX - deltaX * edgeZ;

            int along = edgeZ * startX - edgeX * startZ;
            if (denominator > 0) {
                if (along < 0 || denominator < along) continue;
            } else {
                if (along > 0 || along < denominator) continue;
            }

            along = deltaX * startZ - deltaZ * startX;
            if (denominator > 0) {
                if (along < 0 || denominator < along) continue;
            } else {
                if (along > 0 || along < denominator) continue;
            }

            if (denominator == 0) continue;

            int nb = triNeighbor[3 * t + edge];
            if (nb == 0xFFFF) continue;
            if (nb == previous) continue;
            if (pointInTri(nb, x0, z0)) return nb;

            int r = walkToFace(nb, t, x0, z0, x1, z1);
            if (r >= 0) return r;
        }

        return -1;
    }

    private boolean clip(int t, int px, int pz, int r, boolean[] visited, int[] wall) {
        int[] queued = new int[]{-1, -1, -1};

        for (int slot = 0; slot < 3; slot++) {
            int e = triEdge[3 * t + slot];
            if (visited[e]) continue;
            visited[e] = true;

            if (!edgesTouchCircle(t, slot, px, pz, r)) continue;

            int nb = triNeighbor[3 * t + slot];
            if (nb == 0xFFFF || (triKind[nb] & 0x80) != 0) {
                int nextIdx = triV[3 * t + slot];
                int prevIdx = triV[3 * t + (slot + 2) % 3];
                wall[0] = vx[nextIdx];
                wall[1] = vz[nextIdx];
                wall[2] = vx[prevIdx];
                wall[3] = vz[prevIdx];
                wall[4] = e;
                return false;
            }

            queued[slot] = nb;
        }

        for (int slot = 0; slot < 3; slot++)
            if (queued[slot] >= 0 && !clip(queued[slot], px, pz, r, visited, wall)) return false;

        return true;
    }

    public static boolean pointInPoly(int x, int z, int[] polyX, int[] polyZ) {
        int n = polyX.length;
        int px = polyX[n - 1];
        int pz = polyZ[n - 1];
        boolean inside = false;

        for (int k = 0; k < n; k++) {
            int nx = polyX[k];
            int nz = polyZ[k];


            if ((z >= nz && z < pz) || (z < nz && z >= pz)) {
                if (x < nx && x < px)
                    inside = !inside;
                else if (x < nx || x < px) {
                    int dz = pz - nz;
                    int side = (px - nx) * (z - nz);

                    if (dz < 0 && side < dz * (x - nx))
                        inside = !inside;
                    else if (dz >= 0 && dz * (x - nx) < side)
                        inside = !inside;
                }
            }
            px = nx;
            pz = nz;
        }

        return inside;
    }

    public int findTriangle(int x, int z) {
        for (int r = 0; r < regionTris.length; r++)
            for (int t : regionTris[r])
                if (pointInTri(t, x, z)) return t;

        return -1;
    }

    public int floorY(int t, int x, int z) {
        if (planeA.length == 0)
            return regionHeight[triRegion[t]] << 16;

        int p = triPlane[t];
        return fixedMul(triDist[t] - fixedMul(planeA[p], x) - fixedMul(planeC[p], z), planeInvB[p]);
    }

}