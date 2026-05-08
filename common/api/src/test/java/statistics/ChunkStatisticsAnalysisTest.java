package statistics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import com.dfsek.terra.api.statistics.ChunkStatistics;
import com.dfsek.terra.api.statistics.ChunkStatisticsAnalysis;
import com.dfsek.terra.api.statistics.ChunkStatisticsSummary;
import com.dfsek.terra.api.statistics.FeatureStatistics;
import com.dfsek.terra.api.statistics.StatisticalSummary;

import static org.junit.jupiter.api.Assertions.assertEquals;


class ChunkStatisticsAnalysisTest {
    @Test
    void summarizesPercentilesAndTailAverage() {
        StatisticalSummary values = ChunkStatisticsAnalysis.summarizeLongs(List.of(1L, 2L, 3L, 4L, 5L, 100L));
        assertEquals(6, values.samples());
        assertEquals(6, values.nonZeroSamples());
        assertEquals(19.166666666666668D, values.average());
        assertEquals(100D, values.percentile95());
        assertEquals(100D, values.onePercentLowAverage());
    }

    @Test
    void groupsAndZeroFillsMissingPhaseAndFeatureSamples() {
        List<ChunkStatistics> statistics = List.of(
            new ChunkStatistics("CLI",
                "PACK",
                "pipeline",
                0,
                0,
                10,
                Map.of("chunk_base", 6L),
                Map.of("tree", new FeatureStatistics(2, 1, 1)),
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                1,
                2,
                10,
                20,
                true,
                null),
            new ChunkStatistics("CLI",
                "PACK",
                "pipeline",
                0,
                1,
                20,
                Map.of("stage:flora", 9L),
                Map.of(),
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                2,
                3,
                11,
                21,
                false,
                "failure")
        );

        ChunkStatisticsSummary summary = ChunkStatisticsAnalysis.summarize(statistics).get(0);
        assertEquals(1, summary.successfulSamples());
        assertEquals(1, summary.failedSamples());
        assertEquals(15D, summary.totalNanos().average());
        assertEquals(2, summary.phaseNanos().size());
        assertEquals(3D, summary.phaseNanos().get("chunk_base").average());
        assertEquals(1, summary.phaseNanos().get("chunk_base").nonZeroSamples());
        assertEquals(4.5D, summary.phaseNanos().get("stage:flora").average());
        assertEquals(1, summary.phaseNanos().get("stage:flora").nonZeroSamples());
        assertEquals(1D, summary.features().get("tree").evaluations().average());
        assertEquals(1, summary.features().get("tree").evaluations().nonZeroSamples());
        assertEquals(0.5D, summary.features().get("tree").matches().average());
        assertEquals(0.5D, summary.features().get("tree").placements().average());
    }
}
