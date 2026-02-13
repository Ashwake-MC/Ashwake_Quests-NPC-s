package com.ashwake.quests.npcs.network;

import com.ashwake.quests.npcs.AshwakeQuestsNpcsMod;
import com.ashwake.quests.npcs.GuidanceQuestData;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class GuidanceNetwork {
    private static final String VERSION = "1";

    private GuidanceNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(GuidanceAcceptPayload.TYPE, GuidanceAcceptPayload.STREAM_CODEC, GuidanceNetwork::handleAccept);
        registrar.playToServer(GuidanceCompletePayload.TYPE, GuidanceCompletePayload.STREAM_CODEC, GuidanceNetwork::handleComplete);
        registrar.playToClient(GuidanceSyncPayload.TYPE, GuidanceSyncPayload.STREAM_CODEC);
    }

    public static void sendSync(ServerPlayer player, boolean accepted, boolean completed) {
        PacketDistributor.sendToPlayer(player, new GuidanceSyncPayload(accepted, completed));
    }

    private static void handleAccept(GuidanceAcceptPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer serverPlayer) {
                GuidanceQuestData.setAccepted(serverPlayer, true);
                AshwakeQuestsNpcsMod.ensureOrinPresent(
                        serverPlayer.level().getServer().getLevel(Level.OVERWORLD));
                sendSync(serverPlayer, true, GuidanceQuestData.isCompleted(serverPlayer));
            }
        });
    }

    private static void handleComplete(GuidanceCompletePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer serverPlayer) {
                if (!GuidanceQuestData.isAccepted(serverPlayer)) {
                    return;
                }
                if (GuidanceQuestData.isCompleted(serverPlayer)) {
                    return;
                }
                GuidanceQuestData.setCompleted(serverPlayer, true);
                sendSync(serverPlayer, GuidanceQuestData.isAccepted(serverPlayer), true);
            }
        });
    }

    public record GuidanceAcceptPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<GuidanceAcceptPayload> TYPE = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(AshwakeQuestsNpcsMod.MODID, "guidance_accept"));
        public static final StreamCodec<RegistryFriendlyByteBuf, GuidanceAcceptPayload> STREAM_CODEC =
                StreamCodec.unit(new GuidanceAcceptPayload());

        @Override
        public CustomPacketPayload.Type<GuidanceAcceptPayload> type() {
            return TYPE;
        }
    }

    public record GuidanceCompletePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<GuidanceCompletePayload> TYPE = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(AshwakeQuestsNpcsMod.MODID, "guidance_complete"));
        public static final StreamCodec<RegistryFriendlyByteBuf, GuidanceCompletePayload> STREAM_CODEC =
                StreamCodec.unit(new GuidanceCompletePayload());

        @Override
        public CustomPacketPayload.Type<GuidanceCompletePayload> type() {
            return TYPE;
        }
    }

    public record GuidanceSyncPayload(boolean accepted, boolean completed) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<GuidanceSyncPayload> TYPE = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(AshwakeQuestsNpcsMod.MODID, "guidance_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, GuidanceSyncPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBoolean(payload.accepted);
                    buf.writeBoolean(payload.completed);
                },
                buf -> new GuidanceSyncPayload(buf.readBoolean(), buf.readBoolean())
        );

        @Override
        public CustomPacketPayload.Type<GuidanceSyncPayload> type() {
            return TYPE;
        }
    }
}
