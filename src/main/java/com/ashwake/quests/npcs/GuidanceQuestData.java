package com.ashwake.quests.npcs;

import net.minecraft.world.entity.player.Player;

public final class GuidanceQuestData {
    private static final String KEY = AshwakeQuestsNpcsMod.MODID + ":guidance_accepted";
    private static final String COMPLETED_KEY = AshwakeQuestsNpcsMod.MODID + ":guidance_completed";

    private GuidanceQuestData() {
    }

    public static boolean isAccepted(Player player) {
        return player.getPersistentData().getBooleanOr(KEY, false);
    }

    public static void setAccepted(Player player, boolean accepted) {
        player.getPersistentData().putBoolean(KEY, accepted);
    }

    public static boolean isCompleted(Player player) {
        return player.getPersistentData().getBooleanOr(COMPLETED_KEY, false);
    }

    public static void setCompleted(Player player, boolean completed) {
        player.getPersistentData().putBoolean(COMPLETED_KEY, completed);
    }
}
