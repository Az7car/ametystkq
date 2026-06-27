package io.papermc.paper.entity.tracker;

import io.papermc.paper.util.MCUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class EntityBroadcastController {

    public static int BROADCAST_INTERVAL = 6; // every 6 ticks (configurable via AmetystKQBootstrap)
    private final ServerLevel level;
    private final Int2ObjectOpenHashMap<List<ServerPlayer>> pendingBroadcasts = new Int2ObjectOpenHashMap<>();
    private int tick;

    public EntityBroadcastController(final ServerLevel level) {
        this.level = level;
    }

    public void addEntity(final Entity entity, final List<ServerPlayer> viewers) {
        if (viewers.isEmpty()) return;
        synchronized (this.pendingBroadcasts) {
            this.pendingBroadcasts.put(entity.getId(), new ArrayList<>(viewers));
        }
    }

    public void tick() {
        this.tick++;
        if (this.tick % BROADCAST_INTERVAL != 0) return;

        final Int2ObjectOpenHashMap<List<ServerPlayer>> toFlush;
        synchronized (this.pendingBroadcasts) {
            if (this.pendingBroadcasts.isEmpty()) return;
            toFlush = new Int2ObjectOpenHashMap<>(this.pendingBroadcasts);
            this.pendingBroadcasts.clear();
        }

        for (final var entry : toFlush.int2ObjectEntrySet()) {
            final Entity entity = this.level.getEntity(entry.getIntKey());
            if (entity == null || !entity.shouldBeSaved()) continue;

            final List<ServerPlayer> viewers = entry.getValue();
            if (viewers.isEmpty()) continue;

            final List<Packet<? super ClientGamePacketListener>> bundle = new ArrayList<>();
            final ClientboundTeleportEntityPacket movePacket = ClientboundTeleportEntityPacket.teleport(entity.getId(), PositionMoveRotation.of(entity), Set.of(), entity.onGround());
            final ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(entity.getId(), entity.getEntityData().getNonDefaultValues());
            final ClientboundRotateHeadPacket headPacket = new ClientboundRotateHeadPacket(entity, (byte) (entity.getYRot() * 256.0F / 360.0F));

            bundle.add(movePacket);
            bundle.add(headPacket);
            bundle.add(metadataPacket);

            final ClientboundBundlePacket bundled = new ClientboundBundlePacket(bundle);

            for (final ServerPlayer viewer : viewers) {
                if (viewer.connection != null) {
                    viewer.connection.send(bundled);
                }
            }
        }
    }

    public void shutdown() {
        this.pendingBroadcasts.clear();
    }
}
