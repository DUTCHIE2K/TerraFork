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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dfsek.terra.api.statistics.ChunkStatistics;
import com.dfsek.terra.api.statistics.ChunkStatisticsCollector;
import com.dfsek.terra.api.statistics.ChunkStatisticsSession;
import com.dfsek.terra.api.statistics.FeatureStatistics;
import com.dfsek.terra.api.world.chunk.generation.ProtoChunk;
import com.dfsek.terra.api.world.chunk.generation.ProtoWorld;


public class ChunkStatisticsCollectorImpl implements ChunkStatisticsCollector {
    private static final int DEFAULT_CAPACITY = 75000;
    private static final ChunkStatisticsSession NO_OP_SESSION = new NoOpChunkStatisticsSession();
    private final ThreadLocal<ChunkStatisticsSessionImpl> current = new ThreadLocal<>();
    private final Deque<ChunkStatistics> statistics = new ArrayDeque<>();
    private final int capacity;
    private volatile boolean enabled;

    public ChunkStatisticsCollectorImpl() {
        this(DEFAULT_CAPACITY);
    }

    public ChunkStatisticsCollectorImpl(int capacity) {
        this.capacity = capacity;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if(!enabled) {
            reset();
            current.remove();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ChunkStatisticsSession beginChunk(String platform, String packId, String window, int chunkX, int chunkZ) {
        if(!enabled) {
            return NO_OP_SESSION;
        }
        return new ChunkStatisticsSessionImpl(this, platform, packId, window, chunkX, chunkZ);
    }

    @Override
    public ProtoChunk wrap(ProtoChunk chunk) {
        if(!enabled || chunk instanceof MeteredProtoChunk) {
            return chunk;
        }
        ChunkStatisticsSessionImpl session = current.get();
        if(session == null) {
            return chunk;
        }
        return new MeteredProtoChunk(session, chunk);
    }

    @Override
    public ProtoWorld wrap(ProtoWorld world) {
        if(!enabled || world instanceof MeteredProtoWorld) {
            return world;
        }
        ChunkStatisticsSessionImpl session = current.get();
        if(session == null) {
            return world;
        }
        return new MeteredProtoWorld(session, world);
    }

    @Override
    public void pushPhase(String phase) {
        ChunkStatisticsSessionImpl session = current.get();
        if(session != null) {
            session.pushPhase(phase);
        }
    }

    @Override
    public void popPhase(String phase) {
        ChunkStatisticsSessionImpl session = current.get();
        if(session != null) {
            session.popPhase(phase);
        }
    }

    @Override
    public void recordFeatureEvaluation(String featureId) {
        ChunkStatisticsSessionImpl session = current.get();
        if(session != null) {
            session.recordFeatureEvaluation(featureId);
        }
    }

    @Override
    public void recordFeatureMatch(String featureId) {
        ChunkStatisticsSessionImpl session = current.get();
        if(session != null) {
            session.recordFeatureMatch(featureId);
        }
    }

    @Override
    public void recordFeaturePlacement(String featureId) {
        ChunkStatisticsSessionImpl session = current.get();
        if(session != null) {
            session.recordFeaturePlacement(featureId);
        }
    }

    @Override
    public List<ChunkStatistics> getStatistics() {
        synchronized(statistics) {
            return new ArrayList<>(statistics);
        }
    }

    @Override
    public void reset() {
        synchronized(statistics) {
            statistics.clear();
        }
    }

    void publish(ChunkStatistics chunkStatistics) {
        synchronized(statistics) {
            while(statistics.size() >= capacity) {
                statistics.removeFirst();
            }
            statistics.addLast(chunkStatistics);
        }
    }

    private static final class NoOpChunkStatisticsSession implements ChunkStatisticsSession {
        private static final Activation NO_OP_ACTIVATION = () -> {
        };

        @Override
        public Activation activate() {
            return NO_OP_ACTIVATION;
        }

        @Override
        public void fail(Throwable throwable) {
        }

        @Override
        public void close() {
        }
    }

    static final class ChunkStatisticsSessionImpl implements ChunkStatisticsSession {
        private final ChunkStatisticsCollectorImpl collector;
        private final String platform;
        private final String packId;
        private final String window;
        private final int chunkX;
        private final int chunkZ;
        private final long startNanos = System.nanoTime();
        private final Deque<Frame> phases = new ArrayDeque<>();
        private final Map<String, Long> phaseNanos = new LinkedHashMap<>();
        private final Map<String, MutableFeatureStatistics> featureStats = new LinkedHashMap<>();
        private boolean successful = true;
        private String failure;
        private boolean closed;
        private long protoChunkReads;
        private long protoChunkWrites;
        private long worldBlockReads;
        private long worldBlockEntityReads;
        private long worldBlockWrites;
        private long entitySpawns;
        private long crossChunkReads;
        private long crossChunkWrites;
        private int maxReadChunkDistance;
        private int maxWriteChunkDistance;
        private Integer minTouchedY;
        private Integer maxTouchedY;

        private ChunkStatisticsSessionImpl(ChunkStatisticsCollectorImpl collector,
                                           String platform,
                                           String packId,
                                           String window,
                                           int chunkX,
                                           int chunkZ) {
            this.collector = collector;
            this.platform = platform;
            this.packId = packId;
            this.window = window;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public Activation activate() {
            ChunkStatisticsSessionImpl previous = collector.current.get();
            collector.current.set(this);
            return () -> {
                if(previous == null) {
                    collector.current.remove();
                } else {
                    collector.current.set(previous);
                }
            };
        }

        @Override
        public void fail(Throwable throwable) {
            successful = false;
            String message = throwable.getMessage();
            failure = message == null ? throwable.getClass().getName() : throwable.getClass().getName() + ": " + message;
        }

        @Override
        public void close() {
            if(closed) {
                return;
            }
            closed = true;
            long now = System.nanoTime();
            while(!phases.isEmpty()) {
                Frame frame = phases.pop();
                phaseNanos.merge(frame.phase(), now - frame.startNanos(), Long::sum);
            }
            collector.publish(new ChunkStatistics(platform,
                packId,
                window,
                chunkX,
                chunkZ,
                now - startNanos,
                phaseNanos,
                snapshotFeatures(),
                protoChunkReads,
                protoChunkWrites,
                worldBlockReads,
                worldBlockEntityReads,
                worldBlockWrites,
                entitySpawns,
                crossChunkReads,
                crossChunkWrites,
                maxReadChunkDistance,
                maxWriteChunkDistance,
                minTouchedY,
                maxTouchedY,
                successful,
                failure));
            if(collector.current.get() == this) {
                collector.current.remove();
            }
        }

        void pushPhase(String phase) {
            phases.push(new Frame(phase, System.nanoTime()));
        }

        void popPhase(String phase) {
            if(phases.isEmpty()) {
                return;
            }
            Frame frame = phases.pop();
            String resolved = frame.phase().equals(phase) ? phase : frame.phase();
            phaseNanos.merge(resolved, System.nanoTime() - frame.startNanos(), Long::sum);
        }

        void recordProtoChunkRead(int y) {
            protoChunkReads++;
            touchY(y);
        }

        void recordProtoChunkWrite(int y) {
            protoChunkWrites++;
            touchY(y);
        }

        void recordWorldBlockRead(int x, int y, int z) {
            worldBlockReads++;
            touchY(y);
            int distance = chunkDistance(x, z);
            if(distance > 0) {
                crossChunkReads++;
            }
            maxReadChunkDistance = Math.max(maxReadChunkDistance, distance);
        }

        void recordWorldBlockEntityRead(int x, int y, int z) {
            worldBlockEntityReads++;
            touchY(y);
            int distance = chunkDistance(x, z);
            if(distance > 0) {
                crossChunkReads++;
            }
            maxReadChunkDistance = Math.max(maxReadChunkDistance, distance);
        }

        void recordWorldBlockWrite(int x, int y, int z) {
            worldBlockWrites++;
            touchY(y);
            int distance = chunkDistance(x, z);
            if(distance > 0) {
                crossChunkWrites++;
            }
            maxWriteChunkDistance = Math.max(maxWriteChunkDistance, distance);
        }

        void recordEntitySpawn(double x, double y, double z) {
            entitySpawns++;
            touchY((int) Math.floor(y));
            int distance = chunkDistance((int) Math.floor(x), (int) Math.floor(z));
            if(distance > 0) {
                crossChunkWrites++;
            }
            maxWriteChunkDistance = Math.max(maxWriteChunkDistance, distance);
        }

        void recordFeatureEvaluation(String featureId) {
            featureStats(featureId).evaluations++;
        }

        void recordFeatureMatch(String featureId) {
            featureStats(featureId).matches++;
        }

        void recordFeaturePlacement(String featureId) {
            featureStats(featureId).placements++;
        }

        private Map<String, FeatureStatistics> snapshotFeatures() {
            Map<String, FeatureStatistics> snapshot = new LinkedHashMap<>();
            featureStats.forEach((featureId, stats) -> snapshot.put(featureId,
                new FeatureStatistics(stats.evaluations, stats.matches, stats.placements)));
            return snapshot;
        }

        private MutableFeatureStatistics featureStats(String featureId) {
            return featureStats.computeIfAbsent(featureId, ignored -> new MutableFeatureStatistics());
        }

        private void touchY(int y) {
            if(minTouchedY == null || y < minTouchedY) {
                minTouchedY = y;
            }
            if(maxTouchedY == null || y > maxTouchedY) {
                maxTouchedY = y;
            }
        }

        private int chunkDistance(int blockX, int blockZ) {
            int accessedChunkX = Math.floorDiv(blockX, 16);
            int accessedChunkZ = Math.floorDiv(blockZ, 16);
            return Math.max(Math.abs(accessedChunkX - chunkX), Math.abs(accessedChunkZ - chunkZ));
        }
    }

    private record Frame(String phase, long startNanos) {
    }

    private static final class MutableFeatureStatistics {
        private long evaluations;
        private long matches;
        private long placements;
    }
}
