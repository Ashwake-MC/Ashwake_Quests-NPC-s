package com.ashwake.quests.npcs;

import com.ashwake.quests.npcs.client.GuidanceClientNetwork;
import com.ashwake.quests.npcs.client.GuidanceNpcClientEvents;
import com.ashwake.quests.npcs.client.GuidanceNpcRenderer;
import com.ashwake.quests.npcs.client.OrinClientNetwork;
import com.ashwake.quests.npcs.client.OrinHollowmereRenderer;
import com.ashwake.quests.npcs.client.VaelaGrimshotRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = AshwakeQuestsNpcsMod.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = AshwakeQuestsNpcsMod.MODID, value = Dist.CLIENT)
public class AshwakeQuestsNpcsClient {
    public AshwakeQuestsNpcsClient(ModContainer container, IEventBus modEventBus) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(GuidanceClientNetwork::registerClientHandlers);
        modEventBus.addListener(OrinClientNetwork::registerClientHandlers);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        event.enqueueWork(() -> {
            EntityRenderers.register(AshwakeQuestsNpcsMod.GUIDANCE_NPC.get(), GuidanceNpcRenderer::new);
            EntityRenderers.register(AshwakeQuestsNpcsMod.ORIN_HOLLOWMERE.get(), OrinHollowmereRenderer::new);
            EntityRenderers.register(AshwakeQuestsNpcsMod.VAELA_GRIMSHOT.get(), VaelaGrimshotRenderer::new);
            NeoForge.EVENT_BUS.register(new GuidanceNpcClientEvents());
        });
        AshwakeQuestsNpcsMod.LOGGER.info("HELLO FROM CLIENT SETUP");
        AshwakeQuestsNpcsMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
