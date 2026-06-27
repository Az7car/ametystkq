package io.papermc.paper.util.nbt;

import io.papermc.paper.util.StringPool;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public final class NbtOptimizer {

    private static final Logger LOGGER = LoggerFactory.getLogger("NbtOptimizer");
    private static final StringPool GLOBAL_STRING_POOL = new StringPool();
    private static final Map<String, StringTag> TAG_CACHE = new Object2ObjectOpenHashMap<>(65536);
    private static boolean enabled = true;
    private static long deduplicatedStrings;

    private NbtOptimizer() {}

    public static void setEnabled(final boolean flag) {
        enabled = flag;
        if (flag) {
            LOGGER.info("NBT string deduplication enabled");
        }
    }

    public static String deduplicate(final String s) {
        if (!enabled || s == null || s.isEmpty()) return s;
        deduplicatedStrings++;
        return GLOBAL_STRING_POOL.string(s);
    }

    public static StringTag cachedStringTag(final String value) {
        if (!enabled) return StringTag.valueOf(value);
        final String pooled = deduplicate(value);
        return TAG_CACHE.computeIfAbsent(pooled, StringTag::valueOf);
    }

    public static CompoundTag optimize(final CompoundTag tag) {
        if (!enabled || tag == null) return tag;
        return optimizeCompound(tag);
    }

    private static CompoundTag optimizeCompound(final CompoundTag tag) {
        if (tag.isEmpty()) return tag;

        for (final String key : tag.keySet()) {
            final String pooledKey = deduplicate(key);
            final Tag value = tag.get(pooledKey);
            if (value == null) continue;

            if (value instanceof CompoundTag child) {
                tag.put(pooledKey, optimizeCompound(child));
            } else if (value instanceof StringTag st) {
                tag.put(pooledKey, cachedStringTag(st.value()));
            }
        }
        return tag;
    }

    public static long getDeduplicatedCount() {
        return deduplicatedStrings;
    }

    public static void resetStats() {
        deduplicatedStrings = 0;
    }

    public static int getPoolSize() {
        return TAG_CACHE.size();
    }
}
