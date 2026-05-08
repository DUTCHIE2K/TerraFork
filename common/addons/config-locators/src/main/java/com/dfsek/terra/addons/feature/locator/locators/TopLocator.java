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


public class TopLocator implements Locator {
    private final Range search;

    public TopLocator(Range search) {
        this.search = search;
    }

    @Override
    public BinaryColumn getSuitableCoordinates(Column<?> column) {
        int max = Math.min(search.getMax(), column.getMaxY() - 1);
        int min = Math.max(search.getMin(), column.getMinY());
        if(min > max) return BinaryColumn.getNull();

        boolean currentAir = column.getBlock(max).isAir();
        for(int y = max; y >= min; y--) {
            boolean belowAir = column.getBlock(y - 1).isAir();
            if(currentAir && !belowAir) {
                return new BinaryColumn(y, y + 1, yi -> true);
            }
            currentAir = belowAir;
        }
        return BinaryColumn.getNull();
    }
}
