package com.duckcraftian.upf.ps1;

import org.junit.jupiter.api.Test;

import static com.duckcraftian.upf.ps1.PSXMath.rcos;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PSXMathTest {

    @Test
    void rcosSmoothFactorTest() {
        assertEquals(4096, rcos(0));
        assertEquals(0, rcos(1024));
        assertEquals(-4096, rcos(2048));
        assertEquals(0, rcos(3072));
        assertEquals(4096, rcos(4096));

        int[] times = {
                0,
                15,
                30
        };

        int[] results = new int[3];
        int i = 0;
        for (int time : times) {
            int factor = rcos(time * 2048 / 30 + 2048) + 4096;
            results[i] = factor;
            i++;
        }

        assertArrayEquals(results, new int[]{0, 4096, 8192});

    }

}