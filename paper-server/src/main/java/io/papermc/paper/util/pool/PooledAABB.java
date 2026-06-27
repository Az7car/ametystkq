package io.papermc.paper.util.pool;

import net.minecraft.world.phys.AABB;

public final class PooledAABB {

    public static AABB get(final double minX, final double minY, final double minZ,
                            final double maxX, final double maxY, final double maxZ) {
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static void free(final AABB aabb) {
    }
}
