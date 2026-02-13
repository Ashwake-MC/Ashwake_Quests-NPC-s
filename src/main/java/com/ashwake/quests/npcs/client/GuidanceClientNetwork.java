package com.ashwake.quests.npcs.client;

import com.ashwake.quests.npcs.network.GuidanceNetwork;

import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class GuidanceClientNetwork {
    private GuidanceClientNetwork() {
    }

    public static void registerClientHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(GuidanceNetwork.GuidanceSyncPayload.TYPE, GuidanceClientNetwork::handleSync);
    }

    private static void handleSync(GuidanceNetwork.GuidanceSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            GuidanceQuestState.setAccepted(payload.accepted());
            GuidanceQuestState.setCompleted(payload.completed());
        });
    }
}
