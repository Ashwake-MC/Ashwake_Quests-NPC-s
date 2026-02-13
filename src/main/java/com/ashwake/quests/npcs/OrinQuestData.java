package com.ashwake.quests.npcs;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class OrinQuestData {
    private static final String STAGE_KEY = AshwakeQuestsNpcsMod.MODID + ":orin_quest_stage";
    private static final String WAYSTONE_KEY = AshwakeQuestsNpcsMod.MODID + ":orin_waystone_given";
    private static final String VISITED_KEY = AshwakeQuestsNpcsMod.MODID + ":orin_personal_visited";
    private static final String COMPLETED_KEY = AshwakeQuestsNpcsMod.MODID + ":orin_world_unshaped_completed";
    private static final String PERSONAL_SPAWN_X = AshwakeQuestsNpcsMod.MODID + ":personal_spawn_x";
    private static final String PERSONAL_SPAWN_Y = AshwakeQuestsNpcsMod.MODID + ":personal_spawn_y";
    private static final String PERSONAL_SPAWN_Z = AshwakeQuestsNpcsMod.MODID + ":personal_spawn_z";
    private static final String RETURN_DIM_KEY = AshwakeQuestsNpcsMod.MODID + ":waystone_return_dim";
    private static final String RETURN_X_KEY = AshwakeQuestsNpcsMod.MODID + ":waystone_return_x";
    private static final String RETURN_Y_KEY = AshwakeQuestsNpcsMod.MODID + ":waystone_return_y";
    private static final String RETURN_Z_KEY = AshwakeQuestsNpcsMod.MODID + ":waystone_return_z";

    public static final int STAGE_NONE = 0;
    public static final int STAGE_Q1_ACCEPTED = 1;
    public static final int STAGE_Q1_COMPLETED = 2;
    public static final int STAGE_Q2_ACCEPTED = 3;
    public static final int STAGE_Q2_COMPLETED = 4;
    public static final int STAGE_Q3_ACCEPTED = 5;
    public static final int STAGE_Q3_COMPLETED = 6;
    public static final int STAGE_Q4_ACCEPTED = 7;
    public static final int STAGE_Q4_COMPLETED = 8;

    private OrinQuestData() {
    }

    public static boolean isWaystoneGiven(Player player) {
        return player.getPersistentData().getBooleanOr(WAYSTONE_KEY, false);
    }

    public static void setWaystoneGiven(Player player, boolean given) {
        player.getPersistentData().putBoolean(WAYSTONE_KEY, given);
    }

    public static int getStage(Player player) {
        return player.getPersistentData().getIntOr(STAGE_KEY, STAGE_NONE);
    }

    public static void setStage(Player player, int stage) {
        player.getPersistentData().putInt(STAGE_KEY, stage);
    }

    public static boolean isQuestCompleted(Player player) {
        return getStage(player) >= STAGE_Q2_COMPLETED
                || player.getPersistentData().getBooleanOr(COMPLETED_KEY, false);
    }

    public static boolean isVisitedPersonal(Player player) {
        return player.getPersistentData().getBooleanOr(VISITED_KEY, false);
    }

    public static void setVisitedPersonal(Player player, boolean visited) {
        player.getPersistentData().putBoolean(VISITED_KEY, visited);
    }

    public static boolean hasPersonalSpawn(Player player) {
        return player.getPersistentData().contains(PERSONAL_SPAWN_X);
    }

    public static BlockPos getPersonalSpawn(Player player, BlockPos fallback) {
        if (!hasPersonalSpawn(player)) {
            return fallback;
        }
        int x = player.getPersistentData().getIntOr(PERSONAL_SPAWN_X, fallback.getX());
        int y = player.getPersistentData().getIntOr(PERSONAL_SPAWN_Y, fallback.getY());
        int z = player.getPersistentData().getIntOr(PERSONAL_SPAWN_Z, fallback.getZ());
        return new BlockPos(x, y, z);
    }

    public static void setPersonalSpawn(Player player, BlockPos pos) {
        player.getPersistentData().putInt(PERSONAL_SPAWN_X, pos.getX());
        player.getPersistentData().putInt(PERSONAL_SPAWN_Y, pos.getY());
        player.getPersistentData().putInt(PERSONAL_SPAWN_Z, pos.getZ());
    }

    public static boolean hasReturnLocation(Player player) {
        return player.getPersistentData().contains(RETURN_DIM_KEY)
                && player.getPersistentData().contains(RETURN_X_KEY)
                && player.getPersistentData().contains(RETURN_Y_KEY)
                && player.getPersistentData().contains(RETURN_Z_KEY);
    }

    public static void setReturnLocation(Player player, ResourceKey<Level> dimension, Vec3 pos) {
        player.getPersistentData().putString(RETURN_DIM_KEY, dimension.identifier().toString());
        player.getPersistentData().putDouble(RETURN_X_KEY, pos.x);
        player.getPersistentData().putDouble(RETURN_Y_KEY, pos.y);
        player.getPersistentData().putDouble(RETURN_Z_KEY, pos.z);
    }

    public static ReturnLocation getReturnLocation(Player player) {
        String dimensionId = player.getPersistentData().getStringOr(RETURN_DIM_KEY, "");
        if (dimensionId.isEmpty()) {
            return null;
        }
        Identifier identifier = Identifier.parse(dimensionId);
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, identifier);
        double x = player.getPersistentData().getDoubleOr(RETURN_X_KEY, 0.0);
        double y = player.getPersistentData().getDoubleOr(RETURN_Y_KEY, 0.0);
        double z = player.getPersistentData().getDoubleOr(RETURN_Z_KEY, 0.0);
        return new ReturnLocation(dimension, new Vec3(x, y, z));
    }

    public record ReturnLocation(ResourceKey<Level> dimension, Vec3 pos) {
    }

    public static void setQuestCompleted(Player player, boolean completed) {
        if (completed) {
            player.getPersistentData().putBoolean(COMPLETED_KEY, true);
            if (getStage(player) < STAGE_Q2_COMPLETED) {
                setStage(player, STAGE_Q2_COMPLETED);
            }
        } else {
            player.getPersistentData().putBoolean(COMPLETED_KEY, false);
        }
    }
}
