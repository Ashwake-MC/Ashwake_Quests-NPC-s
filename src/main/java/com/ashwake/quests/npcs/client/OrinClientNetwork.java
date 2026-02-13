package com.ashwake.quests.npcs.client;

import com.ashwake.quests.npcs.network.OrinNetwork;

import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class OrinClientNetwork {
    private OrinClientNetwork() {
    }

    public static void registerClientHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(OrinNetwork.OrinSyncPayload.TYPE, OrinClientNetwork::handleSync);
    }

    private static void handleSync(OrinNetwork.OrinSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> OrinQuestState.setState(
                payload.stage(),
                payload.waystoneGiven(),
                payload.visitedPersonal(),
                payload.questCompleted()));
    }
}
