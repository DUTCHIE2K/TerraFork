package com.dfsek.terra.addons.commands.chunkstats;

import org.incendo.cloud.CommandManager;
import org.incendo.cloud.description.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.dfsek.terra.addons.manifest.api.AddonInitializer;
import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.addon.BaseAddon;
import com.dfsek.terra.api.command.CommandSender;
import com.dfsek.terra.api.event.events.platform.CommandRegistrationEvent;
import com.dfsek.terra.api.event.functional.FunctionalEventHandler;
import com.dfsek.terra.api.inject.annotations.Inject;
import com.dfsek.terra.api.statistics.ChunkStatisticsAnalysis;
import com.dfsek.terra.api.statistics.ChunkStatisticsCollector;
import com.dfsek.terra.api.statistics.ChunkStatisticsSummary;
import com.dfsek.terra.api.statistics.FeatureActivitySummary;
import com.dfsek.terra.api.statistics.StatisticalSummary;


public class ChunkStatisticsCommandAddon implements AddonInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ChunkStatisticsCommandAddon.class);

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

    private static String renderSummary(ChunkStatisticsCollector collector) {
        if(!collector.isEnabled()) {
            return "Chunk statistics collection is disabled. Enable debug.chunk-statistics in config.yml first.";
        }

        List<ChunkStatisticsSummary> summaries = ChunkStatisticsAnalysis.summarize(collector.getStatistics());
        if(summaries.isEmpty()) {
            return "No chunk statistics samples collected.";
        }

        StringBuilder builder = new StringBuilder("Chunk statistics summary");
        builder.append("\n1% low = slowest 1% average");
        summaries.forEach(summary -> appendSummary(builder, summary));
        return builder.toString();
    }

    private static void appendSummary(StringBuilder builder, ChunkStatisticsSummary summary) {
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
            builder.append(" (").append(summary.failedSamples()).append(" failed)");
        }

        builder.append("\nTotal: ").append(formatMillisSummary(summary.totalNanos()));

        builder.append("\nAccess:");
        appendNumericLine(builder, "protoChunkReads", summary.protoChunkReads());
        appendNumericLine(builder, "protoChunkWrites", summary.protoChunkWrites());
        appendNumericLine(builder, "worldBlockReads", summary.worldBlockReads());
        appendNumericLine(builder, "worldBlockEntityReads", summary.worldBlockEntityReads());
        appendNumericLine(builder, "worldBlockWrites", summary.worldBlockWrites());
        appendNumericLine(builder, "entitySpawns", summary.entitySpawns());
        appendNumericLine(builder, "crossChunkReads", summary.crossChunkReads());
        appendNumericLine(builder, "crossChunkWrites", summary.crossChunkWrites());
        appendNumericLine(builder, "maxReadChunkDistance", summary.maxReadChunkDistance());
        appendNumericLine(builder, "maxWriteChunkDistance", summary.maxWriteChunkDistance());
        appendNumericLine(builder, "minTouchedY", summary.minTouchedY());
        appendNumericLine(builder, "maxTouchedY", summary.maxTouchedY());

        builder.append("\nPhases:");
        summary.phaseNanos().entrySet().stream()
            .sorted(Map.Entry.<String, StatisticalSummary>comparingByValue(Comparator.comparingDouble(StatisticalSummary::average)).reversed())
            .forEach(entry -> builder.append("\n - ")
                .append(entry.getKey())
                .append(": ")
                .append(formatMillisSummary(entry.getValue())));

        builder.append("\nFeatures:");
        if(summary.features().isEmpty()) {
            builder.append("\n - none");
        } else {
            summary.features().entrySet().stream()
                .sorted((left, right) -> Double.compare(right.getValue().placements().average(), left.getValue().placements().average()))
                .forEach(entry -> appendFeatureLine(builder, entry.getKey(), entry.getValue()));
        }
    }

    private static void appendFeatureLine(StringBuilder builder, String feature, FeatureActivitySummary summary) {
        builder.append("\n - ")
            .append(feature)
            .append(": eval ")
            .append(formatCountSummary(summary.evaluations()))
            .append(" | match ")
            .append(formatCountSummary(summary.matches()))
            .append(" | place ")
            .append(formatCountSummary(summary.placements()));
    }

    private static void appendNumericLine(StringBuilder builder, String label, StatisticalSummary summary) {
        builder.append("\n - ")
            .append(label)
            .append(": ")
            .append(formatCountSummary(summary));
    }

    private static String formatMillisSummary(StatisticalSummary summary) {
        return format(summary.average() / 1_000_000D)
               + "ms avg / "
               + format(summary.percentile95() / 1_000_000D)
               + "ms p95 / "
               + format(summary.onePercentLowAverage() / 1_000_000D)
               + "ms 1% low";
    }

    private static String formatCountSummary(StatisticalSummary summary) {
        return format(summary.average())
               + " avg / "
               + format(summary.percentile95())
               + " p95 / "
               + format(summary.onePercentLowAverage())
               + " 1% low";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
