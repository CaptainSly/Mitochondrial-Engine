package com.duckcraftian.upf.ps1.video;

import com.duckcraftian.upf.ps1.audio.XaAdpcm;

import java.util.ArrayList;
import java.util.List;

import static com.duckcraftian.upf.ps1.disc.DiscConstants.RAW_SECTOR_SIZE;
import static com.duckcraftian.upf.system.DataTypeHelper.u8;

public class MovieAudio {

    public static final int PER_SECTOR = 2016;

    public static short[][] decode(byte[] all) {
        int sectors = 0;
        for (int s = 0; s < all.length / RAW_SECTOR_SIZE; s++) {
            if ((u8(all, s * RAW_SECTOR_SIZE + 18) & 0x04) != 0) {
                sectors = sectors + 1;
            }
        }

        short[] left = new short[sectors * PER_SECTOR];
        short[] right = new short[sectors * PER_SECTOR];
        XaAdpcm xa = new XaAdpcm();
        List<Integer> tmpL = new ArrayList<>();
        List<Integer> tmpR = new ArrayList<>();

        int at = 0;

        for (int s = 0; s < all.length / RAW_SECTOR_SIZE; s++) {
            int offset = s * RAW_SECTOR_SIZE;
            if ((u8(all, offset + 18) & 0x04) != 0) {
                tmpL.clear();
                tmpR.clear();
                xa.decodeSector(all, offset, tmpL, tmpR);
                for (int k = 0; k < PER_SECTOR; k++) {
                    int tl = tmpL.get(k);
                    int tr = tmpR.get(k);

                    left[at + k] = (short) tl;
                    right[at + k] = (short) tr;
                }

                at = at + PER_SECTOR;
            }
        }

        return new short[][]{left, right};
    }

}