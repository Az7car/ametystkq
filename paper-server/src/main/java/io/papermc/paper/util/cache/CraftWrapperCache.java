package io.papermc.paper.util.cache;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import java.util.UUID;

public final class CraftWrapperCache {

    private static final Int2ObjectOpenHashMap<CraftEntity> entityById = new Int2ObjectOpenHashMap<>(4096, 0.75f);
    private static final Object2ObjectOpenHashMap<UUID, CraftEntity> entityByUuid = new Object2ObjectOpenHashMap<>(4096, 0.75f);
    private static final Object2ObjectOpenHashMap<UUID, CraftWorld> worldByUuid = new Object2ObjectOpenHashMap<>(64, 0.75f);
    private static final Object2ObjectOpenHashMap<ServerLevel, CraftWorld> worldByHandle = new Object2ObjectOpenHashMap<>(64, 0.75f);
    private static final Object lock = new Object();

    public static CraftEntity getEntity(final org.bukkit.entity.Entity entity) {
        if (entity instanceof CraftEntity ce) return ce;
        return null;
    }

    public static CraftEntity getEntityById(final int id) {
        synchronized (lock) {
            return entityById.get(id);
        }
    }

    public static CraftEntity getEntityByUuid(final UUID uuid) {
        synchronized (lock) {
            return entityByUuid.get(uuid);
        }
    }

    public static void cacheEntity(final CraftEntity craftEntity) {
        final net.minecraft.world.entity.Entity handle = craftEntity.getHandle();
        synchronized (lock) {
            entityById.put(handle.getId(), craftEntity);
            entityByUuid.put(handle.getUUID(), craftEntity);
        }
    }

    public static void removeEntity(final net.minecraft.world.entity.Entity entity) {
        synchronized (lock) {
            entityById.remove(entity.getId());
            entityByUuid.remove(entity.getUUID());
        }
    }

    public static CraftWorld getWorld(final World world) {
        if (world instanceof CraftWorld cw) return cw;
        return null;
    }

    public static CraftWorld getWorldByUuid(final UUID uuid) {
        synchronized (lock) {
            return worldByUuid.get(uuid);
        }
    }

    public static CraftWorld getWorldByHandle(final ServerLevel handle) {
        synchronized (lock) {
            return worldByHandle.get(handle);
        }
    }

    public static void cacheWorld(final CraftWorld world) {
        synchronized (lock) {
            worldByUuid.put(world.getUID(), world);
            worldByHandle.put(world.getHandle(), world);
        }
    }

    public static void removeWorld(final ServerLevel handle) {
        synchronized (lock) {
            final CraftWorld removed = worldByHandle.remove(handle);
            if (removed != null) {
                worldByUuid.remove(removed.getUID());
            }
        }
    }

    public static void clear() {
        synchronized (lock) {
            entityById.clear();
            entityByUuid.clear();
            worldByUuid.clear();
            worldByHandle.clear();
        }
    }

    public static int entityCount() {
        synchronized (lock) {
            return entityById.size();
        }
    }

    public static int worldCount() {
        synchronized (lock) {
            return worldByUuid.size();
        }
    }
}
