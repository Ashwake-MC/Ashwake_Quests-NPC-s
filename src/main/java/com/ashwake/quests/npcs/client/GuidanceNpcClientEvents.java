package com.ashwake.quests.npcs.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.ashwake.quests.npcs.client.gui.AshwakeDialogScreen;
import com.ashwake.quests.npcs.client.GuidanceQuestState;
import com.ashwake.quests.npcs.client.OrinQuestState;
import com.ashwake.quests.npcs.GuidanceNpcEntity;
import com.ashwake.quests.npcs.OrinHollowmereEntity;
import com.ashwake.quests.npcs.VaelaGrimshotEntity;
import com.ashwake.quests.npcs.AshwakeQuestsNpcsMod;
import com.ashwake.quests.npcs.OrinQuestData;
import com.ashwake.quests.npcs.network.GuidanceNetwork.GuidanceAcceptPayload;
import com.ashwake.quests.npcs.network.GuidanceNetwork.GuidanceCompletePayload;
import com.ashwake.quests.npcs.network.OrinNetwork.OrinAcceptPayload;
import com.ashwake.quests.npcs.network.OrinNetwork.OrinCompletePayload;
import com.ashwake.quests.npcs.network.OrinNetwork.OrinReclaimPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class GuidanceNpcClientEvents {
    private static final Identifier GUIDANCE_DIALOG = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "dialog/guidance_keeper.txt");
    private static final Identifier GUIDANCE_DIALOG_IDLE = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "dialog/guidance_keeper_idle.txt");
    private static final Identifier ORIN_DIALOG_Q1 = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "dialog/orin_hollowmere_q1.txt");
    private static final Identifier ORIN_DIALOG_Q2 = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "dialog/orin_hollowmere_q2.txt");
    private static final Identifier ORIN_DIALOG_Q3 = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "dialog/orin_hollowmere_q3.txt");
    private static final Identifier ORIN_DIALOG_Q4 = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "dialog/orin_hollowmere_q4.txt");
    private static final Identifier ORIN_DIALOG_IDLE = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "dialog/orin_hollowmere_idle.txt");
    private static final Identifier ORIN_DIALOG_FOUND = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "dialog/orin_hollowmere_found.txt");
    private static final Identifier VAELA_DIALOG_IDLE = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "dialog/vaela_grimshot_idle.txt");
    private static final String PAGE_BREAK_MARKER = AshwakeDialogScreen.PAGE_BREAK_MARKER;
    private static final int HUB_MARGIN = 8;
    private static final int HUB_WIDTH = 170;
    private static final int HUB_MIN_WIDTH = 120;

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.screen instanceof AshwakeDialogScreen) {
            return;
        }

        Component speaker = event.getTarget().getDisplayName();
        if (event.getTarget() instanceof GuidanceNpcEntity) {
            openGuidanceDialog(minecraft, speaker);
        } else if (event.getTarget() instanceof OrinHollowmereEntity) {
            if (!GuidanceQuestState.isAccepted()) {
                minecraft.player.displayClientMessage(Component.translatable("ashwake_quests_npcs.orin.locked"), true);
                return;
            }
            openOrinDialog(minecraft, speaker);
        } else if (event.getTarget() instanceof VaelaGrimshotEntity) {
            openVaelaDialog(minecraft, speaker);
        } else {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private void openGuidanceDialog(Minecraft minecraft, Component speaker) {
        Identifier dialogId = GuidanceQuestState.isAccepted() ? GUIDANCE_DIALOG_IDLE : GUIDANCE_DIALOG;
        List<String> paragraphs = loadDialog(minecraft, dialogId);
        Runnable acceptAction = null;
        BooleanSupplier canAccept = null;
        if (!GuidanceQuestState.isAccepted()) {
            acceptAction = () -> {
                GuidanceQuestState.acceptGuidanceQuest();
                ClientPacketDistributor.sendToServer(new GuidanceAcceptPayload());
                queueDialogRefresh(minecraft, () -> openGuidanceDialog(minecraft, speaker));
            };
            canAccept = () -> !GuidanceQuestState.isAccepted();
        }
        minecraft.setScreen(new AshwakeDialogScreen(
                speaker,
                paragraphs,
                acceptAction,
                canAccept,
                null,
                null));
    }

    private void openVaelaDialog(Minecraft minecraft, Component speaker) {
        List<String> paragraphs = loadDialog(minecraft, VAELA_DIALOG_IDLE);
        minecraft.setScreen(new AshwakeDialogScreen(speaker, paragraphs));
    }

    private void openOrinDialog(Minecraft minecraft, Component speaker) {
        int stage = OrinQuestState.getStage();
        boolean needsGuidanceCompletion = !GuidanceQuestState.isCompleted() && stage == OrinQuestData.STAGE_NONE;
        Identifier dialogId;
        if (needsGuidanceCompletion) {
            dialogId = ORIN_DIALOG_FOUND;
        } else if (stage <= OrinQuestData.STAGE_Q1_ACCEPTED) {
            dialogId = ORIN_DIALOG_Q1;
        } else if (stage <= OrinQuestData.STAGE_Q2_ACCEPTED) {
            dialogId = ORIN_DIALOG_Q2;
        } else if (stage <= OrinQuestData.STAGE_Q3_ACCEPTED) {
            dialogId = ORIN_DIALOG_Q3;
        } else if (stage <= OrinQuestData.STAGE_Q4_ACCEPTED) {
            dialogId = ORIN_DIALOG_Q4;
        } else {
            dialogId = ORIN_DIALOG_IDLE;
        }

        List<String> paragraphs = loadDialog(minecraft, dialogId);
        Runnable acceptAction = null;
        BooleanSupplier canAccept = null;
        Runnable completeAction = null;
        BooleanSupplier canComplete = null;

        if (needsGuidanceCompletion) {
            completeAction = () -> {
                GuidanceQuestState.completeGuidanceQuest();
                ClientPacketDistributor.sendToServer(new GuidanceCompletePayload());
                queueDialogRefresh(minecraft, () -> openOrinDialog(minecraft, speaker));
            };
            canComplete = () -> !GuidanceQuestState.isCompleted();
        } else if (stage == OrinQuestData.STAGE_NONE) {
            acceptAction = () -> {
                OrinQuestState.setStage(OrinQuestData.STAGE_Q1_ACCEPTED);
                ClientPacketDistributor.sendToServer(new OrinAcceptPayload((byte)1));
                queueDialogRefresh(minecraft, () -> openOrinDialog(minecraft, speaker));
            };
            canAccept = () -> OrinQuestState.getStage() == OrinQuestData.STAGE_NONE;
        } else if (stage == OrinQuestData.STAGE_Q1_ACCEPTED) {
            completeAction = () -> {
                OrinQuestState.setStage(OrinQuestData.STAGE_Q1_COMPLETED);
                ClientPacketDistributor.sendToServer(new OrinCompletePayload((byte)1));
                queueDialogRefresh(minecraft, () -> openOrinDialog(minecraft, speaker));
            };
            canComplete = () -> OrinQuestState.getStage() == OrinQuestData.STAGE_Q1_ACCEPTED;
        } else if (stage == OrinQuestData.STAGE_Q1_COMPLETED) {
            acceptAction = () -> {
                OrinQuestState.setStage(OrinQuestData.STAGE_Q2_ACCEPTED);
                ClientPacketDistributor.sendToServer(new OrinAcceptPayload((byte)2));
                queueDialogRefresh(minecraft, () -> openOrinDialog(minecraft, speaker));
            };
            canAccept = () -> OrinQuestState.getStage() == OrinQuestData.STAGE_Q1_COMPLETED;
        } else if (stage == OrinQuestData.STAGE_Q2_ACCEPTED) {
            completeAction = () -> {
                if (!OrinQuestState.isVisitedPersonal()) {
                    if (minecraft.player != null) {
                        minecraft.player.displayClientMessage(
                                Component.translatable("ashwake_quests_npcs.orin.quest2_needs_personal"),
                                true);
                    }
                    return;
                }
                OrinQuestState.setStage(OrinQuestData.STAGE_Q2_COMPLETED);
                ClientPacketDistributor.sendToServer(new OrinCompletePayload((byte)2));
                queueDialogRefresh(minecraft, () -> openOrinDialog(minecraft, speaker));
            };
            canComplete = () -> OrinQuestState.getStage() == OrinQuestData.STAGE_Q2_ACCEPTED;
        } else if (stage == OrinQuestData.STAGE_Q2_COMPLETED) {
            acceptAction = () -> {
                OrinQuestState.setStage(OrinQuestData.STAGE_Q3_ACCEPTED);
                ClientPacketDistributor.sendToServer(new OrinAcceptPayload((byte)3));
                queueDialogRefresh(minecraft, () -> openOrinDialog(minecraft, speaker));
            };
            canAccept = () -> OrinQuestState.getStage() == OrinQuestData.STAGE_Q2_COMPLETED;
        } else if (stage == OrinQuestData.STAGE_Q3_ACCEPTED) {
            completeAction = () -> {
                OrinQuestState.setStage(OrinQuestData.STAGE_Q3_COMPLETED);
                ClientPacketDistributor.sendToServer(new OrinCompletePayload((byte)3));
                queueDialogRefresh(minecraft, () -> openOrinDialog(minecraft, speaker));
            };
            canComplete = () -> OrinQuestState.getStage() == OrinQuestData.STAGE_Q3_ACCEPTED;
        } else if (stage == OrinQuestData.STAGE_Q3_COMPLETED) {
            acceptAction = () -> {
                OrinQuestState.setStage(OrinQuestData.STAGE_Q4_ACCEPTED);
                ClientPacketDistributor.sendToServer(new OrinAcceptPayload((byte)4));
                queueDialogRefresh(minecraft, () -> openOrinDialog(minecraft, speaker));
            };
            canAccept = () -> OrinQuestState.getStage() == OrinQuestData.STAGE_Q3_COMPLETED;
        } else if (stage == OrinQuestData.STAGE_Q4_ACCEPTED) {
            completeAction = () -> {
                OrinQuestState.setStage(OrinQuestData.STAGE_Q4_COMPLETED);
                ClientPacketDistributor.sendToServer(new OrinCompletePayload((byte)4));
                queueDialogRefresh(minecraft, () -> openOrinDialog(minecraft, speaker));
            };
            canComplete = () -> OrinQuestState.getStage() == OrinQuestData.STAGE_Q4_ACCEPTED;
        }

        Component shopLabel = null;
        Runnable shopAction = null;
        BooleanSupplier canShop = null;
        if (OrinQuestState.isQuestCompleted()) {
            shopLabel = Component.translatable("ashwake_quests_npcs.orin.shop.reclaim");
            shopAction = () -> ClientPacketDistributor.sendToServer(new OrinReclaimPayload());
            canShop = () -> OrinQuestState.isQuestCompleted() && !hasWaystone(minecraft);
        }

        minecraft.setScreen(new AshwakeDialogScreen(
                speaker,
                paragraphs,
                acceptAction,
                canAccept,
                completeAction,
                canComplete,
                shopLabel,
                shopAction,
                canShop));
    }

    private void queueDialogRefresh(Minecraft minecraft, Runnable refreshAction) {
        if (minecraft != null) {
            minecraft.execute(refreshAction);
        }
    }

    @SubscribeEvent
    public void onContainerRender(ContainerScreenEvent.Render.Foreground event) {
        if (!GuidanceQuestState.isAccepted()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(-screen.getGuiLeft(), -screen.getGuiTop());
        renderQuestHub(guiGraphics, minecraft, screen);
        guiGraphics.pose().popMatrix();
    }

    @SubscribeEvent
    public void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        GuidanceQuestState.setAccepted(false);
        GuidanceQuestState.setCompleted(false);
        OrinQuestState.reset();
    }

    private static List<String> loadDialog(Minecraft minecraft, Identifier dialogId) {
        List<String> paragraphs = new ArrayList<>();
        try (BufferedReader reader = minecraft.getResourceManager().openAsReader(dialogId)) {
            StringBuilder current = new StringBuilder();
            boolean lastBlank = false;
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    if (current.length() > 0) {
                        paragraphs.add(current.toString());
                        current.setLength(0);
                    }
                    if (!lastBlank && !paragraphs.isEmpty()) {
                        paragraphs.add("");
                        lastBlank = true;
                    }
                    continue;
                }
                lastBlank = false;
                if (trimmed.startsWith("-")) {
                    if (current.length() > 0) {
                        paragraphs.add(current.toString());
                        current.setLength(0);
                    }
                    paragraphs.add(trimmed);
                    continue;
                }
                if (trimmed.equals(PAGE_BREAK_MARKER)) {
                    if (current.length() > 0) {
                        paragraphs.add(current.toString());
                        current.setLength(0);
                    }
                    paragraphs.add(PAGE_BREAK_MARKER);
                    continue;
                }
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(trimmed);
            }
            if (current.length() > 0) {
                paragraphs.add(current.toString());
            }
        } catch (IOException e) {
            paragraphs.clear();
            paragraphs.add(Component.translatable("ashwake_quests_npcs.dialog.guidance.body").getString());
        }

        if (paragraphs.isEmpty()) {
            paragraphs.add(Component.translatable("ashwake_quests_npcs.dialog.guidance.body").getString());
        }
        return paragraphs;
    }

    private static void renderQuestHub(GuiGraphics guiGraphics, Minecraft minecraft, AbstractContainerScreen<?> screen) {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        boolean compact = screenWidth < 360 || screenHeight < 240;
        int width = Math.min(HUB_WIDTH, screenWidth - HUB_MARGIN * 2);
        if (compact) {
            int compactWidth = Math.max(HUB_MIN_WIDTH, (int)(screenWidth * 0.45f));
            width = Math.min(width, compactWidth);
        }
        if (width < HUB_MIN_WIDTH) {
            return;
        }

        int padding = compact ? 4 : 6;
        int innerWidth = width - padding * 2;
        int lineHeight = minecraft.font.lineHeight + (compact ? 0 : 1);

        Component title = Component.translatable("ashwake_quests_npcs.quest_hub.title");
        Component questTitle = Component.translatable("ashwake_quests_npcs.quest_hub.quest_title");
        Component status;
        List<Component> objectives = new ArrayList<>();

        int stage = OrinQuestState.getStage();
        if (stage == OrinQuestData.STAGE_NONE && !GuidanceQuestState.isCompleted()) {
            status = Component.translatable("ashwake_quests_npcs.quest_hub.status_active");
            objectives.add(Component.translatable("ashwake_quests_npcs.quest_hub.obj_find_orin"));
        } else if (stage == OrinQuestData.STAGE_NONE) {
            status = Component.translatable("ashwake_quests_npcs.quest_hub.status_q1_offer");
            objectives.add(Component.translatable("ashwake_quests_npcs.quest_hub.obj_q1_accept"));
        } else if (stage == OrinQuestData.STAGE_Q1_ACCEPTED) {
            status = Component.translatable("ashwake_quests_npcs.quest_hub.status_q1");
            objectives.add(Component.translatable("ashwake_quests_npcs.quest_hub.obj_q1_complete"));
        } else if (stage == OrinQuestData.STAGE_Q1_COMPLETED) {
            status = Component.translatable("ashwake_quests_npcs.quest_hub.status_q2_offer");
            objectives.add(Component.translatable("ashwake_quests_npcs.quest_hub.obj_q2_accept"));
        } else if (stage == OrinQuestData.STAGE_Q2_ACCEPTED) {
            status = Component.translatable("ashwake_quests_npcs.quest_hub.status_q2");
            if (OrinQuestState.isVisitedPersonal()) {
                objectives.add(Component.translatable("ashwake_quests_npcs.quest_hub.obj_return_orin"));
            } else {
                objectives.add(Component.translatable("ashwake_quests_npcs.quest_hub.obj_waystone"));
            }
        } else if (stage == OrinQuestData.STAGE_Q2_COMPLETED) {
            status = Component.translatable("ashwake_quests_npcs.quest_hub.status_q3_offer");
            objectives.add(Component.translatable("ashwake_quests_npcs.quest_hub.obj_q3_accept"));
        } else if (stage == OrinQuestData.STAGE_Q3_ACCEPTED) {
            status = Component.translatable("ashwake_quests_npcs.quest_hub.status_q3");
            objectives.add(Component.translatable("ashwake_quests_npcs.quest_hub.obj_q3_complete"));
        } else if (stage == OrinQuestData.STAGE_Q3_COMPLETED) {
            status = Component.translatable("ashwake_quests_npcs.quest_hub.status_q4_offer");
            objectives.add(Component.translatable("ashwake_quests_npcs.quest_hub.obj_q4_accept"));
        } else if (stage == OrinQuestData.STAGE_Q4_ACCEPTED) {
            status = Component.translatable("ashwake_quests_npcs.quest_hub.status_q4");
            objectives.add(Component.translatable("ashwake_quests_npcs.quest_hub.obj_q4_seek"));
        } else {
            status = Component.translatable("ashwake_quests_npcs.quest_hub.status_complete");
            objectives.add(Component.translatable("ashwake_quests_npcs.quest_hub.obj_complete"));
        }

        List<FormattedCharSequence> titleLines = minecraft.font.split(title, innerWidth);
        List<FormattedCharSequence> questLines = minecraft.font.split(questTitle, innerWidth);
        List<FormattedCharSequence> statusLines = minecraft.font.split(status, innerWidth);
        List<List<FormattedCharSequence>> wrapped = new ArrayList<>();
        int linesCount = 0;
        for (Component objective : objectives) {
            List<FormattedCharSequence> lines = minecraft.font.split(objective, innerWidth - 10);
            wrapped.add(lines);
            linesCount += lines.size();
        }

        int separatorSpacing = compact ? 2 : 4;
        int objectiveSpacing = compact ? 1 : 2;
        int statusSpacing = compact ? 4 : 6;
        int maxHeight = screenHeight - HUB_MARGIN * 2;
        int height = padding;
        height += titleLines.size() * lineHeight;
        height += questLines.size() * lineHeight;
        height += separatorSpacing;
        height += statusLines.size() * lineHeight;
        height += statusSpacing;
        height += linesCount * lineHeight;
        height += (objectives.size() - 1) * objectiveSpacing;
        height += padding;
        if (height > maxHeight && !questLines.isEmpty()) {
            questLines = List.of();
            height = padding;
            height += titleLines.size() * lineHeight;
            height += separatorSpacing;
            height += statusLines.size() * lineHeight;
            height += statusSpacing;
            height += linesCount * lineHeight;
            height += (objectives.size() - 1) * objectiveSpacing;
            height += padding;
        }

        int topPos = screen.getGuiTop();

        int left = HUB_MARGIN;
        if (left + width > screenWidth - HUB_MARGIN) {
            left = Math.max(HUB_MARGIN, screenWidth - width - HUB_MARGIN);
        }

        int top = Math.max(HUB_MARGIN, topPos);
        if (top + height > screenHeight - HUB_MARGIN) {
            height = screenHeight - HUB_MARGIN * 2;
            top = Math.max(HUB_MARGIN, screenHeight - height - HUB_MARGIN);
        }
        int right = left + width;
        int bottom = top + height;

        int borderColor = withAlpha(0x8D4C25, 210);
        int fillTop = withAlpha(0x24150E, 220);
        int fillBottom = withAlpha(0x1A0F0A, 220);
        guiGraphics.fill(left, top, right, bottom, borderColor);
        guiGraphics.fillGradient(left + 1, top + 1, right - 1, bottom - 1, fillTop, fillBottom);

        int textX = left + padding;
        int cursorY = top + padding;
        int titleColor = withAlpha(0xF0B57A, 255);
        int questColor = withAlpha(0xE6D4C1, 230);
        int statusColor = withAlpha(0xD7C08D, 255);

        for (FormattedCharSequence line : titleLines) {
            guiGraphics.drawString(minecraft.font, line, textX, cursorY, titleColor, false);
            cursorY += lineHeight;
        }
        for (FormattedCharSequence line : questLines) {
            guiGraphics.drawString(minecraft.font, line, textX, cursorY, questColor, false);
            cursorY += lineHeight;
        }
        guiGraphics.fill(textX, cursorY, right - padding, cursorY + 1, withAlpha(0x6E3C1F, 200));
        cursorY += separatorSpacing;
        for (FormattedCharSequence line : statusLines) {
            guiGraphics.drawString(minecraft.font, line, textX, cursorY, statusColor, false);
            cursorY += lineHeight;
        }
        cursorY += statusSpacing;

        int bulletColor = withAlpha(0xC98A5B, 220);
        int textColor = withAlpha(0xE6D4C1, 230);
        for (List<FormattedCharSequence> lines : wrapped) {
            boolean first = true;
            for (FormattedCharSequence line : lines) {
                if (cursorY + lineHeight > bottom - padding) {
                    return;
                }
                if (first) {
                    guiGraphics.fill(textX, cursorY + 4, textX + 3, cursorY + 7, bulletColor);
                }
                guiGraphics.drawString(minecraft.font, line, textX + 8, cursorY, textColor, false);
                cursorY += lineHeight;
                first = false;
            }
            cursorY += objectiveSpacing;
        }
    }

    private static int withAlpha(int rgb, int alpha) {
        int clamped = Mth.clamp(alpha, 0, 255);
        return (clamped << 24) | (rgb & 0x00FFFFFF);
    }

    private static boolean hasWaystone(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        return minecraft.player.getInventory()
                .contains(AshwakeQuestsNpcsMod.ASHWAKE_WAYSTONE.get().getDefaultInstance());
    }
}
