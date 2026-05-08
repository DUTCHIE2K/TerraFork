package com.dfsek.terra.api.world.biome.generation;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.dfsek.terra.api.properties.Context;
import com.dfsek.terra.api.util.Column;
import com.dfsek.terra.api.world.biome.Biome;
import com.dfsek.terra.api.world.biome.PlatformBiome;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CachingBiomeProviderTest {
    @Test
    void preservesSpecializedColumnsFromDelegate() {
        TestBiome biome = new TestBiome("specialized");
        BiomeProvider delegate = new BiomeProvider() {
            @Override
            public Biome getBiome(int x, int y, int z, long seed) {
                return biome;
            }

            @Override
            public Column<Biome> getColumn(int x, int z, long seed, int min, int max) {
                return new SpecializedColumn(min, max, biome);
            }

            @Override
            public Iterable<Biome> getBiomes() {
                return Set.of(biome);
            }
        };

        CachingBiomeProvider provider = new CachingBiomeProvider(delegate);
        Column<Biome> column = provider.getColumn(4, 8, 12L, -10, 10);

        assertTrue(column instanceof SpecializedColumn);
        assertSame(biome, column.get(0));
    }

    @Test
    void keepsCachingForGenericColumns() {
        TestBiome biome = new TestBiome("generic");
        AtomicInteger biomeLookups = new AtomicInteger();
        BiomeProvider delegate = new BiomeProvider() {
            @Override
            public Biome getBiome(int x, int y, int z, long seed) {
                biomeLookups.incrementAndGet();
                return biome;
            }

            @Override
            public Optional<Biome> getBaseBiome(int x, int z, long seed) {
                return Optional.of(biome);
            }

            @Override
            public Iterable<Biome> getBiomes() {
                return Set.of(biome);
            }
        };

        CachingBiomeProvider provider = new CachingBiomeProvider(delegate);
        Column<Biome> column = provider.getColumn(1, 2, 3L, -5, 5);

        assertSame(biome, column.get(0));
        assertSame(biome, column.get(0));
        assertSame(biome, column.get(1));
        assertTrue(biomeLookups.get() <= 2);
    }

    private static final class SpecializedColumn implements Column<Biome> {
        private final int minY;
        private final int maxY;
        private final Biome biome;

        private SpecializedColumn(int minY, int maxY, Biome biome) {
            this.minY = minY;
            this.maxY = maxY;
            this.biome = biome;
        }

        @Override
        public int getMinY() {
            return minY;
        }

        @Override
        public int getMaxY() {
            return maxY;
        }

        @Override
        public int getX() {
            return 0;
        }

        @Override
        public int getZ() {
            return 0;
        }

        @Override
        public Biome get(int y) {
            return biome;
        }
    }

    private static final class TestBiome implements Biome {
        private final String id;
        private final Context context = new Context();

        private TestBiome(String id) {
            this.id = id;
        }

        @Override
        public PlatformBiome getPlatformBiome() {
            return null;
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
            return 0;
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
}
