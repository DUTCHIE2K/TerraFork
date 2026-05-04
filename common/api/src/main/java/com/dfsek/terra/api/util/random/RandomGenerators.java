package com.dfsek.terra.api.util.random;

import java.util.random.RandomGenerator;

public final class RandomGenerators {
    private RandomGenerators() {

    }

    public static RandomGenerator xoroshiro128PlusPlus() {
        return new Xoroshiro128PlusPlusRandom();
    }

    public static RandomGenerator xoroshiro128PlusPlus(long seed) {
        return new Xoroshiro128PlusPlusRandom(seed);
    }
}
