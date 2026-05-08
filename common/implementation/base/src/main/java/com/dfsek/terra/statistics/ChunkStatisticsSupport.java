/*
 * This file is part of Terra.
 *
 * Terra is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terra is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terra.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dfsek.terra.statistics;

import java.util.function.Supplier;

import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.statistics.ChunkStatisticsCollector;
import com.dfsek.terra.api.statistics.ChunkStatisticsPhases;
import com.dfsek.terra.api.statistics.ChunkStatisticsSession;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.generation.ChunkGenerator;
import com.dfsek.terra.api.world.chunk.generation.ProtoChunk;
import com.dfsek.terra.api.world.chunk.generation.ProtoWorld;
import com.dfsek.terra.api.world.chunk.generation.stage.GenerationStage;
import com.dfsek.terra.api.world.info.WorldProperties;


public final class ChunkStatisticsSupport {
    private ChunkStatisticsSupport() {
    }

    public static ChunkStatisticsSession beginWindow(Platform platform, ConfigPack pack, String window, int chunkX, int chunkZ) {
        return platform.getChunkStatistics().beginChunk(platform.platformName(), pack.getID(), window, chunkX, chunkZ);
    }

    @SuppressWarnings("try")
    public static <T> T measureWindow(Platform platform,
                                      ConfigPack pack,
                                      String window,
                                      String phase,
                                      int chunkX,
                                      int chunkZ,
                                      Supplier<T> supplier) {
        ChunkStatisticsCollector collector = platform.getChunkStatistics();
        ChunkStatisticsSession session = beginWindow(platform, pack, window, chunkX, chunkZ);
        try(session) {
            try(ChunkStatisticsSession.Activation activation = session.activate()) {
                collector.pushPhase(phase);
                try {
                    return supplier.get();
                } finally {
                    collector.popPhase(phase);
                }
            } catch(RuntimeException | Error e) {
                session.fail(e);
                throw e;
            }
        }
    }

    public static void measureWindow(Platform platform,
                                     ConfigPack pack,
                                     String window,
                                     String phase,
                                     int chunkX,
                                     int chunkZ,
                                     Runnable runnable) {
        measureWindow(platform, pack, window, phase, chunkX, chunkZ, () -> {
            runnable.run();
            return null;
        });
    }

    public static void generateBase(Platform platform,
                                    ChunkGenerator generator,
                                    ProtoChunk chunk,
                                    WorldProperties world,
                                    BiomeProvider biomeProvider,
                                    int chunkX,
                                    int chunkZ) {
        ChunkStatisticsCollector collector = platform.getChunkStatistics();
        ProtoChunk meteredChunk = collector.wrap(chunk);
        collector.pushPhase(ChunkStatisticsPhases.CHUNK_BASE);
        try {
            generator.generateChunkData(meteredChunk, world, biomeProvider, chunkX, chunkZ);
        } finally {
            collector.popPhase(ChunkStatisticsPhases.CHUNK_BASE);
        }
    }

    public static void runStages(Platform platform, Iterable<GenerationStage> stages, ProtoWorld world) {
        ChunkStatisticsCollector collector = platform.getChunkStatistics();
        ProtoWorld meteredWorld = collector.wrap(world);
        for(GenerationStage stage : stages) {
            runStage(platform, stage, meteredWorld);
        }
    }

    public static void runStage(Platform platform, GenerationStage stage, ProtoWorld world) {
        ChunkStatisticsCollector collector = platform.getChunkStatistics();
        String phase = ChunkStatisticsPhases.stage(stage);
        collector.pushPhase(phase);
        try {
            stage.populate(world);
        } finally {
            collector.popPhase(phase);
        }
    }
}
