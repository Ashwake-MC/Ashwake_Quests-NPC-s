package com.ashwake.quests.npcs.network;

import com.ashwake.quests.npcs.AshwakeQuestsNpcsMod;
import com.ashwake.quests.npcs.OrinQuestData;
import com.ashwake.quests.npcs.GuidanceQuestData;
import com.ashwake.quests.npcs.network.GuidanceNetwork;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class OrinNetwork {
    private static final String VERSION = "1";

    private OrinNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(OrinSyncPayload.TYPE, OrinSyncPayload.STREAM_CODEC);
        registrar.playToServer(OrinAcceptPayload.TYPE, OrinAcceptPayload.STREAM_CODEC, OrinNetwork::handleAccept);
        registrar.playToServer(OrinCompletePayload.TYPE, OrinCompletePayload.STREAM_CODEC, OrinNetwork::handleComplete);
        registrar.playToServer(OrinReclaimPayload.TYPE, OrinReclaimPayload.STREAM_CODEC, OrinNetwork::handleReclaim);
    }

    public static void sendSync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OrinSyncPayload(
                OrinQuestData.getStage(player),
                OrinQuestData.isWaystoneGiven(player),
                OrinQuestData.isVisitedPersonal(player),
                OrinQuestData.isQuestCompleted(player)
        ));
    }

    private static void handleAccept(OrinAcceptPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!GuidanceQuestData.isAccepted(serverPlayer)) {
                return;
            }
            int stage = OrinQuestData.getStage(serverPlayer);
            if (payload.questId() == 1 && stage == OrinQuestData.STAGE_NONE) {
                if (!GuidanceQuestData.isCompleted(serverPlayer)) {
                    return;
                }
                OrinQuestData.setStage(serverPlayer, OrinQuestData.STAGE_Q1_ACCEPTED);
            } else if (payload.questId() == 2 && stage == OrinQuestData.STAGE_Q1_COMPLETED) {
                OrinQuestData.setStage(serverPlayer, OrinQuestData.STAGE_Q2_ACCEPTED);
                if (!OrinQuestData.isWaystoneGiven(serverPlayer)) {
                    ItemStack waystone = new ItemStack(AshwakeQuestsNpcsMod.ASHWAKE_WAYSTONE.get());
                    if (!serverPlayer.getInventory().add(waystone)) {
                        serverPlayer.drop(waystone, false);
                    }
                    OrinQuestData.setWaystoneGiven(serverPlayer, true);
                    serverPlayer.sendSystemMessage(
                            net.minecraft.network.chat.Component.translatable("ashwake_quests_npcs.waystone.received"));
                }
            } else if (payload.questId() == 3 && stage == OrinQuestData.STAGE_Q2_COMPLETED) {
                OrinQuestData.setStage(serverPlayer, OrinQuestData.STAGE_Q3_ACCEPTED);
            } else if (payload.questId() == 4 && stage == OrinQuestData.STAGE_Q3_COMPLETED) {
                OrinQuestData.setStage(serverPlayer, OrinQuestData.STAGE_Q4_ACCEPTED);
                AshwakeQuestsNpcsMod.ensureVaelaPresent(serverPlayer.level().getServer().getLevel(Level.OVERWORLD));
            } else {
                return;
            }
            sendSync(serverPlayer);
        });
    }

    private static void handleComplete(OrinCompletePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            int stage = OrinQuestData.getStage(serverPlayer);
            String messageKey = null;
            if (payload.questId() == 1 && stage == OrinQuestData.STAGE_Q1_ACCEPTED) {
                OrinQuestData.setStage(serverPlayer, OrinQuestData.STAGE_Q1_COMPLETED);
                messageKey = "ashwake_quests_npcs.orin.quest1_complete";
            } else if (payload.questId() == 2 && stage == OrinQuestData.STAGE_Q2_ACCEPTED) {
                if (!OrinQuestData.isVisitedPersonal(serverPlayer)) {
                    serverPlayer.sendSystemMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "ashwake_quests_npcs.orin.quest2_needs_personal"));
                    sendSync(serverPlayer);
                    return;
                }
                OrinQuestData.setStage(serverPlayer, OrinQuestData.STAGE_Q2_COMPLETED);
                messageKey = "ashwake_quests_npcs.orin.quest2_complete";
            } else if (payload.questId() == 3 && stage == OrinQuestData.STAGE_Q3_ACCEPTED) {
                OrinQuestData.setStage(serverPlayer, OrinQuestData.STAGE_Q3_COMPLETED);
                messageKey = "ashwake_quests_npcs.orin.quest3_complete";
            } else if (payload.questId() == 4 && stage == OrinQuestData.STAGE_Q4_ACCEPTED) {
                OrinQuestData.setStage(serverPlayer, OrinQuestData.STAGE_Q4_COMPLETED);
                messageKey = "ashwake_quests_npcs.orin.quest4_complete";
                AshwakeQuestsNpcsMod.ensureVaelaPresent(serverPlayer.level().getServer().getLevel(Level.OVERWORLD));
            } else {
                return;
            }
            boolean quest2Complete = OrinQuestData.getStage(serverPlayer) >= OrinQuestData.STAGE_Q2_COMPLETED;
            if (quest2Complete) {
                OrinQuestData.setQuestCompleted(serverPlayer, true);
                if (GuidanceQuestData.isAccepted(serverPlayer) && !GuidanceQuestData.isCompleted(serverPlayer)) {
                    GuidanceQuestData.setCompleted(serverPlayer, true);
                    GuidanceNetwork.sendSync(serverPlayer, true, true);
                }
            }
            sendSync(serverPlayer);
            if (messageKey != null) {
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable(messageKey));
            }
        });
    }

    private static void handleReclaim(OrinReclaimPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!OrinQuestData.isQuestCompleted(serverPlayer)) {
                return;
            }

            ItemStack waystone = new ItemStack(AshwakeQuestsNpcsMod.ASHWAKE_WAYSTONE.get());
            if (serverPlayer.getInventory().contains(waystone)) {
                serverPlayer.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("ashwake_quests_npcs.waystone.already_have"));
                return;
            }

            if (!serverPlayer.getInventory().add(waystone)) {
                serverPlayer.drop(waystone, false);
            }
            OrinQuestData.setWaystoneGiven(serverPlayer, true);
            sendSync(serverPlayer);
            serverPlayer.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable("ashwake_quests_npcs.waystone.reclaimed"));
        });
    }

    public record OrinSyncPayload(int stage, boolean waystoneGiven, boolean visitedPersonal, boolean questCompleted)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OrinSyncPayload> TYPE = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(AshwakeQuestsNpcsMod.MODID, "orin_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OrinSyncPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeInt(payload.stage);
                    buf.writeBoolean(payload.waystoneGiven);
                    buf.writeBoolean(payload.visitedPersonal);
                    buf.writeBoolean(payload.questCompleted);
                },
                buf -> new OrinSyncPayload(buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean())
        );

        @Override
        public CustomPacketPayload.Type<OrinSyncPayload> type() {
            return TYPE;
        }
    }

    public record OrinAcceptPayload(byte questId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OrinAcceptPayload> TYPE = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(AshwakeQuestsNpcsMod.MODID, "orin_accept"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OrinAcceptPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeByte(payload.questId),
                buf -> new OrinAcceptPayload(buf.readByte()));

        @Override
        public CustomPacketPayload.Type<OrinAcceptPayload> type() {
            return TYPE;
        }
    }

    public record OrinCompletePayload(byte questId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OrinCompletePayload> TYPE = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(AshwakeQuestsNpcsMod.MODID, "orin_complete"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OrinCompletePayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeByte(payload.questId),
                buf -> new OrinCompletePayload(buf.readByte()));

        @Override
        public CustomPacketPayload.Type<OrinCompletePayload> type() {
            return TYPE;
        }
    }

    public record OrinReclaimPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OrinReclaimPayload> TYPE = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(AshwakeQuestsNpcsMod.MODID, "orin_reclaim"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OrinReclaimPayload> STREAM_CODEC =
                StreamCodec.unit(new OrinReclaimPayload());

        @Override
        public CustomPacketPayload.Type<OrinReclaimPayload> type() {
            return TYPE;
        }
    }
}
