package com.ashwake.quests.npcs.client.gui;

import com.ashwake.quests.npcs.AshwakeQuestsNpcsMod;
import com.ashwake.quests.npcs.client.OrinQuestState;
import com.ashwake.quests.npcs.network.OrinNetwork.OrinReclaimPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class OrinReclaimScreen extends Screen {
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 140;
    private static final int PANEL_PADDING = 14;

    private Button reclaimButton;

    public OrinReclaimScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int buttonWidth = PANEL_WIDTH - PANEL_PADDING * 2;
        int buttonHeight = 22;
        int buttonY = top + PANEL_HEIGHT - PANEL_PADDING - buttonHeight;

        this.reclaimButton = new ReclaimButton(
                left + PANEL_PADDING,
                buttonY,
                buttonWidth,
                buttonHeight,
                Component.translatable("ashwake_quests_npcs.orin.shop.reclaim"),
                button -> requestReclaim()
        );
        addRenderableWidget(this.reclaimButton);
        updateButtonState();
    }

    @Override
    public void tick() {
        updateButtonState();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xB0100A08, 0xB0191411);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;

        guiGraphics.fill(left - 6, top - 6, right + 6, bottom + 6, 0x3A000000);
        guiGraphics.fill(left, top, right, bottom, 0xFF1A0F0C);
        guiGraphics.fillGradient(left + 1, top + 1, right - 1, bottom - 1, 0xFF25160F, 0xFF1B110C);
        guiGraphics.fill(left + 1, top + 1, right - 1, top + 2, 0xFF8F4F28);

        Component title = Component.translatable("ashwake_quests_npcs.orin.shop.title");
        Component subtitle = Component.translatable("ashwake_quests_npcs.orin.shop.subtitle");
        guiGraphics.drawString(this.font, title, left + PANEL_PADDING, top + 16, 0xF0B57A, false);
        guiGraphics.drawString(this.font, subtitle, left + PANEL_PADDING, top + 30, 0xD7C08D, false);

        Component status = getStatusLine();
        guiGraphics.drawString(this.font, status, left + PANEL_PADDING, bottom - PANEL_PADDING - 28, 0xCDAE8D, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void updateButtonState() {
        if (this.reclaimButton == null) {
            return;
        }
        this.reclaimButton.active = OrinQuestState.isQuestCompleted() && !hasWaystone();
    }

    private boolean hasWaystone() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        return this.minecraft.player.getInventory()
                .contains(AshwakeQuestsNpcsMod.ASHWAKE_WAYSTONE.get().getDefaultInstance());
    }

    private Component getStatusLine() {
        if (!OrinQuestState.isQuestCompleted()) {
            return Component.translatable("ashwake_quests_npcs.orin.shop.locked");
        }
        if (hasWaystone()) {
            return Component.translatable("ashwake_quests_npcs.orin.shop.have");
        }
        return Component.translatable("ashwake_quests_npcs.orin.shop.ready");
    }

    private void requestReclaim() {
        ClientPacketDistributor.sendToServer(new OrinReclaimPayload());
    }

    private static final class ReclaimButton extends Button {
        protected ReclaimButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();
            boolean hovered = this.isHoveredOrFocused();
            float alpha = this.alpha;
            int glowAlpha = hovered ? 120 : 70;
            int glowColor = (applyAlpha(glowAlpha, alpha) << 24) | 0xE08A43;
            guiGraphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, glowColor);

            int topColor = hovered ? 0xFF3B1F13 : 0xFF2A160F;
            int bottomColor = hovered ? 0xFF2A140D : 0xFF1E110B;
            int borderColor = hovered ? 0xFFB76A36 : 0xFF7B3F22;
            guiGraphics.fill(x, y, x + w, y + h, applyAlphaToRgb(topColor, alpha));
            guiGraphics.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1,
                    applyAlphaToRgb(topColor, alpha),
                    applyAlphaToRgb(bottomColor, alpha));
            guiGraphics.fill(x, y, x + w, y + 1, applyAlphaToRgb(borderColor, alpha));
            guiGraphics.fill(x, y + h - 1, x + w, y + h, applyAlphaToRgb(borderColor, alpha));

            int textColor = hovered ? 0xFFEAC59A : 0xFFD6B28B;
            int textX = x + w / 2 - Minecraft.getInstance().font.width(this.getMessage()) / 2;
            int textY = y + (h - Minecraft.getInstance().font.lineHeight) / 2 + 1;
            guiGraphics.drawString(Minecraft.getInstance().font, this.getMessage(), textX, textY,
                    applyAlphaToRgb(textColor, alpha), false);
        }

        private static int applyAlpha(int alpha, float multiplier) {
            int clamped = Mth.clamp(alpha, 0, 255);
            return Mth.clamp((int)(clamped * multiplier), 0, 255);
        }

        private static int applyAlphaToRgb(int argb, float multiplier) {
            int alpha = (argb >>> 24) & 0xFF;
            int rgb = argb & 0x00FFFFFF;
            int blended = applyAlpha(alpha, multiplier);
            return (blended << 24) | rgb;
        }
    }
}
