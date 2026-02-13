package com.ashwake.quests.npcs.client;

import com.ashwake.quests.npcs.GuidanceNpcEntity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public class GuidanceNpcRenderer extends HumanoidMobRenderer<GuidanceNpcEntity, AvatarRenderState, PlayerModel> {
    private static final Identifier GUIDANCE_NPC_ID = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "guidance_npc");
    private static final Identifier GUIDANCE_NPC_TEXTURE = Identifier.fromNamespaceAndPath(
            "ashwake_quests_npcs",
            "textures/entity/guidance_npc.png");
    private static final PlayerSkin GUIDANCE_NPC_SKIN = PlayerSkin.insecure(
            new ClientAsset.ResourceTexture(GUIDANCE_NPC_ID, GUIDANCE_NPC_TEXTURE),
            new ClientAsset.ResourceTexture(GUIDANCE_NPC_ID, GUIDANCE_NPC_TEXTURE),
            new ClientAsset.ResourceTexture(GUIDANCE_NPC_ID, GUIDANCE_NPC_TEXTURE),
            PlayerModelType.WIDE);

    public GuidanceNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(GuidanceNpcEntity entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = GUIDANCE_NPC_SKIN;
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
        return GUIDANCE_NPC_TEXTURE;
    }
}
