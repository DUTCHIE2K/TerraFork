/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.statistics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public record ChunkStatisticsSummary(
    ChunkStatisticsGroup group,
    int successfulSamples,
    int failedSamples,
    StatisticalSummary totalNanos,
    StatisticalSummary protoChunkReads,
    StatisticalSummary protoChunkWrites,
    StatisticalSummary worldBlockReads,
    StatisticalSummary worldBlockEntityReads,
    StatisticalSummary worldBlockWrites,
    StatisticalSummary entitySpawns,
    StatisticalSummary crossChunkReads,
    StatisticalSummary crossChunkWrites,
    StatisticalSummary maxReadChunkDistance,
    StatisticalSummary maxWriteChunkDistance,
    StatisticalSummary minTouchedY,
    StatisticalSummary maxTouchedY,
    Map<String, StatisticalSummary> phaseNanos,
    Map<String, FeatureActivitySummary> features
) {
    public ChunkStatisticsSummary {
        phaseNanos = Collections.unmodifiableMap(new LinkedHashMap<>(phaseNanos));
        features = Collections.unmodifiableMap(new LinkedHashMap<>(features));
    }
}
