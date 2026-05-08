/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.statistics;


public record StatisticalSummary(int samples, double average, double percentile95, double onePercentLowAverage, int nonZeroSamples) {
    public double activeRatio() {
        if(samples == 0) {
            return 0D;
        }
        return (double) nonZeroSamples / samples;
    }
}
