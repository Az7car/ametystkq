package io.papermc.paper.util.pathfinding;

import net.minecraft.world.level.pathfinder.Node;
import java.util.List;

public final class PathNodePool {

    private PathNodePool() {}

    public static Node acquire(final int x, final int y, final int z) {
        return new Node(x, y, z);
    }

    public static void release(final Node node) {
    }

    public static void releaseAll(final List<Node> nodes) {
        nodes.clear();
    }

    public static void reset() {
    }

    public static int poolSize() {
        return 0;
    }
}
