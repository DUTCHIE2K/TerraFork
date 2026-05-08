Chunk statistics report
[13:50:01 INFO]: [com.dfsek.terra.addons.commands.chunkstats.ChunkStatisticsCommandAddon] Chunk statistics summary
Window timings are per-window and not additive. Compare coverage before comparing averages.
Phase timings are inclusive: parent phases include nested phase time.
Coverage compares each window against the baseline window when available, otherwise the fullest observed window.
Observed in = percentage of samples with non-zero activity.
Slowest 1% average = average of the slowest 1% of samples.

[Bukkit | OVERWORLD | base] 17915 samples | 100.00% coverage versus baseline window (17915/17915)
Time per sample: 28.00ms average / 47.59ms 95th percentile / 69.61ms slowest 1% average
Total sampled time: 501.64s
Access counts (average per sample): proto chunk reads 0.00, proto chunk writes 33268.31
Access bounds (average per sample): touched y-range -64.00..80.14
Timed phases (inclusive):
 - chunk_base: 28.00ms average / 47.58ms 95th percentile / 69.60ms slowest 1% average | observed in 100.00% of samples

[Bukkit | OVERWORLD | beard] 17915 samples | 100.00% coverage versus baseline window (17915/17915)
Time per sample: 1.07ms average / 2.29ms 95th percentile / 34.32ms slowest 1% average
Total sampled time: 19.16s
Timed phases (inclusive):
 - beard: 1.07ms average / 2.29ms 95th percentile / 34.32ms slowest 1% average | observed in 100.00% of samples

[Bukkit | OVERWORLD | stages] 16567 samples | 92.48% coverage versus baseline window (16567/17915)
Time per sample: 14.24ms average / 34.60ms 95th percentile / 90.17ms slowest 1% average
Total sampled time: 235.97s
Access counts (average per sample): world block reads 124051.38, block-entity reads 0.00, world block writes 1298.18 | cross-chunk reads 1402.84, cross-chunk writes 169.75
Access bounds (average per sample): max read chunk distance 1.00 | max write chunk distance 1.00 | touched y-range -64.00..318.61
Timed phases (inclusive):
 - stage:global-preprocessors: 4.14ms average / 5.25ms 95th percentile / 66.15ms slowest 1% average | observed in 100.00% of samples
 - stage:flora: 2.39ms average / 18.02ms 95th percentile / 28.91ms slowest 1% average | observed in 100.00% of samples
 - stage:underwater-flora: 2.06ms average / 2.54ms 95th percentile / 4.82ms slowest 1% average | observed in 100.00% of samples
 - stage:trees: 1.94ms average / 17.98ms 95th percentile / 35.66ms slowest 1% average | observed in 100.00% of samples
 - stage:ores: 1.23ms average / 2.07ms 95th percentile / 7.40ms slowest 1% average | observed in 100.00% of samples
 - feature:CONTAIN_FLOATING_WATER: 1.14ms average / 1.78ms 95th percentile / 3.80ms slowest 1% average | observed in 100.00% of samples
 - feature:CONTAIN_FLOATING_WATER_DEEPSLATE: 1.08ms average / 1.70ms 95th percentile / 3.59ms slowest 1% average | observed in 100.00% of samples
 - stage:landforms: 0.90ms average / 2.54ms 95th percentile / 38.94ms slowest 1% average | observed in 100.00% of samples
 - feature:CLINGING_KELP: 0.83ms average / 1.05ms 95th percentile / 2.21ms slowest 1% average | observed in 95.08% of samples
 - stage:deposits: 0.62ms average / 0.98ms 95th percentile / 2.33ms slowest 1% average | observed in 100.00% of samples
 - +370 additional phases hidden
Feature activity (average counts per sample):
 - CAVE_GLOW_LICHEN: evaluations 381.34, matches 38.16, placements 0.79 | observed in 100.00% of samples
 - AMETHYST_GEODES: evaluations 380.75, matches 0.01, placements 0.00 | observed in 100.00% of samples
 - ANDESITE_DEPOSITS: evaluations 380.75, matches 2.98, placements 2.00 | observed in 100.00% of samples
 - CAVE_CARVERS: evaluations 380.75, matches 0.04, placements 0.00 | observed in 100.00% of samples
 - CAVE_ENTRANCES: evaluations 380.75, matches 0.31, placements 0.01 | observed in 100.00% of samples
 - COAL_ORE: evaluations 380.75, matches 14.86, placements 9.99 | observed in 100.00% of samples
 - COAL_ORE_UNIFORM: evaluations 380.75, matches 22.32, placements 15.02 | observed in 100.00% of samples
 - CONTAIN_FLOATING_WATER: evaluations 380.75, matches 380.75, placements 0.76 | observed in 100.00% of samples
 - +195 additional features hidden

[Bukkit | OVERWORLD | height] 4332 samples | 24.18% coverage versus baseline window (4332/17915) | partial coverage
Time per sample: 9.00ms average / 58.57ms 95th percentile / 97.08ms slowest 1% average
Total sampled time: 39.00s
Timed phases (inclusive):
 - height: 9.00ms average / 58.56ms 95th percentile / 97.08ms slowest 1% average | observed in 100.00% of samples

[Bukkit | OVERWORLD | column] 328 samples | 1.83% coverage versus baseline window (328/17915) | sparse coverage
Time per sample: 1.54ms average / 2.97ms 95th percentile / 38.57ms slowest 1% average
Total sampled time: 504.40ms
Timed phases (inclusive):
 - column: 1.54ms average / 2.96ms 95th percentile / 38.57ms slowest 1% average | observed in 100.00% of samples