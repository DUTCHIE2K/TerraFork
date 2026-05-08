package com.dfsek.terra.addons.commands.chunkstats;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dfsek.terra.api.statistics.ChunkStatistics;
import com.dfsek.terra.api.statistics.ChunkStatisticsCollector;
import com.dfsek.terra.api.statistics.ChunkStatisticsSession;
import com.dfsek.terra.api.statistics.FeatureStatistics;
import com.dfsek.terra.api.world.chunk.generation.ProtoChunk;
import com.dfsek.terra.api.world.chunk.generation.ProtoWorld;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ChunkStatisticsCommandAddonTest {
    @Test
    void renderSummaryHidesLowSignalPhasesAndFeatures() {
        List<ChunkStatistics> statistics = new ArrayList<>();
        for(int i = 0; i < 20; i++) {
            Map<String, Long> phases = i == 0
                ? Map.of("stage:flora", 8_000_000L, "feature:rare", 1_000L)
                : Map.of("stage:flora", 8_000_000L);
            Map<String, FeatureStatistics> features = i == 0
                ? Map.of("tree", new FeatureStatistics(8, 4, 2), "rare", new FeatureStatistics(1, 1, 1))
                : Map.of("tree", new FeatureStatistics(8, 4, 2));
            statistics.add(new ChunkStatistics("CLI",
                "PACK",
                "stages",
                0,
                i,
                10_000_000L,
                phases,
                features,
                0,
                0,
                120,
                0,
                20,
                0,
                5,
                0,
                1,
                0,
                64,
                96,
                true,
                null));
        }

        String summary = ChunkStatisticsCommandAddon.renderSummary(new TestCollector(statistics));

        assertTrue(summary.contains("[CLI | PACK | stages] 20 samples | 100.00% coverage versus fullest observed window (20/20)"));
        assertTrue(summary.contains("Total sampled time: 200.00ms"));
        assertTrue(summary.contains("Time per sample: 10.00ms average / 10.00ms 95th percentile / 10.00ms slowest 1% average"));
        assertTrue(summary.contains("Feature activity (average counts per sample):"));
        assertTrue(summary.contains("stage:flora"));
        assertTrue(summary.contains("+1 additional phases hidden"));
        assertTrue(summary.contains("+1 additional features hidden"));
        assertFalse(summary.contains("feature:rare"));
        assertFalse(summary.contains("\n - rare:"));
    }

    @Test
    void renderSummaryShowsCoverageAgainstBaseAndSparseCue() {
        List<ChunkStatistics> statistics = new ArrayList<>();
        for(int i = 0; i < 20; i++) {
            statistics.add(new ChunkStatistics("CLI",
                "PACK",
                "base",
                0,
                i,
                10_000_000L,
                Map.of("chunk_base", 10_000_000L),
                Map.of(),
                0,
                10,
                20,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                64,
                96,
                true,
                null));
        }
        statistics.add(new ChunkStatistics("CLI",
            "PACK",
            "column",
            0,
            0,
            2_000_000L,
            Map.of("column", 2_000_000L),
            Map.of(),
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            null,
            null,
            true,
            null));

        String summary = ChunkStatisticsCommandAddon.renderSummary(new TestCollector(statistics));

        assertTrue(summary.contains("[CLI | PACK | base] 20 samples | 100.00% coverage versus baseline window (20/20)"));
        assertTrue(summary.contains("[CLI | PACK | column] 1 samples | 5.00% coverage versus baseline window (1/20) | sparse coverage"));
    }

    private record TestCollector(List<ChunkStatistics> statistics) implements ChunkStatisticsCollector {
        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public ChunkStatisticsSession beginChunk(String platform, String packId, String window, int chunkX, int chunkZ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProtoChunk wrap(ProtoChunk chunk) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProtoWorld wrap(ProtoWorld world) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void pushPhase(String phase) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void popPhase(String phase) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void recordFeatureEvaluation(String featureId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void recordFeatureMatch(String featureId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void recordFeaturePlacement(String featureId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ChunkStatistics> getStatistics() {
            return statistics;
        }

        @Override
        public void reset() {
            throw new UnsupportedOperationException();
        }
    }
}
