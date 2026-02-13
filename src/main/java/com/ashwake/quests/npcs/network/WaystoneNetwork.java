package com.ashwake.quests.npcs.network;

import com.ashwake.quests.npcs.AshwakeQuestsNpcsMod;
import com.ashwake.quests.npcs.OrinQuestData;
import com.ashwake.quests.npcs.network.OrinNetwork;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.Set;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class WaystoneNetwork {
    private static final String VERSION = "1";
    private static final int COOLDOWN_TICKS = 200;

    private WaystoneNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(WaystoneTeleportPayload.TYPE, WaystoneTeleportPayload.STREAM_CODEC, WaystoneNetwork::handleTeleport);
    }

    private static void handleTeleport(WaystoneTeleportPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }

            ItemStack waystoneStack = AshwakeQuestsNpcsMod.ASHWAKE_WAYSTONE.get().getDefaultInstance();
            ItemCooldowns cooldowns = serverPlayer.getCooldowns();
            if (cooldowns.isOnCooldown(waystoneStack)) {
                return;
            }

            Destination destination = Destination.fromId(payload.destinationId());
            if (destination == null) {
                return;
            }

            if (destination == Destination.PERSONAL) {
                if (!serverPlayer.level().dimension().equals(AshwakeQuestsNpcsMod.PERSONAL_WORLD_DIMENSION)) {
                    OrinQuestData.setReturnLocation(serverPlayer, serverPlayer.level().dimension(), serverPlayer.position());
                }
                ServerLevel targetLevel = serverPlayer.level().getServer()
                        .getLevel(AshwakeQuestsNpcsMod.PERSONAL_WORLD_DIMENSION);
                if (targetLevel == null) {
                    targetLevel = serverPlayer.level().getServer().getLevel(Level.OVERWORLD);
                }
                if (targetLevel == null) {
                    return;
                }
                AshwakeQuestsNpcsMod.configurePersonalWorldBorder(
                        targetLevel,
                        OrinQuestData.getStage(serverPlayer),
                        false);
                BlockPos defaultAnchor = BlockPos.containing(AshwakeQuestsNpcsMod.PERSONAL_WORLD_POS);
                BlockPos storedSpawn = OrinQuestData.getPersonalSpawn(serverPlayer, defaultAnchor);
                BlockPos personalSpawn = AshwakeQuestsNpcsMod.findSurfaceSpawn(targetLevel, storedSpawn);
                if (!storedSpawn.equals(personalSpawn) || !OrinQuestData.hasPersonalSpawn(serverPlayer)) {
                    OrinQuestData.setPersonalSpawn(serverPlayer, personalSpawn);
                }
                serverPlayer.teleportTo(targetLevel,
                        personalSpawn.getX() + 0.5,
                        personalSpawn.getY(),
                        personalSpawn.getZ() + 0.5,
                        Set.of(),
                        serverPlayer.getYRot(),
                        serverPlayer.getXRot(),
                        true);
                OrinQuestData.setVisitedPersonal(serverPlayer, true);
            } else {
                ServerLevel targetLevel = serverPlayer.level().getServer().getLevel(Level.OVERWORLD);
                double targetX = AshwakeQuestsNpcsMod.HUB_POS.x;
                double targetY = AshwakeQuestsNpcsMod.HUB_POS.y;
                double targetZ = AshwakeQuestsNpcsMod.HUB_POS.z;
                if (serverPlayer.level().dimension().equals(AshwakeQuestsNpcsMod.PERSONAL_WORLD_DIMENSION)) {
                    OrinQuestData.ReturnLocation returnLocation = OrinQuestData.getReturnLocation(serverPlayer);
                    if (returnLocation != null) {
                        ServerLevel returnLevel = serverPlayer.level().getServer().getLevel(returnLocation.dimension());
                        if (returnLevel != null) {
                            targetLevel = returnLevel;
                            targetX = returnLocation.pos().x;
                            targetY = returnLocation.pos().y;
                            targetZ = returnLocation.pos().z;
                        }
                    }
                }
                if (targetLevel == null) {
                    return;
                }
                serverPlayer.teleportTo(targetLevel,
                        targetX,
                        targetY,
                        targetZ,
                        Set.of(),
                        serverPlayer.getYRot(),
                        serverPlayer.getXRot(),
                        true);
            }

            cooldowns.addCooldown(waystoneStack, COOLDOWN_TICKS);
            OrinNetwork.sendSync(serverPlayer);
        });
    }

    public enum Destination {
        HUB((byte)0),
        PERSONAL((byte)1);

        private final byte id;

        Destination(byte id) {
            this.id = id;
        }

        public byte id() {
            return id;
        }

        public static Destination fromId(byte id) {
            for (Destination destination : values()) {
                if (destination.id == id) {
                    return destination;
                }
            }
            return null;
        }
    }

    public record WaystoneTeleportPayload(byte destinationId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<WaystoneTeleportPayload> TYPE = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(AshwakeQuestsNpcsMod.MODID, "waystone_teleport"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WaystoneTeleportPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeByte(payload.destinationId),
                buf -> new WaystoneTeleportPayload(buf.readByte())
        );

        @Override
        public CustomPacketPayload.Type<WaystoneTeleportPayload> type() {
            return TYPE;
        }
    }
}
