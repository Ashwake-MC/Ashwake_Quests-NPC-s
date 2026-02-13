package com.ashwake.quests.npcs.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.ashwake.quests.npcs.client.gui.WaystoneAlignmentScreen;

public class AshwakeWaystoneItem extends Item {
    public AshwakeWaystoneItem(Properties properties) {
        super(properties);
    }

    @Override
    @Deprecated
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag
    ) {
        tooltipAdder.accept(Component.translatable("item.ashwake_quests_npcs.ashwake_waystone.tooltip_title")
                .withStyle(ChatFormatting.GOLD));
        tooltipAdder.accept(Component.translatable("item.ashwake_quests_npcs.ashwake_waystone.desc")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltipAdder.accept(Component.translatable("item.ashwake_quests_npcs.ashwake_waystone.tooltip_hint")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            if (player.getCooldowns().isOnCooldown(stack)) {
                player.displayClientMessage(Component.translatable("ashwake_quests_npcs.waystone.cooldown"), true);
                return InteractionResult.FAIL;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (!(minecraft.screen instanceof WaystoneAlignmentScreen)) {
                minecraft.setScreen(new WaystoneAlignmentScreen());
            }
        }
        return InteractionResult.SUCCESS;
    }
}
