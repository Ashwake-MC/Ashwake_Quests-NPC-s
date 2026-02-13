package com.ashwake.quests.npcs.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

public class AshwakeDialogScreen extends Screen {
    public static final String PAGE_BREAK_MARKER = "[[PAGE]]";
    private static final int MAX_PANEL_WIDTH = 360;
    private static final int MAX_PANEL_HEIGHT = 220;
    private static final int MIN_PANEL_HEIGHT = 160;
    private static final int MIN_PANEL_WIDTH = 200;
    private static final int PANEL_PADDING = 14;
    private static final int SPARK_COUNT = 32;
    private static final int CORNER_ACCENT = 8;
    private static final int SIDE_BUTTON_GAP = 10;
    private static final int SIDE_BUTTON_MIN_WIDTH = 90;
    private static final int SIDE_BUTTON_MAX_WIDTH = 160;

    private final Component speaker;
    private final List<String> paragraphs;
    private final RandomSource random = RandomSource.create();
    private final List<Spark> sparks = new ArrayList<>();
    private final List<List<PageLine>> pages = new ArrayList<>();
    private final Runnable acceptAction;
    private final BooleanSupplier canAccept;
    private final Runnable completeAction;
    private final BooleanSupplier canComplete;
    private final Component sideActionLabel;
    private final Runnable sideAction;
    private final BooleanSupplier canSideAction;

    private long openedAt;
    private int panelWidth;
    private int panelHeight;
    private int lineHeight;
    private int pageIndex;
    private int sideButtonWidth;

    private AshwakeButton closeButton;
    private AshwakeButton nextButton;
    private AshwakeButton prevButton;
    private AshwakeButton acceptButton;
    private AshwakeButton sideButton;

    public AshwakeDialogScreen(Component speaker, Component body) {
        this(speaker, List.of(body.getString()), null, null, null, null, null, null, null);
    }

    public AshwakeDialogScreen(Component speaker, List<String> paragraphs) {
        this(speaker, paragraphs, null, null, null, null, null, null, null);
    }

    public AshwakeDialogScreen(
            Component speaker,
            List<String> paragraphs,
            Runnable acceptAction,
            BooleanSupplier canAccept,
            Runnable completeAction,
            BooleanSupplier canComplete
    ) {
        this(speaker, paragraphs, acceptAction, canAccept, completeAction, canComplete, null, null, null);
    }

    public AshwakeDialogScreen(
            Component speaker,
            List<String> paragraphs,
            Runnable acceptAction,
            BooleanSupplier canAccept,
            Runnable completeAction,
            BooleanSupplier canComplete,
            Component sideActionLabel,
            Runnable sideAction,
            BooleanSupplier canSideAction
    ) {
        super(Component.empty());
        this.speaker = speaker;
        this.paragraphs = paragraphs.isEmpty() ? List.of("") : new ArrayList<>(paragraphs);
        this.acceptAction = acceptAction;
        this.canAccept = canAccept;
        this.completeAction = completeAction;
        this.canComplete = canComplete;
        this.sideActionLabel = sideActionLabel;
        this.sideAction = sideAction;
        this.canSideAction = canSideAction;
    }

    @Override
    protected void init() {
        this.openedAt = Util.getMillis();
        updateLayout();
        this.clearWidgets();
        this.sideButton = null;

        int buttonHeight = 20;
        int buttonSpacing = 6;
        int availableWidth = this.panelWidth - PANEL_PADDING * 2;
        int buttonCount = 4;
        int buttonWidth = Math.min(90, Math.max(48, (availableWidth - buttonSpacing * (buttonCount - 1)) / buttonCount));

        int left = getPanelLeft();
        int top = getPanelTop();
        int buttonY = top + this.panelHeight - PANEL_PADDING - buttonHeight;

        int prevX = left + PANEL_PADDING;
        int nextX = prevX + buttonWidth + buttonSpacing;
        int actionX = nextX + buttonWidth + buttonSpacing;
        int closeX = actionX + buttonWidth + buttonSpacing;

        this.prevButton = new AshwakeButton(prevX, buttonY, buttonWidth, buttonHeight,
                Component.translatable("ashwake_quests_npcs.dialog.prev"),
                button -> previousPage());
        this.nextButton = new AshwakeButton(nextX, buttonY, buttonWidth, buttonHeight,
                Component.translatable("ashwake_quests_npcs.dialog.next"),
                button -> nextPage());
        this.acceptButton = new AshwakeButton(actionX, buttonY, buttonWidth, buttonHeight,
                Component.translatable("ashwake_quests_npcs.dialog.accept"),
                button -> handleAction());
        this.closeButton = new AshwakeButton(closeX, buttonY, buttonWidth, buttonHeight,
                Component.translatable("ashwake_quests_npcs.dialog.close"),
                button -> onClose());

        this.addRenderableWidget(this.prevButton);
        this.addRenderableWidget(this.nextButton);
        this.addRenderableWidget(this.acceptButton);
        this.addRenderableWidget(this.closeButton);
        if (hasSideAction()) {
            int sideX = left + this.panelWidth + SIDE_BUTTON_GAP;
            this.sideButton = new AshwakeButton(sideX, buttonY, this.sideButtonWidth, buttonHeight,
                    this.sideActionLabel,
                    button -> handleSideAction());
            this.addRenderableWidget(this.sideButton);
        }

        updateButtonStates();
    }

    @Override
    public void tick() {
        for (Spark spark : this.sparks) {
            spark.advance(this.random, this.panelWidth, this.panelHeight);
        }
        updateButtonStates();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RIGHT || event.key() == InputConstants.KEY_D || event.key() == InputConstants.KEY_PAGEDOWN) {
            if (nextPage()) {
                return true;
            }
        }
        if (event.key() == InputConstants.KEY_LEFT || event.key() == InputConstants.KEY_A || event.key() == InputConstants.KEY_PAGEUP) {
            if (previousPage()) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0.0) {
            if (nextPage()) {
                return true;
            }
        } else if (scrollY > 0.0) {
            if (previousPage()) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        this.minecraft.gui.renderDeferredSubtitles();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        float openProgress = getOpenProgress(partialTick);
        float pulse = 0.5f + 0.5f * Mth.sin((Util.getMillis() - this.openedAt) * 0.0032f);
        setButtonAlpha(openProgress);

        int left = getPanelLeft();
        int top = getPanelTop();
        int right = left + this.panelWidth;
        int bottom = top + this.panelHeight;

        int glowAlpha = (int)(70 * openProgress + 50 * pulse);
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

        int textLeft = left + PANEL_PADDING;
        int textTop = top + PANEL_PADDING;
        int buttonY = bottom - PANEL_PADDING - 20;
        renderTitlePlate(guiGraphics, textLeft, textTop, openProgress);

        int titleColor = withAlpha(0xF2C08B, (int)(255 * openProgress));
        guiGraphics.drawString(this.font, this.speaker, textLeft, textTop, titleColor, false);

        int bodyY = textTop + 16;
        List<PageLine> pageLines = getCurrentPageLines();
        for (PageLine line : pageLines) {
            int lineX = textLeft + line.style.indent;
            int textColor = line.style.pickColor(openProgress);
            if (line.style == LineStyle.HEADING) {
                int underlineWidth = Math.min(this.panelWidth - PANEL_PADDING * 2, this.font.width(line.text) + 8);
                int underlineY = bodyY + this.lineHeight - 2;
                guiGraphics.fill(lineX, underlineY, lineX + underlineWidth, underlineY + 1,
                        withAlpha(0xC87A3E, (int)(200 * openProgress)));
            } else if (line.style == LineStyle.QUOTE) {
                int barColor = withAlpha(0xA86A3C, (int)(200 * openProgress));
                guiGraphics.fill(lineX - 6, bodyY, lineX - 4, bodyY + this.lineHeight, barColor);
            } else if (line.style == LineStyle.BULLET) {
                int dotColor = withAlpha(0xC98A5B, (int)(220 * openProgress));
                guiGraphics.fill(lineX - 6, bodyY + 4, lineX - 3, bodyY + 7, dotColor);
            }
            guiGraphics.drawString(this.font, line.text, lineX, bodyY, textColor, false);
            bodyY += this.lineHeight;
        }

        int hintY = buttonY - 14;
        int hintColor = withAlpha(0xC98A5B, (int)(200 * openProgress));
        Component pageLabel = Component.translatable("ashwake_quests_npcs.dialog.page", this.pageIndex + 1, Math.max(1, this.pages.size()));
        int pageWidth = this.font.width(pageLabel);
        int pagePlatePadding = 6;
        int pagePlateWidth = pageWidth + pagePlatePadding * 2;
        int pagePlateLeft = right - PANEL_PADDING - pagePlateWidth;
        int pagePlateTop = hintY - 3;
        int pagePlateHeight = this.font.lineHeight + 6;
        guiGraphics.fill(pagePlateLeft, pagePlateTop, pagePlateLeft + pagePlateWidth, pagePlateTop + pagePlateHeight,
                withAlpha(0x24150E, (int)(190 * openProgress)));
        guiGraphics.drawString(this.font, pageLabel, pagePlateLeft + pagePlatePadding, hintY, hintColor, false);

        int hintMaxWidth = pagePlateLeft - textLeft - 6;
        Component hint = getHintComponent(hintMaxWidth);
        guiGraphics.drawString(this.font, hint, textLeft, hintY, hintColor, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void updateLayout() {
        this.sideButtonWidth = computeSideButtonWidth();
        int sideReserve = getSideReserve();
        int minWidth = hasSideAction() ? PANEL_PADDING * 2 + 150 : MIN_PANEL_WIDTH;
        int availableWidth = Math.max(minWidth, this.width - 32 - sideReserve);
        this.panelWidth = Math.min(MAX_PANEL_WIDTH, availableWidth);
        int innerWidth = this.panelWidth - PANEL_PADDING * 2;
        int textWidth = Math.max(150, innerWidth);
        this.lineHeight = this.font.lineHeight + 1;

        int headerHeight = PANEL_PADDING + 20;
        int footerHeight = PANEL_PADDING + 20 + 16;
        int availableHeight = Math.max(100, this.height - 32);
        this.panelHeight = Math.min(MAX_PANEL_HEIGHT, Math.max(MIN_PANEL_HEIGHT, availableHeight));

        int bodySpace = this.panelHeight - headerHeight - footerHeight;
        int maxLines = Math.max(3, bodySpace / this.lineHeight);

        buildPages(textWidth, maxLines);
        initSparks();
    }

    private void buildPages(int textWidth, int maxLines) {
        this.pages.clear();
        List<PageLine> currentPage = new ArrayList<>();
        int lineCount = 0;

        for (String paragraph : this.paragraphs) {
            if (PAGE_BREAK_MARKER.equals(paragraph)) {
                if (!currentPage.isEmpty()) {
                    this.pages.add(currentPage);
                    currentPage = new ArrayList<>();
                    lineCount = 0;
                }
                continue;
            }
            if (paragraph.isEmpty()) {
                if (lineCount == 0) {
                    continue;
                }
                if (lineCount + 1 > maxLines) {
                    this.pages.add(currentPage);
                    currentPage = new ArrayList<>();
                    lineCount = 0;
                }
                currentPage.add(new PageLine(FormattedCharSequence.EMPTY, LineStyle.BODY));
                lineCount++;
                continue;
            }

            LineStyle style = classifyParagraph(paragraph);
            String sanitized = style == LineStyle.BULLET ? paragraph.substring(1).trim() : paragraph;
            List<FormattedCharSequence> lines = this.font.split(Component.literal(sanitized), textWidth - style.indent);
            for (FormattedCharSequence line : lines) {
                if (lineCount >= maxLines) {
                    this.pages.add(currentPage);
                    currentPage = new ArrayList<>();
                    lineCount = 0;
                }
                currentPage.add(new PageLine(line, style));
                lineCount++;
            }
        }

        if (!currentPage.isEmpty()) {
            this.pages.add(currentPage);
        }
        if (this.pages.isEmpty()) {
            this.pages.add(List.of());
        }
        this.pageIndex = Mth.clamp(this.pageIndex, 0, this.pages.size() - 1);
        updateButtonStates();
    }

    private List<PageLine> getCurrentPageLines() {
        if (this.pages.isEmpty()) {
            return List.of();
        }
        return this.pages.get(Mth.clamp(this.pageIndex, 0, this.pages.size() - 1));
    }

    private Component getHintComponent(int maxWidth) {
        if (maxWidth <= 0) {
            return Component.empty();
        }
        Component full = Component.translatable("ashwake_quests_npcs.dialog.hint");
        if (this.font.width(full) <= maxWidth) {
            return full;
        }
        Component shortHint = Component.translatable("ashwake_quests_npcs.dialog.hint_short");
        if (this.font.width(shortHint) <= maxWidth) {
            return shortHint;
        }
        return Component.empty();
    }

    private LineStyle classifyParagraph(String paragraph) {
        String trimmed = paragraph.trim();
        if (trimmed.startsWith("-")) {
            return LineStyle.BULLET;
        }
        if (trimmed.startsWith("\"") || trimmed.startsWith("\u201C")) {
            return LineStyle.QUOTE;
        }
        if (isHeading(trimmed)) {
            return LineStyle.HEADING;
        }
        return LineStyle.BODY;
    }

    private static boolean isHeading(String line) {
        if (line.length() < 4) {
            return false;
        }
        String letters = line.replaceAll("[^A-Za-z]", "");
        if (letters.length() < 3) {
            return false;
        }
        if (!line.equals(line.toUpperCase())) {
            return false;
        }
        return line.length() <= 52;
    }

    private void initSparks() {
        this.sparks.clear();
        for (int i = 0; i < SPARK_COUNT; i++) {
            Spark spark = new Spark();
            spark.reset(this.random, this.panelWidth, this.panelHeight, true);
            this.sparks.add(spark);
        }
    }

    private boolean nextPage() {
        if (this.pageIndex < this.pages.size() - 1) {
            this.pageIndex++;
            updateButtonStates();
            return true;
        }
        return false;
    }

    private boolean previousPage() {
        if (this.pageIndex > 0) {
            this.pageIndex--;
            updateButtonStates();
            return true;
        }
        return false;
    }

    private void updateButtonStates() {
        if (this.prevButton != null) {
            this.prevButton.active = this.pageIndex > 0;
        }
        boolean onLastPage = this.pageIndex >= this.pages.size() - 1;
        boolean canAcceptNow = canAcceptNow();
        boolean canCompleteNow = canCompleteNow();
        if (this.nextButton != null) {
            this.nextButton.active = !onLastPage;
            this.nextButton.visible = !onLastPage;
        }
        if (this.acceptButton != null) {
            boolean showAction = onLastPage && (canAcceptNow || canCompleteNow);
            this.acceptButton.active = showAction;
            this.acceptButton.visible = showAction;
            if (showAction) {
                this.acceptButton.setMessage(Component.translatable(
                        canAcceptNow ? "ashwake_quests_npcs.dialog.accept" : "ashwake_quests_npcs.dialog.complete"));
            }
        }
        if (this.sideButton != null) {
            this.sideButton.active = canSideActionNow();
            this.sideButton.visible = true;
        }
    }

    private void setButtonAlpha(float alpha) {
        if (this.closeButton != null) {
            this.closeButton.setAlpha(alpha);
        }
        if (this.prevButton != null) {
            this.prevButton.setAlpha(alpha);
        }
        if (this.nextButton != null) {
            this.nextButton.setAlpha(alpha);
        }
        if (this.acceptButton != null) {
            this.acceptButton.setAlpha(alpha);
        }
        if (this.sideButton != null) {
            this.sideButton.setAlpha(alpha);
        }
    }

    private void handleAction() {
        if (canAcceptNow()) {
            if (this.acceptAction != null) {
                this.acceptAction.run();
            }
            triggerAcceptEffects();
        } else if (canCompleteNow()) {
            if (this.completeAction != null) {
                this.completeAction.run();
            }
        }
        updateButtonStates();
    }

    private void handleSideAction() {
        if (this.sideAction != null) {
            this.sideAction.run();
        }
        updateButtonStates();
    }

    private boolean canAcceptNow() {
        return this.acceptAction != null && (this.canAccept == null || this.canAccept.getAsBoolean());
    }

    private boolean canCompleteNow() {
        return this.completeAction != null && (this.canComplete == null || this.canComplete.getAsBoolean());
    }

    private boolean canSideActionNow() {
        return this.sideAction != null && (this.canSideAction == null || this.canSideAction.getAsBoolean());
    }

    private boolean hasSideAction() {
        return this.sideActionLabel != null;
    }

    private int computeSideButtonWidth() {
        if (!hasSideAction()) {
            return 0;
        }
        int targetWidth = this.font.width(this.sideActionLabel) + 16;
        return Mth.clamp(targetWidth, SIDE_BUTTON_MIN_WIDTH, SIDE_BUTTON_MAX_WIDTH);
    }

    private int getSideReserve() {
        if (!hasSideAction()) {
            return 0;
        }
        return this.sideButtonWidth + SIDE_BUTTON_GAP;
    }

    private int getPanelLeft() {
        int groupWidth = this.panelWidth + getSideReserve();
        return (this.width - groupWidth) / 2;
    }

    private int getPanelTop() {
        return (this.height - this.panelHeight) / 2;
    }

    private void triggerAcceptEffects() {
        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) {
            return;
        }

        Component questTitle = this.speaker;
        this.minecraft.player.displayClientMessage(
                Component.translatable("ashwake_quests_npcs.quest.accepted", questTitle),
                true);

        double baseX = this.minecraft.player.getX();
        double baseY = this.minecraft.player.getY() + 0.8;
        double baseZ = this.minecraft.player.getZ();

        for (int i = 0; i < 26; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * 0.9;
            double offsetY = this.random.nextDouble() * 1.1;
            double offsetZ = (this.random.nextDouble() - 0.5) * 0.9;
            double speedX = (this.random.nextDouble() - 0.5) * 0.02;
            double speedY = 0.01 + this.random.nextDouble() * 0.02;
            double speedZ = (this.random.nextDouble() - 0.5) * 0.02;

            this.minecraft.level.addParticle(ParticleTypes.ASH, baseX + offsetX, baseY + offsetY, baseZ + offsetZ,
                    speedX, speedY, speedZ);
            if (i % 4 == 0) {
                this.minecraft.level.addParticle(ParticleTypes.FLAME, baseX + offsetX, baseY + offsetY, baseZ + offsetZ,
                        speedX * 0.6, speedY * 0.9, speedZ * 0.6);
            }
        }
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

        guiGraphics.fill(left + inset, top + inset, left + inset + CORNER_ACCENT, top + inset + 1, accentColor);
        guiGraphics.fill(left + inset, top + inset, left + inset + 1, top + inset + CORNER_ACCENT, accentColor);

        guiGraphics.fill(right - inset - CORNER_ACCENT, top + inset, right - inset, top + inset + 1, accentColor);
        guiGraphics.fill(right - inset - 1, top + inset, right - inset, top + inset + CORNER_ACCENT, accentColor);

        guiGraphics.fill(left + inset, bottom - inset - 1, left + inset + CORNER_ACCENT, bottom - inset, accentColor);
        guiGraphics.fill(left + inset, bottom - inset - CORNER_ACCENT, left + inset + 1, bottom - inset, accentColor);

        guiGraphics.fill(right - inset - CORNER_ACCENT, bottom - inset - 1, right - inset, bottom - inset, accentColor);
        guiGraphics.fill(right - inset - 1, bottom - inset - CORNER_ACCENT, right - inset, bottom - inset, accentColor);
    }

    private void renderTitlePlate(GuiGraphics guiGraphics, int textLeft, int textTop, float openProgress) {
        int maxWidth = this.panelWidth - PANEL_PADDING * 2;
        int titleWidth = Math.min(maxWidth, this.font.width(this.speaker) + 16);
        int plateLeft = textLeft - 8;
        int plateTop = textTop - 5;
        int plateHeight = 13;
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
        int bandTop = bottom - 28;
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
        guiGraphics.fill(left + 12, top + 22, left + this.panelWidth - 12, top + 23, lineColor);
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

    private float getOpenProgress(float partialTick) {
        float raw = (Util.getMillis() - this.openedAt + (long)(partialTick * 50.0f)) / 300.0f;
        raw = Mth.clamp(raw, 0.0f, 1.0f);
        return raw * raw * (3.0f - 2.0f * raw);
    }

    private static int withAlpha(int rgb, int alpha) {
        int clamped = Mth.clamp(alpha, 0, 255);
        return (clamped << 24) | (rgb & 0x00FFFFFF);
    }

    private static final class AshwakeButton extends Button {
        protected AshwakeButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();
            boolean hovered = this.isHoveredOrFocused();
            int glowAlpha = hovered ? 120 : 70;
            int glowColor = (applyAlpha(glowAlpha, this.alpha) << 24) | 0xE08A43;
            guiGraphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, glowColor);

            int topColor = hovered ? 0xFF3B1F13 : 0xFF2A160F;
            int bottomColor = hovered ? 0xFF2A140D : 0xFF1E110B;
            int borderColor = hovered ? 0xFFB76A36 : 0xFF7B3F22;
            guiGraphics.fill(x, y, x + w, y + h, applyAlphaToRgb(topColor, this.alpha));
            guiGraphics.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1,
                    applyAlphaToRgb(topColor, this.alpha),
                    applyAlphaToRgb(bottomColor, this.alpha));
            guiGraphics.fill(x, y, x + w, y + 1, applyAlphaToRgb(borderColor, this.alpha));
            guiGraphics.fill(x, y + h - 1, x + w, y + h, applyAlphaToRgb(borderColor, this.alpha));

            int textColor = hovered ? 0xFFEAC59A : 0xFFD6B28B;
            FontHelper.drawCenteredString(guiGraphics, this.getMessage(), x + w / 2, y + (h - Minecraft.getInstance().font.lineHeight) / 2 + 1,
                    applyAlphaToRgb(textColor, this.alpha));
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

    private static final class FontHelper {
        private static void drawCenteredString(GuiGraphics guiGraphics, Component message, int centerX, int y, int color) {
            Minecraft minecraft = Minecraft.getInstance();
            int width = minecraft.font.width(message);
            guiGraphics.drawString(minecraft.font, message, centerX - width / 2, y, color, false);
        }
    }

    private static final class PageLine {
        private final FormattedCharSequence text;
        private final LineStyle style;

        private PageLine(FormattedCharSequence text, LineStyle style) {
            this.text = text;
            this.style = style;
        }
    }

    private enum LineStyle {
        BODY(0),
        QUOTE(6),
        BULLET(10),
        HEADING(0);

        private final int indent;

        LineStyle(int indent) {
            this.indent = indent;
        }

        private int pickColor(float openProgress) {
            int alpha = (int)(230 * openProgress);
            return switch (this) {
                case HEADING -> withAlpha(0xF0B57A, alpha);
                case QUOTE -> withAlpha(0xE8C7A2, alpha);
                case BULLET -> withAlpha(0xE6D4C1, alpha);
                default -> withAlpha(0xE6D4C1, alpha);
            };
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
