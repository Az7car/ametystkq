package io.papermc.paper.entity.tracker;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.BitSet;
import java.util.List;
import java.util.ArrayList;

public final class EntityDeltaTracker {

    private static final int MAX_TRACKED_ENTITIES = 2048;

    private final ServerPlayer player;
    private final Int2ObjectOpenHashMap<EntitySnapshot> tracked = new Int2ObjectOpenHashMap<>();

    public EntityDeltaTracker(final ServerPlayer player) {
        this.player = player;
    }

    public void addEntity(final Entity entity) {
        if (this.tracked.size() >= MAX_TRACKED_ENTITIES) return;
        this.tracked.put(entity.getId(), new EntitySnapshot(entity));
    }

    public void removeEntity(final int entityId) {
        this.tracked.remove(entityId);
    }

    public DeltaResult computeDelta(final Entity entity) {
        final EntitySnapshot current = new EntitySnapshot(entity);
        final EntitySnapshot previous = this.tracked.get(entity.getId());

        if (previous == null) {
            this.tracked.put(entity.getId(), current);
            return new DeltaResult(true, null, null);
        }

        final boolean moved = !current.position.equals(previous.position)
            || current.yRot != previous.yRot
            || current.xRot != previous.xRot;

        final boolean metadataChanged = !current.metadata.equals(previous.metadata);

        this.tracked.put(entity.getId(), current);

        if (!moved && !metadataChanged) return DeltaResult.EMPTY;

        final List<SynchedEntityData.DataValue<?>> dirtyFields = metadataChanged
            ? this.computeDirtyFields(current.metadata, previous.metadata)
            : null;

        return new DeltaResult(false, moved ? current : null, dirtyFields);
    }

    private List<SynchedEntityData.DataValue<?>> computeDirtyFields(
        final List<SynchedEntityData.DataValue<?>> current,
        final List<SynchedEntityData.DataValue<?>> previous
    ) {
        if (current == previous) return null;
        if (current == null || previous == null) return current;

        final List<SynchedEntityData.DataValue<?>> dirty = new ArrayList<>();
        final BitSet seen = new BitSet(Math.max(current.size(), previous.size()));

        for (final SynchedEntityData.DataValue<?> cur : current) {
            seen.set(cur.id());
            final SynchedEntityData.DataValue<?> prev = findById(previous, cur.id());
            if (prev == null || !prev.value().equals(cur.value())) {
                dirty.add(cur);
            }
        }

        // Also include fields that existed before but are now removed
        for (final SynchedEntityData.DataValue<?> prev : previous) {
            if (!seen.get(prev.id())) {
                dirty.add(prev);
            }
        }

        return dirty.isEmpty() ? null : dirty;
    }

    private static SynchedEntityData.DataValue<?> findById(final List<SynchedEntityData.DataValue<?>> list, final int id) {
        for (final SynchedEntityData.DataValue<?> item : list) {
            if (item.id() == id) return item;
        }
        return null;
    }

    public void clear() {
        this.tracked.clear();
    }

    public record DeltaResult(
        boolean isNew,
        EntitySnapshot positionData,
        List<SynchedEntityData.DataValue<?>> dirtyMetadata
    ) {
        static final DeltaResult EMPTY = new DeltaResult(false, null, null);
    }

    public record EntitySnapshot(
        Vec3 position,
        float yRot,
        float xRot,
        boolean onGround,
        List<SynchedEntityData.DataValue<?>> metadata
    ) {
        EntitySnapshot(final Entity entity) {
            this(
                entity.position(),
                entity.getYRot(),
                entity.getXRot(),
                entity.onGround(),
                entity.getEntityData().getNonDefaultValues()
            );
        }
    }
}
