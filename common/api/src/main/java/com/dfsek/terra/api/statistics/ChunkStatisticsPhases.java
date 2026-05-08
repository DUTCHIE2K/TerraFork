/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.statistics;

import com.dfsek.terra.api.registry.key.StringIdentifiable;
import com.dfsek.terra.api.world.chunk.generation.stage.GenerationStage;


public final class ChunkStatisticsPhases {
    public static final String CHUNK_BASE = "chunk_base";
    public static final String HEIGHT = "height";
    public static final String COLUMN = "column";
    public static final String BEARD = "beard";

    private ChunkStatisticsPhases() {
    }

    public static String stage(GenerationStage stage) {
        if(stage instanceof StringIdentifiable identifiable) {
            return "stage:" + identifiable.getID();
        }
        String simpleName = stage.getClass().getSimpleName();
        if(simpleName.isEmpty()) {
            simpleName = stage.getClass().getName();
        }
        return "stage:" + simpleName;
    }

    public static String feature(String featureId) {
        return "feature:" + featureId;
    }

    public static String terraScript(String scriptId) {
        return "terrascript:" + scriptId;
    }
}
