/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.world.chunk.generation;

import org.jetbrains.annotations.NotNull;

import com.dfsek.terra.api.block.state.BlockState;


public final class GeneratedColumn {
    private final int minY;
    private final BlockState[] states;

    public GeneratedColumn(int minY, @NotNull BlockState[] states) {
        this.minY = minY;
        this.states = states;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return minY + states.length;
    }

    public int getHeight() {
        return states.length;
    }

    public @NotNull BlockState getBlock(int y) {
        return states[y - minY];
    }

    public @NotNull BlockState[] getBlockStates() {
        return states;
    }
}
