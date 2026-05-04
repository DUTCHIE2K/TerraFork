package com.dfsek.seismic.math.trigonometry;

import java.util.SplittableRandom;

import com.dfsek.seismic.math.floatingpoint.FloatingPointFunctions;


class TrigonometryUtils {
    // Polynomial defs for atan2 taken from https://mazzo.li/posts/vectorized-atan2.html
    protected static final double a1 = 0.99997726;
    protected static final double a3 = -0.33262347;
    protected static final double a5 = 0.19354346;
    protected static final double a7 = -0.11643287;
    protected static final double a9 = 0.05265332;
    protected static final double a11 = -0.01172120;
    private static final int LOOKUP_BITS = 16;
    static final int lookupTableSize = 1 << LOOKUP_BITS;
    private static final int LOOKUP_TABLE_SIZE_WITH_MARGIN = lookupTableSize + 1;
    private static final float TAU_OVER_LOOKUP_SIZE = (float) (TrigonometryConstants.TAU / lookupTableSize);
    static final double radianToIndex = (~(-1 << LOOKUP_BITS) + 1) / TrigonometryConstants.TAU;
    private static final int[] sinTable;

    static {
        sinTable = new int[LOOKUP_TABLE_SIZE_WITH_MARGIN];
        for(int i = 0; i < LOOKUP_TABLE_SIZE_WITH_MARGIN; i++) {
            double d = i * TAU_OVER_LOOKUP_SIZE;
            sinTable[i] = Float.floatToRawIntBits((float) StrictMath.sin(d));
        }

        // Four cardinal directions (credits: Nate)
        for(int i = 0; i < 360; i += 90) {
            double rad = Math.toRadians(i);
            sinTable[(int) (rad * radianToIndex) & 0xFFFF] = Float.floatToRawIntBits((float) StrictMath.sin(rad));
        }

        SplittableRandom random = new SplittableRandom(0x5EEDC0DEL);
        for(int i = 0; i < LOOKUP_TABLE_SIZE_WITH_MARGIN; i++) {
            double d = (random.nextDouble() * 2 * Math.PI) - Math.PI;
            double expected = TrigonometryFunctions.sin(d);
            double value = StrictMath.sin(d);

            if(!FloatingPointFunctions.equalsWithinEpsilon(expected, value, 0.0001)) {
                throw new IllegalArgumentException(String.format("LUT error at value %f (expected: %s, found: %s)", d,
                    expected, value));
            }
        }

        for(int i = 0; i < 360; i += 90) {
            double rad = Math.toRadians(i);
            double expected = TrigonometryFunctions.sin(rad);
            double value = StrictMath.sin(rad);

            if(!FloatingPointFunctions.equals(expected, value)) {
                throw new IllegalArgumentException(
                    String.format("LUT error at cardinal direction %s (expected: %s, found: %s)", i,
                        expected, value));
            }
        }
    }

    static double sinLookup(int index) {
        int neg = (index & 0x8000) << 16;
        int mask = (index << 17) >> 31;
        int pos = (0x8001 & mask) + (index ^ mask);
        pos &= 0x7fff;
        return Float.intBitsToFloat(sinTable[pos] ^ neg);
    }
}
