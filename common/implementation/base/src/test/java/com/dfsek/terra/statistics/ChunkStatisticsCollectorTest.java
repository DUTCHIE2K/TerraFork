package com.dfsek.terra.statistics;

import com.dfsek.seismic.type.vector.Vector3;
import org.junit.jupiter.api.Test;

import com.dfsek.terra.api.block.BlockType;
import com.dfsek.terra.api.block.entity.BlockEntity;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.block.state.properties.Property;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.entity.Entity;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.statistics.ChunkStatistics;
import com.dfsek.terra.api.statistics.ChunkStatisticsPhases;
import com.dfsek.terra.api.statistics.ChunkStatisticsSession;
import com.dfsek.terra.api.statistics.ChunkStatisticsWindows;
import com.dfsek.terra.api.world.ServerWorld;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.generation.ChunkGenerator;
import com.dfsek.terra.api.world.chunk.generation.ProtoChunk;
import com.dfsek.terra.api.world.chunk.generation.ProtoWorld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ChunkStatisticsCollectorTest {
    private static final BlockState BLOCK_STATE = new TestBlockState();

    @Test
    @SuppressWarnings("try")
    void collectsChunkAndWorldStatistics() {
        ChunkStatisticsCollectorImpl collector = new ChunkStatisticsCollectorImpl(8);
        collector.setEnabled(true);

        ChunkStatisticsSession session = collector.beginChunk("Test", "PACK", ChunkStatisticsWindows.PIPELINE, 4, 5);
        try(session) {
            try(ChunkStatisticsSession.Activation activation = session.activate()) {
                ProtoChunk chunk = collector.wrap(new TestProtoChunk());
                collector.pushPhase(ChunkStatisticsPhases.CHUNK_BASE);
                chunk.setBlock(0, 64, 0, BLOCK_STATE);
                chunk.getBlock(0, 63, 0);
                collector.popPhase(ChunkStatisticsPhases.CHUNK_BASE);

                ProtoWorld world = collector.wrap(new TestProtoWorld(4, 5));
                collector.pushPhase("stage:TEST");
                world.getBlockState(64, 70, 80);
                world.getBlockEntity(80, 71, 80);
                world.setBlockState(63, 60, 80, BLOCK_STATE, false);
                world.spawnEntity(95.1, 61.2, 80.0, new TestEntityType());
                collector.popPhase("stage:TEST");

                collector.recordFeatureEvaluation("tree");
                collector.recordFeatureMatch("tree");
                collector.recordFeaturePlacement("tree");
            }
        }

        ChunkStatistics statistics = collector.getStatistics().get(0);
        assertEquals("Test", statistics.platform());
        assertEquals("PACK", statistics.packId());
        assertEquals(ChunkStatisticsWindows.PIPELINE, statistics.window());
        assertEquals(1, statistics.protoChunkWrites());
        assertEquals(1, statistics.protoChunkReads());
        assertEquals(1, statistics.worldBlockReads());
        assertEquals(1, statistics.worldBlockEntityReads());
        assertEquals(1, statistics.worldBlockWrites());
        assertEquals(1, statistics.entitySpawns());
        assertEquals(1, statistics.crossChunkReads());
        assertEquals(2, statistics.crossChunkWrites());
        assertEquals(1, statistics.maxReadChunkDistance());
        assertEquals(1, statistics.maxWriteChunkDistance());
        assertEquals(Integer.valueOf(60), statistics.minTouchedY());
        assertEquals(Integer.valueOf(71), statistics.maxTouchedY());
        assertTrue(statistics.successful());
        assertTrue(statistics.phaseNanos().containsKey(ChunkStatisticsPhases.CHUNK_BASE));
        assertTrue(statistics.phaseNanos().containsKey("stage:TEST"));
        assertEquals(1, statistics.featureStats().get("tree").evaluations());
        assertEquals(1, statistics.featureStats().get("tree").matches());
        assertEquals(1, statistics.featureStats().get("tree").placements());
    }

    private static final class TestProtoChunk implements ProtoChunk {
        @Override
        public void setBlock(int x, int y, int z, BlockState blockState) {
        }

        @Override
        public BlockState getBlock(int x, int y, int z) {
            return BLOCK_STATE;
        }

        @Override
        public int getMaxHeight() {
            return 320;
        }

        @Override
        public Object getHandle() {
            return this;
        }
    }

    private static final class TestProtoWorld implements ProtoWorld {
        private final int chunkX;
        private final int chunkZ;

        private TestProtoWorld(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public Object getHandle() {
            return this;
        }

        @Override
        public BlockState getBlockState(int x, int y, int z) {
            return BLOCK_STATE;
        }

        @Override
        public BlockEntity getBlockEntity(int x, int y, int z) {
            return new TestBlockEntity(x, y, z);
        }

        @Override
        public long getSeed() {
            return 1L;
        }

        @Override
        public int getMaxHeight() {
            return 320;
        }

        @Override
        public int getMinHeight() {
            return -64;
        }

        @Override
        public ChunkGenerator getGenerator() {
            return null;
        }

        @Override
        public BiomeProvider getBiomeProvider() {
            return null;
        }

        @Override
        public ConfigPack getPack() {
            return null;
        }

        @Override
        public void setBlockState(int x, int y, int z, BlockState data, boolean physics) {
        }

        @Override
        public Entity spawnEntity(double x, double y, double z, EntityType entityType) {
            return new TestEntity();
        }

        @Override
        public int centerChunkX() {
            return chunkX;
        }

        @Override
        public int centerChunkZ() {
            return chunkZ;
        }

        @Override
        public ServerWorld getWorld() {
            return null;
        }
    }

    private static final class TestBlockEntity implements BlockEntity {
        private final int x;
        private final int y;
        private final int z;

        private TestBlockEntity(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean update(boolean applyPhysics) {
            return false;
        }

        @Override
        public Vector3 getPosition() {
            return Vector3.of(x, y, z);
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public int getZ() {
            return z;
        }

        @Override
        public BlockState getBlockState() {
            return BLOCK_STATE;
        }

        @Override
        public Object getHandle() {
            return this;
        }
    }

    private static final class TestEntity implements Entity {
        private Vector3 position = Vector3.of(0, 0, 0);

        @Override
        public Vector3 position() {
            return position;
        }

        @Override
        public void position(Vector3 position) {
            this.position = position;
        }

        @Override
        public void world(ServerWorld world) {
        }

        @Override
        public ServerWorld world() {
            return null;
        }

        @Override
        public Object getHandle() {
            return this;
        }
    }

    private static final class TestEntityType implements EntityType {
        @Override
        public Object getHandle() {
            return this;
        }
    }

    private static final class TestBlockState implements BlockState {
        @Override
        public boolean matches(BlockState other) {
            return other == this;
        }

        @Override
        public <T extends Comparable<T>> boolean has(Property<T> property) {
            return false;
        }

        @Override
        public <T extends Comparable<T>> T get(Property<T> property) {
            return null;
        }

        @Override
        public <T extends Comparable<T>> BlockState set(Property<T> property, T value) {
            return this;
        }

        @Override
        public BlockType getBlockType() {
            return new BlockType() {
                @Override
                public BlockState getDefaultState() {
                    return BLOCK_STATE;
                }

                @Override
                public boolean isSolid() {
                    return true;
                }

                @Override
                public boolean isWater() {
                    return false;
                }

                @Override
                public Object getHandle() {
                    return this;
                }
            };
        }

        @Override
        public String getAsString(boolean properties) {
            return "test:block";
        }

        @Override
        public boolean isAir() {
            return false;
        }

        @Override
        public Object getHandle() {
            return this;
        }
    }
}
