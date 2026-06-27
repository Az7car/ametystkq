package io.papermc.paper.chunkio;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ChunkPrefetcher {

    private static final int PREFETCH_AHEAD = 2;
    private static final int PREFETCH_BEHIND = 1;
    private static final int PREFETCH_SIDES = 1;

    private final ServerLevel level;
    private final LongOpenHashSet pendingPrefetches = new LongOpenHashSet();
    private final ExecutorService prefetchExecutor = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder()
            .setNameFormat("Chunk Prefetcher - %d")
            .setDaemon(true)
            .build()
    );

    public ChunkPrefetcher(final ServerLevel level) {
        this.level = level;
    }

    public void onPlayerMove(final ServerPlayer player) {
        final ChunkPos center = player.chunkPosition();
        final float yaw = player.getYRot();
        final double rad = Math.toRadians(yaw);
        final int dx = (int) (-Math.sin(rad) * PREFETCH_AHEAD);
        final int dz = (int) (Math.cos(rad) * PREFETCH_AHEAD);

        // Prefetch in movement direction
        for (int i = 1; i <= PREFETCH_AHEAD; i++) {
            final int cx = center.x + dx * i / PREFETCH_AHEAD;
            final int cz = center.z + dz * i / PREFETCH_AHEAD;
            this.prefetch(cx, cz);
        }

        // Prefetch sides
        final int sx = -dz;
        final int sz = dx;
        for (int i = 1; i <= PREFETCH_SIDES; i++) {
            this.prefetch(center.x + sx * i / PREFETCH_SIDES, center.z + sz * i / PREFETCH_SIDES);
            this.prefetch(center.x - sx * i / PREFETCH_SIDES, center.z - sz * i / PREFETCH_SIDES);
        }

        // Prefetch behind (for quick turns)
        for (int i = 1; i <= PREFETCH_BEHIND; i++) {
            this.prefetch(center.x - dx * i / PREFETCH_BEHIND, center.z - dz * i / PREFETCH_BEHIND);
        }
    }

    private void prefetch(final int cx, final int cz) {
        final long key = ChunkPos.asLong(cx, cz);
        synchronized (this.pendingPrefetches) {
            if (this.pendingPrefetches.contains(key)) return;
            if (this.level.getChunkSource().getChunkAtIfCachedImmediately(cx, cz) != null) return;
            this.pendingPrefetches.add(key);
        }

        this.prefetchExecutor.submit(() -> {
            try {
                this.level.getChunk(cx, cz);
            } finally {
                synchronized (this.pendingPrefetches) {
                    this.pendingPrefetches.remove(key);
                }
            }
        });
    }

    public void shutdown() {
        this.prefetchExecutor.shutdown();
    }
}
