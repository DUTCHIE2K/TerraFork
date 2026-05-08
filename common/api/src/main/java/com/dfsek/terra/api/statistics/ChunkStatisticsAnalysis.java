/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.statistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class ChunkStatisticsAnalysis {
    private ChunkStatisticsAnalysis() {
    }

    public static List<ChunkStatisticsSummary> summarize(List<ChunkStatistics> statistics) {
        Map<ChunkStatisticsGroup, List<ChunkStatistics>> grouped = new LinkedHashMap<>();
        statistics.forEach(statistic -> grouped.computeIfAbsent(new ChunkStatisticsGroup(statistic.platform(),
            statistic.packId(),
            statistic.window()), ignored -> new ArrayList<>()).add(statistic));

        return grouped.entrySet().stream()
            .map(entry -> summarize(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing((ChunkStatisticsSummary summary) -> summary.group().platform())
                .thenComparing(summary -> summary.group().packId())
                .thenComparing(summary -> summary.group().window()))
            .toList();
    }

    private static ChunkStatisticsSummary summarize(ChunkStatisticsGroup group, List<ChunkStatistics> statistics) {
        int sampleCount = statistics.size();
        Map<String, List<Long>> phaseValues = new LinkedHashMap<>();
        Map<String, FeatureBucket> featureBuckets = new LinkedHashMap<>();
        int successfulSamples = 0;
        int failedSamples = 0;

        List<Long> totalNanos = new ArrayList<>(sampleCount);
        List<Long> protoChunkReads = new ArrayList<>(sampleCount);
        List<Long> protoChunkWrites = new ArrayList<>(sampleCount);
        List<Long> worldBlockReads = new ArrayList<>(sampleCount);
        List<Long> worldBlockEntityReads = new ArrayList<>(sampleCount);
        List<Long> worldBlockWrites = new ArrayList<>(sampleCount);
        List<Long> entitySpawns = new ArrayList<>(sampleCount);
        List<Long> crossChunkReads = new ArrayList<>(sampleCount);
        List<Long> crossChunkWrites = new ArrayList<>(sampleCount);
        List<Long> maxReadChunkDistance = new ArrayList<>(sampleCount);
        List<Long> maxWriteChunkDistance = new ArrayList<>(sampleCount);
        List<Long> minTouchedY = new ArrayList<>(sampleCount);
        List<Long> maxTouchedY = new ArrayList<>(sampleCount);

        List<String> phaseKeys = orderedPhaseKeys(statistics);
        List<String> featureKeys = orderedFeatureKeys(statistics);

        for(ChunkStatistics statistic : statistics) {
            if(statistic.successful()) successfulSamples++;
            else failedSamples++;

            totalNanos.add(statistic.totalNanos());
            protoChunkReads.add(statistic.protoChunkReads());
            protoChunkWrites.add(statistic.protoChunkWrites());
            worldBlockReads.add(statistic.worldBlockReads());
            worldBlockEntityReads.add(statistic.worldBlockEntityReads());
            worldBlockWrites.add(statistic.worldBlockWrites());
            entitySpawns.add(statistic.entitySpawns());
            crossChunkReads.add(statistic.crossChunkReads());
            crossChunkWrites.add(statistic.crossChunkWrites());
            maxReadChunkDistance.add((long) statistic.maxReadChunkDistance());
            maxWriteChunkDistance.add((long) statistic.maxWriteChunkDistance());
            minTouchedY.add(statistic.minTouchedY() == null ? 0L : statistic.minTouchedY().longValue());
            maxTouchedY.add(statistic.maxTouchedY() == null ? 0L : statistic.maxTouchedY().longValue());

            phaseKeys.forEach(phase -> phaseValues.computeIfAbsent(phase, ignored -> new ArrayList<>(sampleCount))
                .add(statistic.phaseNanos().getOrDefault(phase, 0L)));

            featureKeys.forEach(feature -> {
                FeatureStatistics stats = statistic.featureStats().get(feature);
                FeatureBucket bucket = featureBuckets.computeIfAbsent(feature, ignored -> new FeatureBucket(sampleCount));
                bucket.evaluations().add(stats == null ? 0L : stats.evaluations());
                bucket.matches().add(stats == null ? 0L : stats.matches());
                bucket.placements().add(stats == null ? 0L : stats.placements());
            });
        }

        Map<String, StatisticalSummary> phases = new LinkedHashMap<>();
        phaseKeys.forEach(phase -> phases.put(phase, summarizeLongs(phaseValues.get(phase))));

        Map<String, FeatureActivitySummary> features = new LinkedHashMap<>();
        featureKeys.forEach(feature -> {
            FeatureBucket bucket = featureBuckets.get(feature);
            features.put(feature, new FeatureActivitySummary(summarizeLongs(bucket.evaluations()),
                summarizeLongs(bucket.matches()),
                summarizeLongs(bucket.placements())));
        });

        return new ChunkStatisticsSummary(group,
            successfulSamples,
            failedSamples,
            summarizeLongs(totalNanos),
            summarizeLongs(protoChunkReads),
            summarizeLongs(protoChunkWrites),
            summarizeLongs(worldBlockReads),
            summarizeLongs(worldBlockEntityReads),
            summarizeLongs(worldBlockWrites),
            summarizeLongs(entitySpawns),
            summarizeLongs(crossChunkReads),
            summarizeLongs(crossChunkWrites),
            summarizeLongs(maxReadChunkDistance),
            summarizeLongs(maxWriteChunkDistance),
            summarizeLongs(minTouchedY),
            summarizeLongs(maxTouchedY),
            phases,
            features);
    }

    public static StatisticalSummary summarizeLongs(List<Long> values) {
        if(values.isEmpty()) {
            return new StatisticalSummary(0, 0D, 0D, 0D, 0);
        }

        List<Long> sorted = values.stream().sorted().toList();
        int size = sorted.size();
        double average = values.stream().mapToDouble(Long::doubleValue).average().orElse(0D);
        double percentile95 = sorted.get(Math.min(size - 1, Math.max(0, (int) Math.ceil(size * 0.95D) - 1)));
        int tailCount = Math.max(1, (int) Math.ceil(size * 0.01D));
        double onePercentLowAverage = sorted.subList(size - tailCount, size).stream().mapToDouble(Long::doubleValue).average().orElse(0D);
        int nonZeroSamples = (int) values.stream().filter(value -> value != 0L).count();
        return new StatisticalSummary(size, average, percentile95, onePercentLowAverage, nonZeroSamples);
    }

    private static List<String> orderedPhaseKeys(List<ChunkStatistics> statistics) {
        return statistics.stream()
            .flatMap(statistic -> statistic.phaseNanos().keySet().stream())
            .distinct()
            .sorted()
            .toList();
    }

    private static List<String> orderedFeatureKeys(List<ChunkStatistics> statistics) {
        return statistics.stream()
            .flatMap(statistic -> statistic.featureStats().keySet().stream())
            .distinct()
            .sorted()
            .toList();
    }

    private record FeatureBucket(List<Long> evaluations, List<Long> matches, List<Long> placements) {
        private FeatureBucket(int expectedSize) {
            this(new ArrayList<>(expectedSize), new ArrayList<>(expectedSize), new ArrayList<>(expectedSize));
        }
    }
}
