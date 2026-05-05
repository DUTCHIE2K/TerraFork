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

import com.dfsek.terra.api.block.entity.BlockEntity;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.entity.Entity;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.world.ServerWorld;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.generation.ChunkGenerator;
import com.dfsek.terra.api.world.chunk.generation.ProtoWorld;


final class MeteredProtoWorld implements ProtoWorld {
    private final ChunkStatisticsCollectorImpl.ChunkStatisticsSessionImpl session;
    private final ProtoWorld delegate;

    MeteredProtoWorld(ChunkStatisticsCollectorImpl.ChunkStatisticsSessionImpl session, ProtoWorld delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    @Override
    public Object getHandle() {
        return delegate.getHandle();
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        session.recordWorldBlockRead(x, y, z);
        return delegate.getBlockState(x, y, z);
    }

    @Override
    public BlockEntity getBlockEntity(int x, int y, int z) {
        session.recordWorldBlockEntityRead(x, y, z);
        return delegate.getBlockEntity(x, y, z);
    }

    @Override
    public long getSeed() {
        return delegate.getSeed();
    }

    @Override
    public int getMaxHeight() {
        return delegate.getMaxHeight();
    }

    @Override
    public int getMinHeight() {
        return delegate.getMinHeight();
    }

    @Override
    public ChunkGenerator getGenerator() {
        return delegate.getGenerator();
    }

    @Override
    public BiomeProvider getBiomeProvider() {
        return delegate.getBiomeProvider();
    }

    @Override
    public ConfigPack getPack() {
        return delegate.getPack();
    }

    @Override
    public void setBlockState(int x, int y, int z, BlockState data, boolean physics) {
        session.recordWorldBlockWrite(x, y, z);
        delegate.setBlockState(x, y, z, data, physics);
    }

    @Override
    public Entity spawnEntity(double x, double y, double z, EntityType entityType) {
        session.recordEntitySpawn(x, y, z);
        return delegate.spawnEntity(x, y, z, entityType);
    }

    @Override
    public int centerChunkX() {
        return delegate.centerChunkX();
    }

    @Override
    public int centerChunkZ() {
        return delegate.centerChunkZ();
    }

    @Override
    public ServerWorld getWorld() {
        return delegate.getWorld();
    }
}
