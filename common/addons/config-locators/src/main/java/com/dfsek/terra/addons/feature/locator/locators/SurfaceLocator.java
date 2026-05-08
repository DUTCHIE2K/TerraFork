/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package com.dfsek.terra.addons.feature.locator.locators;

import com.dfsek.terra.api.structure.feature.BinaryColumn;
import com.dfsek.terra.api.structure.feature.Locator;
import com.dfsek.terra.api.util.range.Range;
import com.dfsek.terra.api.world.chunk.generation.util.Column;
import com.dfsek.terra.api.world.chunk.generation.util.Column.BinaryColumnBuilder;


public class SurfaceLocator implements Locator {
    private final Range search;

    public SurfaceLocator(Range search) {
        this.search = search;
    }

    @Override
    public BinaryColumn getSuitableCoordinates(Column<?> column) {
        BinaryColumnBuilder builder = column.newBinaryColumn();
        int max = Math.min(search.getMax(), column.getMaxY());
        int min = Math.max(search.getMin(), column.getMinY());
        if(min >= max) return builder.build();
        boolean previousAir = column.getBlock(min - 1).isAir();
        for(int y = min; y < max; y++) {
            boolean currentAir = column.getBlock(y).isAir();
            if(currentAir && !previousAir) {
                builder.set(y);
            }
            previousAir = currentAir;
        }
        return builder.build();
    }
}
