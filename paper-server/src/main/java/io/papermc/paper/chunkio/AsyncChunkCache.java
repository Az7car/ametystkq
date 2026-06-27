package io.papermc.paper.chunkio;

import io.papermc.paper.util.MCUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public final class AsyncChunkCache {

    private static final int CACHE_MAX_SIZE = 2048;
    private static final int OFF_HEAP_BLOCK_SIZE = 128 * 1024; // 128KB per chunk max

    private final Long2ObjectOpenHashMap<ChunkCacheEntry> cache = new Long2ObjectOpenHashMap<>();
    private final ServerLevel level;
    private long cacheHits;
    private long cacheMisses;

    public AsyncChunkCache(final ServerLevel level) {
        this.level = level;
    }

    public void store(final long chunkKey, final CompoundTag data) {
        synchronized (this.cache) {
            if (this.cache.size() >= CACHE_MAX_SIZE) {
                this.evict();
            }
            final ChunkCacheEntry entry = new ChunkCacheEntry(this.serializeOffHeap(data));
            this.cache.put(chunkKey, entry);
        }
    }

    public CompoundTag load(final long chunkKey) {
        synchronized (this.cache) {
            final ChunkCacheEntry entry = this.cache.get(chunkKey);
            if (entry != null) {
                this.cacheHits++;
                return this.deserializeOffHeap(entry.data);
            }
            this.cacheMisses++;
            return null;
        }
    }

    public void invalidate(final long chunkKey) {
        synchronized (this.cache) {
            this.cache.remove(chunkKey);
        }
    }

    public double hitRate() {
        final long total = this.cacheHits + this.cacheMisses;
        return total == 0 ? 0 : (double) this.cacheHits / total;
    }

    private ByteBuffer serializeOffHeap(final CompoundTag tag) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(OFF_HEAP_BLOCK_SIZE);
            try (final DataOutputStream dos = new DataOutputStream(new DeflaterOutputStream(baos))) {
                NbtIo.write(tag, dos);
            }
            final byte[] raw = baos.toByteArray();
            return ByteBuffer.allocateDirect(raw.length).put(raw).flip().asReadOnlyBuffer();
        } catch (final Exception e) {
            AsyncChunkCache.LOGGER.error("Failed to serialize chunk off-heap", e);
            return null;
        }
    }

    private CompoundTag deserializeOffHeap(final ByteBuffer buffer) {
        if (buffer == null) return null;
        try {
            final byte[] raw = new byte[buffer.remaining()];
            buffer.get(raw);
            try (final DataInputStream dis = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(raw)))) {
                return NbtIo.read(dis);
            }
        } catch (final Exception e) {
            AsyncChunkCache.LOGGER.error("Failed to deserialize chunk from off-heap cache", e);
            return null;
        }
    }

    private void evict() {
        int targetSize = CACHE_MAX_SIZE / 2;
        final java.util.ArrayList<Long> toRemove = new java.util.ArrayList<>();
        for (            final var entry : this.cache.long2ObjectEntrySet()) {
            if (toRemove.size() >= this.cache.size() - targetSize) break;
            toRemove.add(entry.getLongKey());
        }
        for (final long key : toRemove) {
            final ChunkCacheEntry removed = this.cache.remove(key);
        }
    }

    private static final class ChunkCacheEntry {
        final ByteBuffer data;
        final long storedAt;

        ChunkCacheEntry(final ByteBuffer data) {
            this.data = data;
            this.storedAt = System.nanoTime();
        }
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("AsyncChunkCache");
}
