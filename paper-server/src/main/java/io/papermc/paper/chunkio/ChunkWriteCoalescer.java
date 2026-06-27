package io.papermc.paper.chunkio;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ChunkWriteCoalescer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkWriteCoalescer.class);
    private static final int FLUSH_INTERVAL_TICKS = 20;
    private static final int MAX_BATCH_SIZE = 64;

    private final ExecutorService ioExecutor = Executors.newFixedThreadPool(
        Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
        new ThreadFactoryBuilder()
            .setNameFormat("Chunk IO Worker - %d")
            .setDaemon(true)
            .build()
    );

    private final Long2ObjectOpenHashMap<List<PendingWrite>> pendingWrites = new Long2ObjectOpenHashMap<>();
    private final ServerLevel level;
    private int tickCounter;

    public ChunkWriteCoalescer(final ServerLevel level) {
        this.level = level;
    }

    public void scheduleWrite(final ChunkPos pos, final CompoundTag data) {
        final long regionKey = this.regionKey(pos);
        synchronized (this.pendingWrites) {
            final List<PendingWrite> batch = this.pendingWrites.computeIfAbsent(regionKey, k -> new ArrayList<>());
            // Replace existing write for same chunk
            for (int i = 0; i < batch.size(); i++) {
                if (batch.get(i).pos.equals(pos)) {
                    batch.set(i, new PendingWrite(pos, data));
                    return;
                }
            }
            if (batch.size() < MAX_BATCH_SIZE) {
                batch.add(new PendingWrite(pos, data));
            }
        }
    }

    public void flush() {
        final Long2ObjectOpenHashMap<List<PendingWrite>> toFlush;
        synchronized (this.pendingWrites) {
            if (this.pendingWrites.isEmpty()) return;
            toFlush = new Long2ObjectOpenHashMap<>(this.pendingWrites);
            this.pendingWrites.clear();
        }

        for (final var entry : toFlush.long2ObjectEntrySet()) {
            final List<PendingWrite> batch = entry.getValue();
            this.ioExecutor.submit(() -> this.writeBatch(batch));
        }
    }

    public void tick() {
        this.tickCounter++;
        if (this.tickCounter >= FLUSH_INTERVAL_TICKS) {
            this.tickCounter = 0;
            this.flush();
        }
    }

    public void shutdown() {
        this.flush();
        this.ioExecutor.shutdown();
        try {
            this.ioExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void writeBatch(final List<PendingWrite> batch) {
        for (final PendingWrite write : batch) {
            try {
                this.level.getChunkSource().chunkMap.write(write.pos, write.data);
            } catch (final Exception e) {
                LOGGER.error("Failed to write chunk {} async", write.pos, e);
            }
        }
    }

    private static long regionKey(final ChunkPos pos) {
        return ((long) (pos.x >> 5) << 32) | (pos.z >> 5) & 0xFFFFFFFFL;
    }

    private record PendingWrite(ChunkPos pos, CompoundTag data) {}
}
