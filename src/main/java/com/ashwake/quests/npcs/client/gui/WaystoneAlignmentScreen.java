package com.ashwake.quests.npcs.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.ashwake.quests.npcs.AshwakeQuestsNpcsMod;
import com.ashwake.quests.npcs.network.WaystoneNetwork;
import com.ashwake.quests.npcs.network.WaystoneNetwork.WaystoneTeleportPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class WaystoneAlignmentScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 360;
    private static final int MAX_PANEL_HEIGHT = 240;
    private static final int MIN_PANEL_WIDTH = 240;
    private static final int MIN_PANEL_HEIGHT = 180;
    private static final int PANEL_PADDING = 16;
    private static final int HEADER_HEIGHT = 56;
    private static final int FOOTER_HEIGHT = 44;
    private static final int DEST_GAP = 10;
    private static final int SPARK_COUNT = 28;
    private static final long CHANNEL_DURATION_MS = 10_000L;
    private static final long FAIL_SHOW_MS = 1800L;

    private final RandomSource random = RandomSource.create();
    private final List<Spark> sparks = new ArrayList<>();

    private WaystoneCardButton hubButton;
    private WaystoneCardButton personalButton;

    private boolean channeling;
    private long channelStart;
    private long failAt;
    private WaystoneNetwork.Destination destination;
    private Vec3 startPos = Vec3.ZERO;
    private long openedAt;
    private int panelWidth;
    private int panelHeight;
    private int cardHeight;
    private int panelPadding;
    private int headerHeight;
    private int footerHeight;
    private int destGap;
    private boolean compact;

    public WaystoneAlignmentScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        this.openedAt = Util.getMillis();
        updateLayout();
        clearWidgets();

        int left = (this.width - this.panelWidth) / 2;
        int top = (this.height - this.panelHeight) / 2;
        int buttonWidth = this.panelWidth - this.panelPadding * 2;
        int firstY = top + this.headerHeight;

        this.hubButton = new WaystoneCardButton(
                left + this.panelPadding,
                firstY,
                buttonWidth,
                this.cardHeight,
                Component.translatable("ashwake_quests_npcs.waystone.dest_hub"),
                Component.translatable("ashwake_quests_npcs.waystone.dest_hub_desc"),
                WaystoneNetwork.Destination.HUB,
                button -> startChannel(WaystoneNetwork.Destination.HUB)
        );

        this.personalButton = new WaystoneCardButton(
                left + this.panelPadding,
                firstY + this.cardHeight + this.destGap,
                buttonWidth,
                this.cardHeight,
                Component.translatable("ashwake_quests_npcs.waystone.dest_personal"),
                Component.translatable("ashwake_quests_npcs.waystone.dest_personal_desc"),
                WaystoneNetwork.Destination.PERSONAL,
                button -> startChannel(WaystoneNetwork.Destination.PERSONAL)
        );

        addRenderableWidget(this.hubButton);
        addRenderableWidget(this.personalButton);
        updateButtonStates();
        initSparks();
    }

    @Override
    public void tick() {
        for (Spark spark : this.sparks) {
            spark.advance(this.random, this.panelWidth, this.panelHeight);
        }
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        if (this.channeling) {
            if (this.minecraft.player.hurtTime > 0) {
                cancelChannel();
                return;
            }
            Vec3 current = this.minecraft.player.position();
            if (current.distanceToSqr(this.startPos) > 0.0025) {
                cancelChannel();
                return;
            }

            spawnChannelParticles();

            long elapsed = Util.getMillis() - this.channelStart;
            if (elapsed >= CHANNEL_DURATION_MS) {
                finishChannel();
            }
        }

        updateButtonStates();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            cancelChannel();
        }
        return super.keyPressed(event);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int topColor = 0xC0100A08;
        int bottomColor = 0xC01B1411;
        guiGraphics.fillGradient(0, 0, this.width, this.height, topColor, bottomColor);
        int hazeColor = 0x0D000000;
        for (int x = 0; x < this.width; x += 18) {
            guiGraphics.fill(x, 0, x + 1, this.height, hazeColor);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        float openProgress = getOpenProgress(partialTick);
        float pulse = 0.5f + 0.5f * Mth.sin((Util.getMillis() - this.openedAt) * 0.0032f);
        setButtonAlpha(openProgress);
        updateButtonSelection();

        int left = (this.width - this.panelWidth) / 2;
        int top = (this.height - this.panelHeight) / 2;
        int right = left + this.panelWidth;
        int bottom = top + this.panelHeight;

        int glowAlpha = (int)(80 * openProgress + 60 * pulse);
        int glowColor = (glowAlpha << 24) | 0xD9752B;
        guiGraphics.fill(left - 8, top - 8, right + 8, bottom + 8, glowColor);

        int shadowAlpha = (int)(120 * openProgress);
        int shadowColor = (shadowAlpha << 24);
        guiGraphics.fill(left + 6, top + 8, right + 6, bottom + 8, shadowColor);

        int borderDark = 0xFF1A0F0C;
        int borderLight = 0xFF9B5327;
        int fillTop = withAlpha(0x1C110C, (int)(220 * openProgress));
        int fillBottom = withAlpha(0x24160F, (int)(220 * openProgress));

        guiGraphics.fill(left, top, right, bottom, borderDark);
        guiGraphics.fill(left + 1, top + 1, right - 1, bottom - 1, borderLight);
        guiGraphics.fillGradient(left + 2, top + 2, right - 2, bottom - 2, fillTop, fillBottom);

        renderEdgeHighlights(guiGraphics, left, top, right, bottom, openProgress, pulse);
        renderCornerAccents(guiGraphics, left, top, right, bottom, openProgress);
        renderEmberBand(guiGraphics, left, bottom, right, openProgress, pulse);
        renderRunes(guiGraphics, left, top, openProgress, pulse);
        renderSparks(guiGraphics, left, top, openProgress);

        int textLeft = left + this.panelPadding;
        int textTop = top + (this.compact ? 12 : 16);
        renderTitlePlate(guiGraphics, textLeft, textTop, openProgress);
        Component title = Component.translatable("ashwake_quests_npcs.waystone.title");
        Component subtitle = Component.translatable("ashwake_quests_npcs.waystone.subtitle");
        guiGraphics.drawString(this.font, title, textLeft, textTop, withAlpha(0xF2C08B, (int)(255 * openProgress)), false);
        if (!this.compact || this.panelHeight >= 200) {
            guiGraphics.drawString(this.font, subtitle, textLeft, textTop + (this.compact ? 12 : 14),
                    withAlpha(0xD7C08D, (int)(230 * openProgress)), false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        renderStatus(guiGraphics, left, bottom, openProgress);
        renderProgressBar(guiGraphics, left, bottom, openProgress, pulse);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void startChannel(WaystoneNetwork.Destination destination) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        if (isOnCooldown()) {
            return;
        }
        this.destination = destination;
        this.channeling = true;
        this.channelStart = Util.getMillis();
        this.startPos = this.minecraft.player.position();
        this.failAt = 0L;
        updateButtonStates();
    }

    private void finishChannel() {
        if (this.destination == null) {
            return;
        }
        ClientPacketDistributor.sendToServer(new WaystoneTeleportPayload(this.destination.id()));
        this.channeling = false;
        this.minecraft.setScreen(null);
    }

    private void cancelChannel() {
        if (!this.channeling) {
            return;
        }
        this.channeling = false;
        this.destination = null;
        this.failAt = Util.getMillis();
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean disable = this.channeling || isOnCooldown();
        if (this.hubButton != null) {
            this.hubButton.active = !disable;
        }
        if (this.personalButton != null) {
            this.personalButton.active = !disable;
        }
    }

    private boolean isOnCooldown() {
        return this.minecraft != null
                && this.minecraft.player != null
                && this.minecraft.player.getCooldowns()
                .isOnCooldown(AshwakeQuestsNpcsMod.ASHWAKE_WAYSTONE.get().getDefaultInstance());
    }

    private Component getStatusLine() {
        if (this.channeling) {
            long elapsed = Util.getMillis() - this.channelStart;
            long remaining = Math.max(0L, (CHANNEL_DURATION_MS - elapsed + 999) / 1000);
            return Component.translatable("ashwake_quests_npcs.waystone.status_channeling", remaining);
        }
        if (this.failAt > 0 && Util.getMillis() - this.failAt < FAIL_SHOW_MS) {
            return Component.translatable("ashwake_quests_npcs.waystone.status_failed");
        }
        if (isOnCooldown()) {
            return Component.translatable("ashwake_quests_npcs.waystone.status_cooldown");
        }
        return Component.translatable("ashwake_quests_npcs.waystone.status_standing");
    }

    private void renderStatus(GuiGraphics guiGraphics, int left, int bottom, float openProgress) {
        Component status = getStatusLine();
        int y = bottom - this.panelPadding - this.font.lineHeight - 10;
        guiGraphics.drawString(this.font, status, left + this.panelPadding, y,
                withAlpha(0xD7C08D, (int)(240 * openProgress)), false);
    }

    private void renderProgressBar(GuiGraphics guiGraphics, int left, int bottom, float openProgress, float pulse) {
        int barLeft = left + this.panelPadding;
        int barTop = bottom - this.panelPadding - 8;
        int barWidth = this.panelWidth - this.panelPadding * 2;
        int barHeight = this.compact ? 5 : 6;
        guiGraphics.fill(barLeft, barTop, barLeft + barWidth, barTop + barHeight, withAlpha(0x2C1A12, (int)(200 * openProgress)));

        float progress = 0.0f;
        int fillColor = withAlpha(0xB86A34, (int)(220 * openProgress));
        if (this.channeling) {
            long elapsed = Util.getMillis() - this.channelStart;
            progress = Mth.clamp((float)elapsed / (float)CHANNEL_DURATION_MS, 0.0f, 1.0f);
        } else if (this.failAt > 0 && Util.getMillis() - this.failAt < FAIL_SHOW_MS) {
            progress = 1.0f;
            fillColor = withAlpha(0x7B2F2A, (int)(230 * openProgress));
        }

        int fillWidth = (int)(barWidth * progress);
        guiGraphics.fill(barLeft, barTop, barLeft + fillWidth, barTop + barHeight, fillColor);
        int glowAlpha = (int)(80 * openProgress + 40 * pulse);
        int glowColor = (glowAlpha << 24) | 0xE08A43;
        guiGraphics.fill(barLeft, barTop - 1, barLeft + fillWidth, barTop, glowColor);
    }

    private void spawnChannelParticles() {
        if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) {
            return;
        }
        double baseX = this.minecraft.player.getX();
        double baseY = this.minecraft.player.getY() + 0.7;
        double baseZ = this.minecraft.player.getZ();
        for (int i = 0; i < 2; i++) {
            double offsetX = (this.minecraft.player.getRandom().nextDouble() - 0.5) * 0.6;
            double offsetY = this.minecraft.player.getRandom().nextDouble() * 0.8;
            double offsetZ = (this.minecraft.player.getRandom().nextDouble() - 0.5) * 0.6;
            this.minecraft.level.addParticle(ParticleTypes.ASH,
                    baseX + offsetX,
                    baseY + offsetY,
                    baseZ + offsetZ,
                    0.0, 0.01, 0.0);
        }
    }

    private void updateLayout() {
        int availableWidth = Math.max(220, this.width - 32);
        int availableHeight = Math.max(170, this.height - 32);
        this.compact = availableWidth < 300 || availableHeight < 220;
        this.panelPadding = this.compact ? 10 : PANEL_PADDING;
        this.headerHeight = this.compact ? 44 : HEADER_HEIGHT;
        this.footerHeight = this.compact ? 36 : FOOTER_HEIGHT;
        this.destGap = this.compact ? 6 : DEST_GAP;
        this.panelWidth = Mth.clamp(availableWidth, MIN_PANEL_WIDTH, MAX_PANEL_WIDTH);
        this.panelHeight = Mth.clamp(availableHeight, MIN_PANEL_HEIGHT, MAX_PANEL_HEIGHT);
        int availableCards = this.panelHeight - this.headerHeight - this.footerHeight - this.destGap;
        int minCard = this.compact ? 34 : 38;
        int maxCard = this.compact ? 52 : 62;
        this.cardHeight = Mth.clamp(availableCards / 2, minCard, maxCard);
    }

    private void updateButtonSelection() {
        boolean selecting = this.channeling && this.destination != null;
        if (this.hubButton != null) {
            this.hubButton.setSelected(selecting && this.destination == WaystoneNetwork.Destination.HUB);
        }
        if (this.personalButton != null) {
            this.personalButton.setSelected(selecting && this.destination == WaystoneNetwork.Destination.PERSONAL);
        }
    }

    private void setButtonAlpha(float alpha) {
        if (this.hubButton != null) {
            this.hubButton.setAlpha(alpha);
        }
        if (this.personalButton != null) {
            this.personalButton.setAlpha(alpha);
        }
    }

    private float getOpenProgress(float partialTick) {
        float raw = (Util.getMillis() - this.openedAt + (long)(partialTick * 50.0f)) / 300.0f;
        raw = Mth.clamp(raw, 0.0f, 1.0f);
        return raw * raw * (3.0f - 2.0f * raw);
    }

    private void renderEdgeHighlights(
            GuiGraphics guiGraphics,
            int left,
            int top,
            int right,
            int bottom,
            float openProgress,
            float pulse
    ) {
        int highlightAlpha = (int)(90 * openProgress + 40 * pulse);
        int highlightColor = (highlightAlpha << 24) | 0xE49A57;
        guiGraphics.fill(left + 2, top + 2, right - 2, top + 3, highlightColor);

        int shadowAlpha = (int)(80 * openProgress);
        int shadowColor = (shadowAlpha << 24);
        guiGraphics.fill(left + 3, bottom - 3, right - 3, bottom - 2, shadowColor);
    }

    private void renderCornerAccents(GuiGraphics guiGraphics, int left, int top, int right, int bottom, float openProgress) {
        int accentAlpha = (int)(120 * openProgress);
        int accentColor = (accentAlpha << 24) | 0xD27B3B;
        int inset = 4;
        int size = 8;

        guiGraphics.fill(left + inset, top + inset, left + inset + size, top + inset + 1, accentColor);
        guiGraphics.fill(left + inset, top + inset, left + inset + 1, top + inset + size, accentColor);

        guiGraphics.fill(right - inset - size, top + inset, right - inset, top + inset + 1, accentColor);
        guiGraphics.fill(right - inset - 1, top + inset, right - inset, top + inset + size, accentColor);

        guiGraphics.fill(left + inset, bottom - inset - 1, left + inset + size, bottom - inset, accentColor);
        guiGraphics.fill(left + inset, bottom - inset - size, left + inset + 1, bottom - inset, accentColor);

        guiGraphics.fill(right - inset - size, bottom - inset - 1, right - inset, bottom - inset, accentColor);
        guiGraphics.fill(right - inset - 1, bottom - inset - size, right - inset, bottom - inset, accentColor);
    }

    private void renderTitlePlate(GuiGraphics guiGraphics, int textLeft, int textTop, float openProgress) {
        int titleWidth = Math.min(this.panelWidth - this.panelPadding * 2, this.font.width(
                Component.translatable("ashwake_quests_npcs.waystone.title")) + 20);
        int plateLeft = textLeft - 8;
        int plateTop = textTop - (this.compact ? 5 : 6);
        int plateHeight = this.compact ? 12 : 14;
        int plateColor = withAlpha(0x2B180F, (int)(210 * openProgress));
        int edgeColor = withAlpha(0xC87A3E, (int)(200 * openProgress));
        guiGraphics.fill(plateLeft, plateTop, plateLeft + titleWidth, plateTop + plateHeight, plateColor);
        guiGraphics.fill(plateLeft, plateTop, plateLeft + titleWidth, plateTop + 1, edgeColor);
    }

    private void renderEmberBand(
            GuiGraphics guiGraphics,
            int left,
            int bottom,
            int right,
            float openProgress,
            float pulse
    ) {
        int bandTop = bottom - 30;
        int bandBottom = bottom - 8;
        int bandAlpha = (int)(60 * openProgress + 30 * pulse);
        int bandTopColor = (bandAlpha << 24) | 0x4B2612;
        int bandBottomColor = withAlpha(0x2A130A, (int)(20 * openProgress));
        guiGraphics.fillGradient(left + 3, bandTop, right - 3, bandBottom, bandTopColor, bandBottomColor);
    }

    private void renderRunes(GuiGraphics guiGraphics, int left, int top, float openProgress, float pulse) {
        long time = Util.getMillis() - this.openedAt;
        int runeCount = Math.max(6, this.panelWidth / 45);
        int areaWidth = this.panelWidth - 28;
        int spacing = areaWidth / runeCount;
        for (int i = 0; i < runeCount; i++) {
            float wave = 0.5f + 0.5f * Mth.sin(time * 0.003f + i);
            int alpha = (int)(70 * openProgress + 60 * wave);
            int color = (alpha << 24) | 0xC0733B;
            int x = left + 14 + (i * spacing);
            int y = top + 8 + (int)(2 * Mth.sin(time * 0.002f + i));
            int height = 6 + (int)(4 * wave);
            guiGraphics.fill(x, y, x + 2, y + height, color);
        }

        int lineAlpha = (int)(60 * openProgress + 30 * pulse);
        int lineColor = (lineAlpha << 24) | 0x8B4B28;
        guiGraphics.fill(left + 12, top + 28, left + this.panelWidth - 12, top + 29, lineColor);
    }

    private void renderSparks(GuiGraphics guiGraphics, int left, int top, float openProgress) {
        for (Spark spark : this.sparks) {
            int alpha = (int)(120 * openProgress + 60 * (1.0f - (spark.y / this.panelHeight)));
            int color = (Mth.clamp(alpha, 0, 255) << 24) | 0xF2A046;
            int x = left + (int)spark.x;
            int y = top + (int)spark.y;
            guiGraphics.fill(x, y, x + spark.size, y + spark.size, color);
        }
    }

    private void initSparks() {
        this.sparks.clear();
        for (int i = 0; i < SPARK_COUNT; i++) {
            Spark spark = new Spark();
            spark.reset(this.random, this.panelWidth, this.panelHeight, true);
            this.sparks.add(spark);
        }
    }

    private static int withAlpha(int rgb, int alpha) {
        int clamped = Mth.clamp(alpha, 0, 255);
        return (clamped << 24) | (rgb & 0x00FFFFFF);
    }

    private static final class WaystoneCardButton extends Button {
        private final Component title;
        private final Component description;
        private final WaystoneNetwork.Destination destination;
        private boolean selected;

        private WaystoneCardButton(
                int x,
                int y,
                int width,
                int height,
                Component title,
                Component description,
                WaystoneNetwork.Destination destination,
                OnPress onPress
        ) {
            super(x, y, width, height, title, onPress, DEFAULT_NARRATION);
            this.title = title;
            this.description = description;
            this.destination = destination;
        }

        private void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();
            boolean hovered = this.isHoveredOrFocused();
            float alpha = this.alpha;
            boolean active = this.active;

            int border = hovered ? 0xFFB66A33 : 0xFF6E3C1F;
            int top = hovered ? 0xFF2A1A12 : 0xFF22140E;
            int bottom = hovered ? 0xFF24130B : 0xFF1B0F0A;
            if (this.selected) {
                border = 0xFFE0A052;
                top = 0xFF342014;
                bottom = 0xFF24130C;
            }

            int glowAlpha = hovered || this.selected ? 120 : 70;
            int glowColor = (applyAlpha(glowAlpha, alpha) << 24) | 0xE08A43;
            guiGraphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, glowColor);

            guiGraphics.fill(x, y, x + w, y + h, applyAlphaToRgb(border, alpha));
            guiGraphics.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1,
                    applyAlphaToRgb(top, alpha),
                    applyAlphaToRgb(bottom, alpha));
            guiGraphics.fill(x + 1, y + 1, x + w - 1, y + 2, applyAlphaToRgb(0xFF8F4F28, alpha));

            boolean compactCard = h < 40;
            int iconX = x + 8;
            int iconY = y + (compactCard ? 6 : 8);
            int iconColor = this.selected ? 0xFFE9B96E : 0xFFC98A5B;
            guiGraphics.fill(iconX, iconY + 6, iconX + 6, iconY + 7, applyAlphaToRgb(iconColor, alpha));
            guiGraphics.fill(iconX + 2, iconY + 2, iconX + 4, iconY + 10, applyAlphaToRgb(iconColor, alpha));

            int titleColor = this.selected ? 0xFFF6D5A6 : 0xFFF0B57A;
            int titleY = y + (compactCard ? 4 : 6);
            guiGraphics.drawString(Minecraft.getInstance().font, this.title, x + 20, titleY,
                    applyAlphaToRgb(titleColor, alpha), false);

            List<FormattedCharSequence> wrappedDesc = Minecraft.getInstance().font.split(this.description, w - 26);
            int lineY = y + (compactCard ? 16 : 20);
            int maxLines = Math.max(1, (h - (lineY - y) - 4) / Minecraft.getInstance().font.lineHeight);
            int shown = 0;
            for (FormattedCharSequence line : wrappedDesc) {
                if (shown >= maxLines) {
                    break;
                }
                guiGraphics.drawString(Minecraft.getInstance().font, line, x + 20, lineY,
                        applyAlphaToRgb(0xFFCDAE8D, alpha), false);
                lineY += Minecraft.getInstance().font.lineHeight;
                shown++;
            }

            if (!active) {
                guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, applyAlphaToRgb(0xAA120B08, alpha));
            }
            if (this.selected) {
                Component tag = Component.translatable("ashwake_quests_npcs.waystone.channeling_tag");
                int tagWidth = Minecraft.getInstance().font.width(tag);
                guiGraphics.drawString(Minecraft.getInstance().font, tag, x + w - tagWidth - 8, titleY,
                        applyAlphaToRgb(0xFFE6C18E, alpha), false);
            }
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

    private static final class Spark {
        private float x;
        private float y;
        private float speed;
        private float drift;
        private int size;

        private void reset(RandomSource random, int width, int height, boolean randomOffset) {
            float margin = 12.0f;
            this.x = margin + random.nextFloat() * (width - margin * 2.0f);
            float offset = randomOffset ? random.nextFloat() * height : 0.0f;
            this.y = height - margin + offset;
            this.speed = 0.25f + random.nextFloat() * 0.55f;
            this.drift = -0.18f + random.nextFloat() * 0.36f;
            this.size = 1 + random.nextInt(2);
        }

        private void advance(RandomSource random, int width, int height) {
            this.y -= this.speed;
            this.x += this.drift;
            if (this.y < 6.0f || this.x < 6.0f || this.x > width - 6.0f) {
                reset(random, width, height, false);
            }
        }
    }
}
