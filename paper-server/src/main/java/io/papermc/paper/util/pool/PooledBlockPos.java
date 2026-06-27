package io.papermc.paper.util.pool;

import net.minecraft.core.BlockPos;

public final class PooledBlockPos {

    private static final ObjectPool<BlockPos.MutableBlockPos> POOL = ObjectPool.create(
        () -> new BlockPos.MutableBlockPos(0, 0, 0),
        () -> {},
        16384
    );

    public static BlockPos.MutableBlockPos get() {
        return POOL.get();
    }

    public static BlockPos.MutableBlockPos get(final int x, final int y, final int z) {
        final BlockPos.MutableBlockPos pos = POOL.get();
        pos.set(x, y, z);
        return pos;
    }

    public static BlockPos.MutableBlockPos get(final BlockPos pos) {
        final BlockPos.MutableBlockPos pooled = POOL.get();
        pooled.set(pos);
        return pooled;
    }

    public static void free(final BlockPos.MutableBlockPos pos) {
        POOL.free(pos);
    }
}
