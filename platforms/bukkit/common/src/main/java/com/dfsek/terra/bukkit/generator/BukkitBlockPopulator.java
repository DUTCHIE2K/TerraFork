package com.dfsek.terra.bukkit.generator;

import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

import com.dfsek.terra.api.statistics.ChunkStatisticsSession;
import com.dfsek.terra.api.statistics.ChunkStatisticsWindows;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.bukkit.PlatformImpl;
import com.dfsek.terra.statistics.ChunkStatisticsSupport;
import com.dfsek.terra.bukkit.world.BukkitProtoWorld;


public class BukkitBlockPopulator extends BlockPopulator {
    private final PlatformImpl platform;
    private final BlockState air;
    private ConfigPack pack;

    public BukkitBlockPopulator(PlatformImpl platform, ConfigPack pack, BlockState air) {
        this.platform = platform;
        this.pack = pack;
        this.air = air;
    }

    public void setPack(ConfigPack pack) {
        this.pack = pack;
    }

    @Override
    @SuppressWarnings("try")
    public void populate(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ,
                         @NotNull LimitedRegion limitedRegion) {
        ChunkStatisticsSession session = ChunkStatisticsSupport.beginWindow(platform, pack, ChunkStatisticsWindows.STAGES, chunkX, chunkZ);
        try(session) {
            try(ChunkStatisticsSession.Activation activation = session.activate()) {
                ChunkStatisticsSupport.runStages(platform, pack.getStages(), new BukkitProtoWorld(limitedRegion, air, pack.getBiomeProvider()));
            } catch(RuntimeException | Error e) {
                session.fail(e);
                throw e;
            }
        }
    }
}
