package com.duckcraftian.upf.ps1.video;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.duckcraftian.upf.ps1.disc.DiscConstants.DATA_OFFSET;
import static com.duckcraftian.upf.ps1.disc.DiscConstants.RAW_SECTOR_SIZE;
import static com.duckcraftian.upf.system.DataTypeHelper.*;

public class StrDemux {

    public static final int FMV_PAYLOAD = 0x7E0;

    public static List<byte[]> demux(byte[] all) {
        List<byte[]> frames = new ArrayList<>();
        ByteArrayOutputStream cur = new ByteArrayOutputStream();

        for (int s = 0; s < all.length / RAW_SECTOR_SIZE; s++) {
            int off = s * RAW_SECTOR_SIZE;
            if ((u8(all, off + 18) & 0x7E) == 0x48) {
                int d = off + DATA_OFFSET;
                if (u16LE(all, d) == 0x0160) {
                    int index = u16LE(all, d + 4);
                    int count = u16LE(all, d + 6);
                    long size = u32LE(all, d + 12);
                    if (index == 0) cur.reset();
                    cur.write(all, d + 0x20, FMV_PAYLOAD);

                    if (index == count - 1)
                        frames.add(Arrays.copyOfRange(cur.toByteArray(), 0, (int) size));
                }
            }
        }

        return frames;
    }

}