package com.dfsek.terra.api.util.random;

import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;


final class Xoroshiro128PlusPlusRandom implements RandomGenerator {
    private static final long GOLDEN_RATIO_64 = 0x9e3779b97f4a7c15L;
    private static final long SILVER_RATIO_64 = 0x6A09E667F3BCC909L;
    private static final AtomicLong DEFAULT_SEED = new AtomicLong(initialSeed());

    private long x0;
    private long x1;

    Xoroshiro128PlusPlusRandom() {
        this(DEFAULT_SEED.getAndAdd(GOLDEN_RATIO_64));
    }

    Xoroshiro128PlusPlusRandom(long seed) {
        seed ^= SILVER_RATIO_64;
        this.x0 = mixStafford13(seed);
        this.x1 = mixStafford13(seed + GOLDEN_RATIO_64);

        if((x0 | x1) == 0) {
            this.x0 = GOLDEN_RATIO_64;
            this.x1 = SILVER_RATIO_64;
        }
    }

    @Override
    public long nextLong() {
        final long s0 = x0;
        long s1 = x1;

        final long result = Long.rotateLeft(s0 + s1, 17) + s0;

        s1 ^= s0;
        x0 = Long.rotateLeft(s0, 49) ^ s1 ^ (s1 << 21);
        x1 = Long.rotateLeft(s1, 28);

        return result;
    }

    private static long initialSeed() {
        return mixStafford13(System.currentTimeMillis()) ^ mixStafford13(System.nanoTime());
    }

    private static long mixStafford13(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
