package com.dfsek.terra.addons.commands.chunkstats;

import org.incendo.cloud.CommandManager;
import org.incendo.cloud.description.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.dfsek.terra.addons.manifest.api.AddonInitializer;
import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.addon.BaseAddon;
import com.dfsek.terra.api.command.CommandSender;
import com.dfsek.terra.api.event.events.platform.CommandRegistrationEvent;
import com.dfsek.terra.api.event.functional.FunctionalEventHandler;
import com.dfsek.terra.api.inject.annotations.Inject;
import com.dfsek.terra.api.statistics.ChunkStatisticsAnalysis;
import com.dfsek.terra.api.statistics.ChunkStatisticsCollector;
import com.dfsek.terra.api.statistics.ChunkStatisticsGroup;
import com.dfsek.terra.api.statistics.ChunkStatisticsSummary;
import com.dfsek.terra.api.statistics.FeatureActivitySummary;
import com.dfsek.terra.api.statistics.StatisticalSummary;
import com.dfsek.terra.api.statistics.ChunkStatisticsWindows;


public class ChunkStatisticsCommandAddon implements AddonInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ChunkStatisticsCommandAddon.class);
    private static final double MIN_ACTIVE_RATIO = 0.10D;
    private static final double MIN_PHASE_TIME_SHARE = 0.01D;
    private static final double MIN_FEATURE_EVALUATIONS = 0.25D;
    private static final double MIN_FEATURE_PLACEMENTS = 0.10D;
    private static final double SPARSE_COVERAGE_RATIO = 0.10D;
    private static final double PARTIAL_COVERAGE_RATIO = 0.50D;
    private static final int MAX_PHASE_LINES = 10;
    private static final int MAX_FEATURE_LINES = 8;

    @Inject
    private Platform platform;

    @Inject
    private BaseAddon addon;


    @Override
    public void initialize() {
        platform.getEventManager()
            .getHandler(FunctionalEventHandler.class)
            .register(addon, CommandRegistrationEvent.class)
            .then(event -> {
                CommandManager<CommandSender> manager = event.getCommandManager();
                manager
                    .command(
                        manager.commandBuilder("chunkstats", Description.of("Inspect chunk statistics"))
                            .literal("summary", Description.of("Summarize collected chunk statistics"), "sum")
                            .permission("terra.chunkstats.summary")
                            .handler(context -> {
                                String summary = renderSummary(platform.getChunkStatistics());
                                logger.info(summary);
                                if(context.sender().getPlayer().isPresent()) {
                                    context.sender().sendMessage("Chunk statistics summary dumped to console.");
                                } else {
                                    context.sender().sendMessage("Chunk statistics summary logged.");
                                }
                            }))
                    .command(
                        manager.commandBuilder("chunkstats", Description.of("Inspect chunk statistics"))
                            .literal("reset", Description.of("Reset collected chunk statistics"), "r")
                            .permission("terra.chunkstats.reset")
                            .handler(context -> {
                                platform.getChunkStatistics().reset();
                                context.sender().sendMessage("Chunk statistics reset.");
                            }));
            });
    }

    static String renderSummary(ChunkStatisticsCollector collector) {
        if(!collector.isEnabled()) {
            return "Chunk statistics collection is disabled. Enable debug.chunk-statistics in config.yml first.";
        }

        List<ChunkStatisticsSummary> summaries = ChunkStatisticsAnalysis.summarize(collector.getStatistics()).stream()
            .sorted(summaryComparator())
            .toList();
        if(summaries.isEmpty()) {
            return "No chunk statistics samples collected.";
        }

        Map<SummaryScope, CoverageReference> coverageReferences = buildCoverageReferences(summaries);

        StringBuilder builder = new StringBuilder("Chunk statistics summary");
        builder.append("\nWindow timings are per-window and not additive. Compare coverage before comparing averages.");
        builder.append("\nPhase timings are inclusive: parent phases include nested phase time.");
        builder.append("\nCoverage compares each window against the baseline window when available, otherwise the fullest observed window.");
        builder.append("\nObserved in = percentage of samples with non-zero activity.");
        builder.append("\nSlowest 1% average = average of the slowest 1% of samples.");
        summaries.forEach(summary -> appendSummary(builder, summary, coverageReferences.get(new SummaryScope(summary.group().platform(),
            summary.group().packId()))));
        return builder.toString();
    }

    private static void appendSummary(StringBuilder builder, ChunkStatisticsSummary summary, CoverageReference coverageReference) {
        builder.append("\n\n[")
            .append(summary.group().platform())
            .append(" | ")
            .append(summary.group().packId())
            .append(" | ")
            .append(summary.group().window())
            .append("] ")
            .append(summary.totalNanos().samples())
            .append(" samples");

        if(summary.failedSamples() > 0) {
            builder.append(" | ").append(summary.failedSamples()).append(" failed");
        }

        CoverageInfo coverage = describeCoverage(summary, coverageReference);
        if(coverage != null) {
            builder.append(" | ")
                .append(formatPercent(coverage.ratio()))
                .append(" coverage versus ")
                .append(coverage.label())
                .append(" (")
                .append(summary.totalNanos().samples())
                .append("/")
                .append(coverage.referenceSamples())
                .append(")");
            if(coverage.cue() != null) {
                builder.append(" | ").append(coverage.cue());
            }
        }

        builder.append("\nTime per sample: ").append(formatMillisSummary(summary.totalNanos()));
        builder.append("\nTotal sampled time: ").append(formatDurationMillis(totalMillis(summary.totalNanos())));
        appendAccessLine(builder, summary);
        appendPhases(builder, summary);
        appendFeatures(builder, summary);
    }

    private static void appendAccessLine(StringBuilder builder, ChunkStatisticsSummary summary) {
        List<String> countParts = new ArrayList<>();
        if(hasAnySignal(summary.protoChunkReads(), summary.protoChunkWrites())) {
            countParts.add("proto chunk reads " + format(summary.protoChunkReads().average())
                           + ", proto chunk writes "
                           + format(summary.protoChunkWrites().average()));
        }
        if(hasAnySignal(summary.worldBlockReads(), summary.worldBlockEntityReads(), summary.worldBlockWrites())) {
            countParts.add("world block reads " + format(summary.worldBlockReads().average())
                           + ", block-entity reads "
                           + format(summary.worldBlockEntityReads().average())
                           + ", world block writes "
                           + format(summary.worldBlockWrites().average()));
        }
        if(hasAnySignal(summary.crossChunkReads(), summary.crossChunkWrites())) {
            countParts.add("cross-chunk reads " + format(summary.crossChunkReads().average())
                           + ", cross-chunk writes "
                           + format(summary.crossChunkWrites().average()));
        }
        if(hasSignal(summary.entitySpawns())) {
            countParts.add("entities spawned " + format(summary.entitySpawns().average()));
        }

        if(!countParts.isEmpty()) {
            builder.append("\nAccess counts (average per sample): ").append(String.join(" | ", countParts));
        }

        List<String> boundParts = new ArrayList<>();
        if(hasSignal(summary.maxReadChunkDistance())) {
            boundParts.add("max read chunk distance " + format(summary.maxReadChunkDistance().average()));
        }
        if(hasSignal(summary.maxWriteChunkDistance())) {
            boundParts.add("max write chunk distance " + format(summary.maxWriteChunkDistance().average()));
        }
        if(hasAnySignal(summary.protoChunkReads(),
            summary.protoChunkWrites(),
            summary.worldBlockReads(),
            summary.worldBlockEntityReads(),
            summary.worldBlockWrites(),
            summary.entitySpawns())) {
            boundParts.add("touched y-range " + format(summary.minTouchedY().average()) + ".." + format(summary.maxTouchedY().average()));
        }

        if(!boundParts.isEmpty()) {
            builder.append("\nAccess bounds (average per sample): ").append(String.join(" | ", boundParts));
        }
    }

    private static void appendPhases(StringBuilder builder, ChunkStatisticsSummary summary) {
        List<Entry<String, StatisticalSummary>> ordered = summary.phaseNanos().entrySet().stream()
            .sorted(Entry.<String, StatisticalSummary>comparingByValue(Comparator.comparingDouble(StatisticalSummary::average)).reversed())
            .toList();

        if(ordered.isEmpty()) {
            return;
        }

        builder.append("\nTimed phases (inclusive):");
        int shown = 0;
        int hidden = 0;
        for(Entry<String, StatisticalSummary> entry : ordered) {
            boolean keep = shown == 0 || isPhaseSignal(entry.getValue(), summary.totalNanos());
            if(keep && shown < MAX_PHASE_LINES) {
                builder.append("\n - ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(formatMillisSummary(entry.getValue()))
                    .append(" | observed in ")
                    .append(formatPercent(entry.getValue().activeRatio()))
                    .append(" of samples");
                shown++;
            } else {
                hidden++;
            }
        }
        if(hidden > 0) {
            builder.append("\n - +").append(hidden).append(" additional phases hidden");
        }
    }

    private static void appendFeatures(StringBuilder builder, ChunkStatisticsSummary summary) {
        if(summary.features().isEmpty()) {
            return;
        }

        List<Entry<String, FeatureActivitySummary>> ordered = summary.features().entrySet().stream()
            .sorted((left, right) -> Double.compare(featureSortValue(right.getValue()), featureSortValue(left.getValue())))
            .toList();

        builder.append("\nFeature activity (average counts per sample):");
        int shown = 0;
        int hidden = 0;
        for(Entry<String, FeatureActivitySummary> entry : ordered) {
            boolean keep = shown == 0 || isFeatureSignal(entry.getValue());
            if(keep && shown < MAX_FEATURE_LINES) {
                appendFeatureLine(builder, entry.getKey(), entry.getValue());
                shown++;
            } else {
                hidden++;
            }
        }
        if(hidden > 0) {
            builder.append("\n - +").append(hidden).append(" additional features hidden");
        }
    }

    private static void appendFeatureLine(StringBuilder builder, String feature, FeatureActivitySummary summary) {
        builder.append("\n - ")
            .append(feature)
            .append(": evaluations ")
            .append(format(summary.evaluations().average()))
            .append(", matches ")
            .append(format(summary.matches().average()))
            .append(", placements ")
            .append(format(summary.placements().average()))
            .append(" | observed in ")
            .append(formatPercent(featureActiveRatio(summary)))
            .append(" of samples");
    }

    private static boolean isPhaseSignal(StatisticalSummary phase, StatisticalSummary total) {
        if(!hasSignal(phase)) {
            return false;
        }
        double totalAverage = total.average();
        double share = totalAverage == 0D ? 0D : phase.average() / totalAverage;
        return phase.activeRatio() >= MIN_ACTIVE_RATIO || share >= MIN_PHASE_TIME_SHARE;
    }

    private static boolean isFeatureSignal(FeatureActivitySummary summary) {
        return featureActiveRatio(summary) >= MIN_ACTIVE_RATIO
               || summary.evaluations().average() >= MIN_FEATURE_EVALUATIONS
               || summary.placements().average() >= MIN_FEATURE_PLACEMENTS;
    }

    private static double featureSortValue(FeatureActivitySummary summary) {
        return Math.max(summary.evaluations().average(), Math.max(summary.matches().average(), summary.placements().average()));
    }

    private static double featureActiveRatio(FeatureActivitySummary summary) {
        int samples = summary.evaluations().samples();
        if(samples == 0) {
            return 0D;
        }
        int activeSamples = Math.max(summary.evaluations().nonZeroSamples(),
            Math.max(summary.matches().nonZeroSamples(), summary.placements().nonZeroSamples()));
        return (double) activeSamples / samples;
    }

    private static boolean hasAnySignal(StatisticalSummary... summaries) {
        for(StatisticalSummary summary : summaries) {
            if(hasSignal(summary)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSignal(StatisticalSummary summary) {
        return summary.nonZeroSamples() > 0 || summary.average() != 0D;
    }

    private static String formatMillisSummary(StatisticalSummary summary) {
        return format(summary.average() / 1_000_000D)
               + "ms average / "
               + format(summary.percentile95() / 1_000_000D)
               + "ms 95th percentile / "
               + format(summary.onePercentLowAverage() / 1_000_000D)
               + "ms slowest 1% average";
    }

    private static String formatPercent(double value) {
        return format(value * 100D) + "%";
    }

    private static double totalMillis(StatisticalSummary summary) {
        return (summary.average() * summary.samples()) / 1_000_000D;
    }

    private static String formatDurationMillis(double millis) {
        if(millis >= 1000D) {
            return format(millis / 1000D) + "s";
        }
        return format(millis) + "ms";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static Comparator<ChunkStatisticsSummary> summaryComparator() {
        return Comparator.comparing((ChunkStatisticsSummary summary) -> summary.group().platform())
            .thenComparing(summary -> summary.group().packId())
            .thenComparingInt(summary -> windowOrder(summary.group().window()))
            .thenComparing(summary -> summary.group().window());
    }

    private static int windowOrder(String window) {
        return switch(window) {
            case ChunkStatisticsWindows.BASE -> 0;
            case ChunkStatisticsWindows.BEARD -> 1;
            case ChunkStatisticsWindows.STAGES -> 2;
            case ChunkStatisticsWindows.HEIGHT -> 3;
            case ChunkStatisticsWindows.COLUMN -> 4;
            case ChunkStatisticsWindows.PIPELINE -> 5;
            default -> Integer.MAX_VALUE;
        };
    }

    private static Map<SummaryScope, CoverageReference> buildCoverageReferences(List<ChunkStatisticsSummary> summaries) {
        Map<SummaryScope, CoverageReference> references = new HashMap<>();
        for(ChunkStatisticsSummary summary : summaries) {
            SummaryScope scope = new SummaryScope(summary.group().platform(), summary.group().packId());
            CoverageReference current = references.get(scope);
            int samples = summary.totalNanos().samples();
            boolean isBase = ChunkStatisticsWindows.BASE.equals(summary.group().window());
            if(current == null || (isBase && !current.fromBase()) || (!current.fromBase() && samples > current.samples())) {
                references.put(scope, new CoverageReference(summary.group(), samples, isBase));
            }
        }
        return references;
    }

    private static CoverageInfo describeCoverage(ChunkStatisticsSummary summary, CoverageReference reference) {
        if(reference == null || reference.samples() == 0) {
            return null;
        }
        double ratio = (double) summary.totalNanos().samples() / reference.samples();
        String cue = null;
        if(ratio < SPARSE_COVERAGE_RATIO) {
            cue = "sparse";
        } else if(ratio < PARTIAL_COVERAGE_RATIO) {
            cue = "partial";
        }
        return new CoverageInfo(reference.fromBase() ? "baseline window" : "fullest observed window",
            reference.samples(),
            ratio,
            cue == null ? null : cue + " coverage");
    }

    private record SummaryScope(String platform, String packId) {
    }

    private record CoverageReference(ChunkStatisticsGroup group, int samples, boolean fromBase) {
    }

    private record CoverageInfo(String label, int referenceSamples, double ratio, String cue) {
    }
}
