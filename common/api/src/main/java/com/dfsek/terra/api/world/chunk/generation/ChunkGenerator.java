/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.world.chunk.generation;

import com.dfsek.seismic.type.vector.Vector3;
import com.dfsek.seismic.type.vector.Vector3Int;
import org.jetbrains.annotations.NotNull;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.generation.util.Palette;
import com.dfsek.terra.api.world.info.WorldProperties;


public interface ChunkGenerator {
    void generateChunkData(@NotNull ProtoChunk chunk, @NotNull WorldProperties world, @NotNull BiomeProvider biomeProvider,
                           int chunkX, int chunkZ);

    default GeneratedColumn getColumn(@NotNull WorldProperties world, int x, int z, @NotNull BiomeProvider biomeProvider) {
        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight();
        BlockState[] states = new BlockState[maxHeight - minHeight];
        for(int y = minHeight; y < maxHeight; y++) {
            states[y - minHeight] = getBlock(world, x, y, z, biomeProvider);
        }
        return new GeneratedColumn(minHeight, states);
    }

    BlockState getBlock(WorldProperties world, int x, int y, int z, BiomeProvider biomeProvider);

    default BlockState getBlock(WorldProperties world, Vector3 vector3, BiomeProvider biomeProvider) {
        return getBlock(world, vector3.getFloorX(), vector3.getFloorY(), vector3.getFloorZ(), biomeProvider);
    }

    default BlockState getBlock(WorldProperties world, Vector3Int vector3, BiomeProvider biomeProvider) {
        return getBlock(world, vector3.getX(), vector3.getY(), vector3.getZ(), biomeProvider);
    }

    Palette getPalette(int x, int y, int z, WorldProperties world, BiomeProvider biomeProvider);
}
