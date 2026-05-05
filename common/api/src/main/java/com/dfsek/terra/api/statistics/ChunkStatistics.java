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


public record ChunkStatistics(
    String platform,
    String packId,
    String window,
    int chunkX,
    int chunkZ,
    long totalNanos,
    Map<String, Long> phaseNanos,
    Map<String, FeatureStatistics> featureStats,
    long protoChunkReads,
    long protoChunkWrites,
    long worldBlockReads,
    long worldBlockEntityReads,
    long worldBlockWrites,
    long entitySpawns,
    long crossChunkReads,
    long crossChunkWrites,
    int maxReadChunkDistance,
    int maxWriteChunkDistance,
    Integer minTouchedY,
    Integer maxTouchedY,
    boolean successful,
    String failure
) {
    public ChunkStatistics {
        phaseNanos = Collections.unmodifiableMap(new LinkedHashMap<>(phaseNanos));
        featureStats = Collections.unmodifiableMap(new LinkedHashMap<>(featureStats));
    }
}
