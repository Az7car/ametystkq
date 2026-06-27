package io.papermc.paper.entity.tracker;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class EntityNetworkOptimizer {

    private static final double MOVEMENT_THRESHOLD = 0.25;
    private static final double ROTATION_THRESHOLD = 5.0;

    private final ServerPlayer player;
    private final EntityDeltaTracker deltaTracker;

    public EntityNetworkOptimizer(final ServerPlayer player) {
        this.player = player;
        this.deltaTracker = new EntityDeltaTracker(player);
    }

    public void onEntityAdded(final Entity entity) {
        this.deltaTracker.addEntity(entity);
    }

    public void onEntityRemoved(final int entityId) {
        this.deltaTracker.removeEntity(entityId);
    }

    public List<Packet<?>> computeDirtyPackets(final Entity entity) {
        final EntityDeltaTracker.DeltaResult delta = this.deltaTracker.computeDelta(entity);
        if (delta.isNew()) return List.of();

        final List<Packet<?>> packets = new ArrayList<>(2);

        if (delta.positionData() != null) {
            final ClientboundTeleportEntityPacket movePacket = ClientboundTeleportEntityPacket.teleport(entity.getId(), PositionMoveRotation.of(entity), Set.of(), entity.onGround());
            packets.add(movePacket);
        }

        if (delta.dirtyMetadata() != null && !delta.dirtyMetadata().isEmpty()) {
            final ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(
                entity.getId(), delta.dirtyMetadata()
            );
            packets.add(metadataPacket);
        }

        return packets;
    }

    public void clear() {
        this.deltaTracker.clear();
    }
}
