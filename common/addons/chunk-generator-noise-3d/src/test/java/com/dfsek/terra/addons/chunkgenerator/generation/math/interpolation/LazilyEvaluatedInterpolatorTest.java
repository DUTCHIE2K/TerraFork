package com.dfsek.terra.addons.chunkgenerator.generation.math.interpolation;

import com.dfsek.seismic.math.floatingpoint.FloatingPointFunctions;
import com.dfsek.seismic.math.numericanalysis.interpolation.InterpolationFunctions;
import com.dfsek.seismic.type.sampler.Sampler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.dfsek.terra.addons.chunkgenerator.config.noise.BiomeNoiseProperties;
import com.dfsek.terra.addons.chunkgenerator.config.noise.ThreadLocalNoiseHolder;
import com.dfsek.terra.api.properties.Context;
import com.dfsek.terra.api.properties.PropertyKey;
import com.dfsek.terra.api.world.biome.Biome;
import com.dfsek.terra.api.world.biome.PlatformBiome;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;


class LazilyEvaluatedInterpolatorTest {
    private static final PropertyKey<BiomeNoiseProperties> NOISE_PROPERTIES_KEY = Context.create(BiomeNoiseProperties.class);
    private static final PlatformBiome PLATFORM_BIOME = () -> "test";
    private static final long SEED = 913578246L;

    @Test
    void matchesReferenceImplementationAcrossShiftedAndAlignedGrids() {
        assertMatchesOriginal(7, -3, -10, 18, 5, 6);
        assertMatchesOriginal(-2, 4, 0, 32, 4, 4);
    }

    @Test
    void reusesCachedSamplesForRepeatedQueries() {
        CountingSampler firstSampler = new CountingSampler(0.75);
        CountingSampler secondSampler = new CountingSampler(-1.25);
        CountingBiomeProvider provider = new CountingBiomeProvider(
            new TestBiome("first", firstSampler),
            new TestBiome("second", secondSampler));

        LazilyEvaluatedInterpolator interpolator =
            new LazilyEvaluatedInterpolator(provider, 3, -2, 18, NOISE_PROPERTIES_KEY, -10, 5, 6, SEED);

        double expected = interpolator.sample(7, -1, 13);
        int biomeCalls = provider.getBiomeCalls();
        int samplerCalls = firstSampler.getSample3DCalls() + secondSampler.getSample3DCalls();

        assertEquals(expected, interpolator.sample(7, -1, 13));
        assertEquals(biomeCalls, provider.getBiomeCalls());
        assertEquals(samplerCalls, firstSampler.getSample3DCalls() + secondSampler.getSample3DCalls());
    }

    private static void assertMatchesOriginal(int chunkX, int chunkZ, int min, int max, int horizontalRes, int verticalRes) {
        CountingSampler firstSampler = new CountingSampler(0.5);
        CountingSampler secondSampler = new CountingSampler(-2.0);
        CountingSampler thirdSampler = new CountingSampler(3.25);
        CountingBiomeProvider provider = new CountingBiomeProvider(
            new TestBiome("first", firstSampler),
            new TestBiome("second", secondSampler),
            new TestBiome("third", thirdSampler));

        LazilyEvaluatedInterpolator interpolator =
            new LazilyEvaluatedInterpolator(provider, chunkX, chunkZ, max, NOISE_PROPERTIES_KEY, min, horizontalRes, verticalRes, SEED);
        ReferenceLazilyEvaluatedInterpolator reference =
            new ReferenceLazilyEvaluatedInterpolator(provider, chunkX, chunkZ, max, NOISE_PROPERTIES_KEY, min, horizontalRes, verticalRes,
                SEED);

        for(int y = min; y < max; y++) {
            for(int x = 0; x < 16; x++) {
                for(int z = 0; z < 16; z++) {
                    double expected = reference.sample(x, y, z);
                    assertEquals(expected, interpolator.sample(x, y, z), 1.0E-12,
                        "Mismatch at x=" + x + ", y=" + y + ", z=" + z + ", hRes=" + horizontalRes + ", vRes=" + verticalRes);
                }
            }
        }
        CountingSampler fourthSampler = new CountingSampler(-0.125);
        CountingSampler fifthSampler = new CountingSampler(1.875);
        CountingBiomeProvider generatorOrderProvider = new CountingBiomeProvider(
            new TestBiome("fourth", fourthSampler),
            new TestBiome("fifth", fifthSampler));
        LazilyEvaluatedInterpolator generatorOrderInterpolator =
            new LazilyEvaluatedInterpolator(generatorOrderProvider, chunkX, chunkZ, max, NOISE_PROPERTIES_KEY, min, horizontalRes,
                verticalRes, SEED);
        ReferenceLazilyEvaluatedInterpolator generatorOrderReference =
            new ReferenceLazilyEvaluatedInterpolator(generatorOrderProvider, chunkX, chunkZ, max, NOISE_PROPERTIES_KEY, min,
                horizontalRes, verticalRes, SEED);

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                for(int y = max - 1; y >= min; y--) {
                    double expected = generatorOrderReference.sample(x, y, z);
                    assertEquals(expected, generatorOrderInterpolator.sample(x, y, z), 1.0E-12,
                        "Generator-order mismatch at x=" + x + ", y=" + y + ", z=" + z + ", hRes=" + horizontalRes + ", vRes="
                            + verticalRes);
                }
            }
        }
    }

    private static final class CountingBiomeProvider implements BiomeProvider {
        private final List<Biome> biomes;
        private final AtomicInteger biomeCalls = new AtomicInteger();

        private CountingBiomeProvider(Biome... biomes) {
            this.biomes = List.of(biomes);
        }

        @Override
        public Biome getBiome(int x, int y, int z, long seed) {
            biomeCalls.incrementAndGet();
            int index = Math.floorMod((int) (seed + (31L * x) + (17L * y) + (13L * z)), biomes.size());
            return biomes.get(index);
        }

        @Override
        public Iterable<Biome> getBiomes() {
            return biomes;
        }

        private int getBiomeCalls() {
            return biomeCalls.get();
        }
    }

    private static final class TestBiome implements Biome {
        private final String id;
        private final int intId = INT_ID_COUNTER.getAndIncrement();
        private final Context context = new Context();

        private TestBiome(String id, Sampler carving) {
            this.id = id;
            context.put(NOISE_PROPERTIES_KEY,
                new BiomeNoiseProperties(Sampler.zero(), Sampler.zero(), carving, 0, 0, 0, 0, new ThreadLocalNoiseHolder()));
        }

        @Override
        public PlatformBiome getPlatformBiome() {
            return PLATFORM_BIOME;
        }

        @Override
        public int getColor() {
            return 0;
        }

        @Override
        public Set<String> getTags() {
            return Set.of();
        }

        @Override
        public int getIntID() {
            return intId;
        }

        @Override
        public Context getContext() {
            return context;
        }

        @Override
        public String getID() {
            return id;
        }
    }

    private static final class CountingSampler implements Sampler {
        private final double offset;
        private final AtomicInteger sample3DCalls = new AtomicInteger();

        private CountingSampler(double offset) {
            this.offset = offset;
        }

        @Override
        public double getSample(long seed, double x, double y) {
            return offset + (seed * 1.0E-7) + (x * 0.5) - (y * 0.25);
        }

        @Override
        public double getSample(long seed, double x, double y, double z) {
            sample3DCalls.incrementAndGet();
            return offset + (seed * 1.0E-7) + (x * 0.5) - (y * 0.25) + (z * 0.75) + (x * z * 0.01) - (y * z * 0.005);
        }

        private int getSample3DCalls() {
            return sample3DCalls.get();
        }
    }

    private static final class ReferenceLazilyEvaluatedInterpolator {
        private final Double[] samples;

        private final int chunkX;
        private final int chunkZ;

        private final int horizontalRes;
        private final int verticalRes;

        private final BiomeProvider biomeProvider;
        private final PropertyKey<BiomeNoiseProperties> noisePropertiesKey;

        private final long seed;
        private final int min;
        private final int max;

        private final int zMul;
        private final int yMul;

        private ReferenceLazilyEvaluatedInterpolator(BiomeProvider biomeProvider, int cx, int cz, int max,
                                                     PropertyKey<BiomeNoiseProperties> noisePropertiesKey, int min,
                                                     int horizontalRes, int verticalRes, long seed) {
            this.noisePropertiesKey = noisePropertiesKey;
            int hSamples = FloatingPointFunctions.ceil(16.0 / horizontalRes);
            int vSamples = FloatingPointFunctions.ceil((double) (max - min) / verticalRes);
            this.zMul = hSamples + 1;
            this.yMul = zMul * zMul;
            this.samples = new Double[yMul * (vSamples + 1)];
            this.chunkX = cx << 4;
            this.chunkZ = cz << 4;
            this.horizontalRes = horizontalRes;
            this.verticalRes = verticalRes;
            this.biomeProvider = biomeProvider;
            this.seed = seed;
            this.min = min;
            this.max = max - 1;
        }

        private double sample(int xIndex, int yIndex, int zIndex, int ox, int oy, int oz) {
            int index = xIndex + (zIndex * zMul) + (yIndex * yMul);
            Double sample = samples[index];
            if(sample == null) {
                int xi = ox + chunkX;
                int zi = oz + chunkZ;

                int y = Math.min(max, oy);

                sample = biomeProvider
                    .getBiome(xi, y, zi, seed)
                    .getContext()
                    .get(noisePropertiesKey)
                    .carving()
                    .getSample(seed, xi, y, zi);
                samples[index] = sample;
            }
            return sample;
        }

        private double sample(int x, int y, int z) {
            int xIndex = x / horizontalRes;
            int yIndex = (y - min) / verticalRes;
            int zIndex = z / horizontalRes;

            double sample_0_0_0 = sample(xIndex, yIndex, zIndex, x, y, z);

            boolean yRange = y % verticalRes == 0;
            if(x % horizontalRes == 0 && yRange && z % horizontalRes == 0) {
                return sample_0_0_0;
            }

            double sample_0_0_1 = sample(xIndex, yIndex, zIndex + 1, x, y, z + horizontalRes);

            double sample_1_0_0 = sample(xIndex + 1, yIndex, zIndex, x + horizontalRes, y, z);
            double sample_1_0_1 = sample(xIndex + 1, yIndex, zIndex + 1, x + horizontalRes, y, z + horizontalRes);

            double xFrac = (double) (x % horizontalRes) / horizontalRes;
            double zFrac = (double) (z % horizontalRes) / horizontalRes;
            double lerpBottom0 = InterpolationFunctions.lerp(sample_0_0_0, sample_0_0_1, zFrac);
            double lerpBottom1 = InterpolationFunctions.lerp(sample_1_0_0, sample_1_0_1, zFrac);

            double lerpBottom = InterpolationFunctions.lerp(lerpBottom0, lerpBottom1, xFrac);

            if(yRange) {
                return lerpBottom;
            }

            double yFrac = (double) Math.floorMod(y, verticalRes) / verticalRes;


            double sample_0_1_0 = sample(xIndex, yIndex + 1, zIndex, x, y + verticalRes, z);
            double sample_0_1_1 = sample(xIndex, yIndex + 1, zIndex + 1, x, y + verticalRes, z + horizontalRes);


            double sample_1_1_0 = sample(xIndex + 1, yIndex + 1, zIndex, x + horizontalRes, y + verticalRes, z);
            double sample_1_1_1 = sample(xIndex + 1, yIndex + 1, zIndex + 1, x + horizontalRes, y + verticalRes,
                z + horizontalRes);

            double lerpTop0 = InterpolationFunctions.lerp(sample_0_1_0, sample_0_1_1, zFrac);
            double lerpTop1 = InterpolationFunctions.lerp(sample_1_1_0, sample_1_1_1, zFrac);

            double lerpTop = InterpolationFunctions.lerp(lerpTop0, lerpTop1, xFrac);

            return InterpolationFunctions.lerp(lerpBottom, lerpTop, yFrac);
        }
    }
}
