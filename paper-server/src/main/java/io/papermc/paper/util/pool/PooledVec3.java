package io.papermc.paper.util.pool;

import net.minecraft.world.phys.Vec3;

public final class PooledVec3 {

    public static Vec3 get(final double x, final double y, final double z) {
        return new Vec3(x, y, z);
    }

    public static void free(final Vec3 vec) {
    }
}
