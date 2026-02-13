package com.ashwake.quests.npcs.client;

import com.ashwake.quests.npcs.OrinHollowmereEntity;
import com.ashwake.quests.npcs.client.GuidanceQuestState;

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

public class OrinHollowmereRenderer extends HumanoidMobRenderer<OrinHollowmereEntity, AvatarRenderState, PlayerModel> {
    private static final Identifier ORIN_NPC_ID = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "orin_hollowmere");
    private static final Identifier ORIN_NPC_TEXTURE = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "textures/entity/orin_hollowmere.png");
    private static final PlayerSkin ORIN_NPC_SKIN = PlayerSkin.insecure(
            new ClientAsset.ResourceTexture(ORIN_NPC_ID, ORIN_NPC_TEXTURE),
            new ClientAsset.ResourceTexture(ORIN_NPC_ID, ORIN_NPC_TEXTURE),
            new ClientAsset.ResourceTexture(ORIN_NPC_ID, ORIN_NPC_TEXTURE),
            PlayerModelType.WIDE);

    public OrinHollowmereRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(OrinHollowmereEntity entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = ORIN_NPC_SKIN;
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
        return ORIN_NPC_TEXTURE;
    }

    @Override
    public boolean shouldRender(OrinHollowmereEntity entity, Frustum camera, double camX, double camY, double camZ) {
        if (!GuidanceQuestState.isAccepted()) {
            return false;
        }
        return super.shouldRender(entity, camera, camX, camY, camZ);
    }
}
