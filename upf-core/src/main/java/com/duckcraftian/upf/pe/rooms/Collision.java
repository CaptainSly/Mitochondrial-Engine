package com.duckcraftian.upf.pe.rooms;

import com.duckcraftian.upf.ps1.PSXMath;

import static com.duckcraftian.upf.ps1.PSXMath.fixedMul;

public record Collision(int[] vx, int[] vz, int[] triV, int[] triRegion, int[] triPlane, int[] triDist, int[] triKind,
                        int[] regionHeight, int[][] regionTris, int[] planeA, int[] planeInvB, int[] planeC) {

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