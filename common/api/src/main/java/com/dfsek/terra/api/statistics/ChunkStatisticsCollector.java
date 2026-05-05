/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.statistics;

import java.util.List;

import com.dfsek.terra.api.world.chunk.generation.ProtoChunk;
import com.dfsek.terra.api.world.chunk.generation.ProtoWorld;


public interface ChunkStatisticsCollector {
    boolean isEnabled();

    ChunkStatisticsSession beginChunk(String platform, String packId, String window, int chunkX, int chunkZ);

    ProtoChunk wrap(ProtoChunk chunk);

    ProtoWorld wrap(ProtoWorld world);

    void pushPhase(String phase);

    void popPhase(String phase);

    void recordFeatureEvaluation(String featureId);

    void recordFeatureMatch(String featureId);

    void recordFeaturePlacement(String featureId);

    List<ChunkStatistics> getStatistics();

    void reset();
}
