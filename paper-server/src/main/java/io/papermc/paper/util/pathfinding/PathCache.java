package io.papermc.paper.util.pathfinding;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import java.util.List;

public final class PathCache {

    private static final int MAX_CACHE_SIZE = 2048;
    private static final ObjectArrayList<PathResult> resultCache = new ObjectArrayList<>(MAX_CACHE_SIZE);

    private PathCache() {}

    public static PathResult cacheResult(final int entityId, final Path path, final long gameTime) {
        synchronized (resultCache) {
            if (resultCache.size() >= MAX_CACHE_SIZE) {
                resultCache.remove(resultCache.size() - 1);
            }
            final PathResult result = new PathResult(entityId, path, gameTime);
            resultCache.add(result);
            return result;
        }
    }

    public static Path getCachedPath(final int entityId, final long currentTime, final long maxAge) {
        synchronized (resultCache) {
            for (int i = resultCache.size() - 1; i >= 0; i--) {
                final PathResult r = resultCache.get(i);
                if (r.entityId == entityId && (currentTime - r.gameTime) <= maxAge) {
                    return r.path;
                }
            }
        }
        return null;
    }

    public static void invalidateEntity(final int entityId) {
        synchronized (resultCache) {
            resultCache.removeIf(r -> r.entityId == entityId);
        }
    }

    public static void clear() {
        synchronized (resultCache) {
            resultCache.clear();
        }
    }

    public record PathResult(int entityId, Path path, long gameTime) {}
}
