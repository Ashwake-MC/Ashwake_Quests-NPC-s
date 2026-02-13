package com.ashwake.quests.npcs.client;

import com.ashwake.quests.npcs.OrinQuestData;
import com.ashwake.quests.npcs.VaelaGrimshotEntity;
import com.ashwake.quests.npcs.client.OrinQuestState;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public class VaelaGrimshotRenderer extends HumanoidMobRenderer<VaelaGrimshotEntity, AvatarRenderState, PlayerModel> {
    private static final Identifier VAELA_ID = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "vaela_grimshot");
    private static final Identifier VAELA_TEXTURE = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "textures/entity/vaela_grimshot.png");
    private static final PlayerSkin VAELA_SKIN = PlayerSkin.insecure(
            new ClientAsset.ResourceTexture(VAELA_ID, VAELA_TEXTURE),
            new ClientAsset.ResourceTexture(VAELA_ID, VAELA_TEXTURE),
            new ClientAsset.ResourceTexture(VAELA_ID, VAELA_TEXTURE),
            PlayerModelType.WIDE);

    public VaelaGrimshotRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(VaelaGrimshotEntity entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = VAELA_SKIN;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showCape = false;
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return VAELA_TEXTURE;
    }

    @Override
    public boolean shouldRender(VaelaGrimshotEntity entity, Frustum camera, double camX, double camY, double camZ) {
        if (OrinQuestState.getStage() < OrinQuestData.STAGE_Q4_ACCEPTED) {
            return false;
        }
        return super.shouldRender(entity, camera, camX, camY, camZ);
    }
}
