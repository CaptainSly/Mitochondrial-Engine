package com.duckcraftian.upf.video;

import com.duckcraftian.upf.UPF;
import com.duckcraftian.upf.ps1.video.BSDecoder;
import com.duckcraftian.upf.ps1.video.BitReader;
import com.duckcraftian.upf.ps1.video.StrDemux;
import com.duckcraftian.upf.serialization.UPDHSerializer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StrDemuxTest {

    @Test
    void demuxTest() throws IOException {
        Path imagesFiles = UPF.getEngineFolder("data").resolve("pe.udh");
        byte[] fmv000 = UPDHSerializer.getStreamedData(imagesFiles, "movies/FMV000");
        List<byte[]> frames = StrDemux.demux(fmv000);
        assertEquals(150, frames.size());
        assertEquals(14772, frames.get(59).length);

        BitReader bit = new BitReader();
        bit.newFrame(frames.get(59), 8);
        assertEquals(0x0020, bit.peek(16));
        assertEquals(0, bit.peek(10));

        int[] out = new int[56];
        int[] firstWords = {0x400, 0x400, 0x600, 0x600, 0x600, 0x600, 0x400, 0x400};

        for (int n = 0; n < 8; n++) {
            int count = BSDecoder.readBlock(bit, 1, out);
            assertEquals(2, count);
            assertEquals(firstWords[n], out[0]);
            assertEquals(0xFE00, out[1]);
        }

        int count = BSDecoder.readBlock(bit, 1, out);
        assertEquals(6, count);
        assertEquals(0x601, out[0]);
        assertEquals(0x3FF, out[1]);
        assertEquals(0x3FF, out[2]);
        assertEquals(0x401, out[3]);
        assertEquals(0x1, out[4]);
        assertEquals(0xFE00, out[5]);


    }

}