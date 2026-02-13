package com.ashwake.quests.npcs;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.server.level.ServerPlayer;
import com.ashwake.quests.npcs.network.GuidanceNetwork;
import com.ashwake.quests.npcs.network.WaystoneNetwork;
import com.ashwake.quests.npcs.network.OrinNetwork;
import com.ashwake.quests.npcs.item.AshwakeWaystoneItem;
import com.ashwake.quests.npcs.OrinQuestData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import com.ashwake.quests.npcs.item.AshwakeWaystoneItem;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(AshwakeQuestsNpcsMod.MODID)
public class AshwakeQuestsNpcsMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "ashwake_quests_npcs";
    private static final String GUIDANCE_NPC_TAG = "ashwake_guidance_npc";
    private static final String FIRST_JOIN_SURFACE_KEY = MODID + ":first_join_surface";
    public static final Vec3 HUB_POS = new Vec3(0.464, 60.0, 28.281);
    private static final ResourceKey<EntityType<?>> GUIDANCE_NPC_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "guidance_npc"));
    private static final String ORIN_HOLLOWMERE_TAG = "ashwake_orin_hollowmere";
    private static final Vec3 ORIN_HOLLOWMERE_POS = new Vec3(9.548, 61.0, 35.717);
    private static final String VAELA_GRIMSHOT_TAG = "ashwake_vaela_grimshot";
    private static final Vec3 VAELA_GRIMSHOT_POS = new Vec3(41.367, 59.0, 28.089);
    public static final Vec3 PERSONAL_WORLD_POS = new Vec3(2200.0, 72.0, 2200.0);
    public static final double PERSONAL_WORLD_BORDER_BASE = 2048.0;
    public static final double PERSONAL_WORLD_BORDER_STEP = 512.0;
    public static final int PERSONAL_SPAWN_SEARCH_RADIUS = 256;
    public static final ResourceKey<Level> PERSONAL_WORLD_DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(MODID, "personal_world"));
    private static final ResourceKey<EntityType<?>> ORIN_HOLLOWMERE_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "orin_hollowmere"));
    private static final ResourceKey<EntityType<?>> VAELA_GRIMSHOT_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MODID, "vaela_grimshot"));
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "ashwake_quests_npcs" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "ashwake_quests_npcs" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold Entity Types which will all be registered under the "ashwake_quests_npcs" namespace
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "ashwake_quests_npcs" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "ashwake_quests_npcs:ashwake_block", combining the namespace and path
    public static final DeferredBlock<Block> ASHWAKE_BLOCK = BLOCKS.registerSimpleBlock("ashwake_block", p -> p.mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "ashwake_quests_npcs:ashwake_block", combining the namespace and path
    public static final DeferredItem<BlockItem> ASHWAKE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("ashwake_block", ASHWAKE_BLOCK);
    public static final DeferredItem<AshwakeWaystoneItem> ASHWAKE_WAYSTONE = ITEMS.registerItem(
            "ashwake_waystone",
            props -> new AshwakeWaystoneItem(props.stacksTo(1)));

    public static final DeferredHolder<EntityType<?>, EntityType<GuidanceNpcEntity>> GUIDANCE_NPC = ENTITY_TYPES.register("guidance_npc",
            () -> EntityType.Builder.of(GuidanceNpcEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(8)
                    .build(GUIDANCE_NPC_KEY));
    public static final DeferredHolder<EntityType<?>, EntityType<OrinHollowmereEntity>> ORIN_HOLLOWMERE = ENTITY_TYPES.register("orin_hollowmere",
            () -> EntityType.Builder.of(OrinHollowmereEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(8)
                    .build(ORIN_HOLLOWMERE_KEY));
    public static final DeferredHolder<EntityType<?>, EntityType<VaelaGrimshotEntity>> VAELA_GRIMSHOT = ENTITY_TYPES.register("vaela_grimshot",
            () -> EntityType.Builder.of(VaelaGrimshotEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(8)
                    .build(VAELA_GRIMSHOT_KEY));

    // Creates a creative tab with the id "ashwake_quests_npcs:ashwake_tab", placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ASHWAKE_TAB = CREATIVE_MODE_TABS.register("ashwake_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ashwake_quests_npcs")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> Items.BOOK.getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ASHWAKE_BLOCK_ITEM.get());
                output.accept(ASHWAKE_WAYSTONE.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public AshwakeQuestsNpcsMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(GuidanceNetwork::register);
        modEventBus.addListener(WaystoneNetwork::register);
        modEventBus.addListener(OrinNetwork::register);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so entity types get registered
        ENTITY_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (AshwakeQuestsNpcsMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerEntityAttributes);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the Ashwake block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Items are exposed via the Ashwake tab instead of vanilla tabs.
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(GUIDANCE_NPC.get(), GuidanceNpcEntity.createAttributes().build());
        event.put(ORIN_HOLLOWMERE.get(), OrinHollowmereEntity.createAttributes().build());
        event.put(VAELA_GRIMSHOT.get(), VaelaGrimshotEntity.createAttributes().build());
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        AABB worldSearch = new AABB(-30000000.0, -2048.0, -30000000.0, 30000000.0, 2048.0, 30000000.0);
        List<GuidanceNpcEntity> existingNpcs = level.getEntitiesOfClass(GuidanceNpcEntity.class, worldSearch,
                npc -> npc.getTags().contains(GUIDANCE_NPC_TAG));
        for (GuidanceNpcEntity existingNpc : existingNpcs) {
            existingNpc.discard();
        }
        List<OrinHollowmereEntity> existingOrin = level.getEntitiesOfClass(OrinHollowmereEntity.class, worldSearch,
                npcEntity -> npcEntity.getTags().contains(ORIN_HOLLOWMERE_TAG));
        for (OrinHollowmereEntity existingNpc : existingOrin) {
            existingNpc.discard();
        }
        List<VaelaGrimshotEntity> existingVaela = level.getEntitiesOfClass(VaelaGrimshotEntity.class, worldSearch,
                npcEntity -> npcEntity.getTags().contains(VAELA_GRIMSHOT_TAG));
        for (VaelaGrimshotEntity existingNpc : existingVaela) {
            existingNpc.discard();
        }

        GuidanceNpcEntity npc = GUIDANCE_NPC.get().create(level, EntitySpawnReason.EVENT);
        if (npc == null) {
            LOGGER.warn("Failed to create Guidance NPC entity.");
            return;
        }

        npc.setPos(HUB_POS.x, HUB_POS.y, HUB_POS.z);
        npc.setYRot(0.0f);
        npc.setXRot(0.0f);
        npc.setCustomName(Component.literal("Guidance NPC"));
        npc.setCustomNameVisible(true);
        npc.setPersistenceRequired();
        npc.setNoGravity(true);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.addTag(GUIDANCE_NPC_TAG);

        level.addFreshEntity(npc);

        ServerLevel personalWorld = event.getServer().getLevel(PERSONAL_WORLD_DIMENSION);
        configurePersonalWorldBorder(personalWorld, OrinQuestData.STAGE_NONE, true);
    }

    public static void configurePersonalWorldBorder(ServerLevel level, int stage, boolean allowShrink) {
        if (level == null || !level.dimension().equals(PERSONAL_WORLD_DIMENSION)) {
            return;
        }
        WorldBorder border = level.getWorldBorder();
        border.setCenter(PERSONAL_WORLD_POS.x, PERSONAL_WORLD_POS.z);
        int effectiveStage = Math.min(stage, OrinQuestData.STAGE_Q2_COMPLETED);
        double targetSize = PERSONAL_WORLD_BORDER_BASE + (Math.max(0, effectiveStage) * PERSONAL_WORLD_BORDER_STEP);
        if (allowShrink || border.getSize() < targetSize) {
            border.setSize(targetSize);
        }
    }

    public static BlockPos findSurfaceSpawn(ServerLevel level, BlockPos anchor) {
        int baseChunkX = SectionPos.blockToSectionCoord(anchor.getX());
        int baseChunkZ = SectionPos.blockToSectionCoord(anchor.getZ());
        int maxChunkRadius = Math.max(1, PERSONAL_SPAWN_SEARCH_RADIUS / 16);
        for (int radius = 0; radius <= maxChunkRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int chunkX = baseChunkX + dx;
                int chunkZNorth = baseChunkZ - radius;
                BlockPos candidate = findSpawnInChunk(level, chunkX, chunkZNorth);
                if (candidate != null) {
                    return candidate;
                }
                if (radius > 0) {
                    int chunkZSouth = baseChunkZ + radius;
                    candidate = findSpawnInChunk(level, chunkX, chunkZSouth);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
            for (int dz = -radius + 1; dz <= radius - 1; dz++) {
                int chunkZ = baseChunkZ + dz;
                int chunkXWest = baseChunkX - radius;
                BlockPos candidate = findSpawnInChunk(level, chunkXWest, chunkZ);
                if (candidate != null) {
                    return candidate;
                }
                if (radius > 0) {
                    int chunkXEast = baseChunkX + radius;
                    candidate = findSpawnInChunk(level, chunkXEast, chunkZ);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }
        return fallbackSurfaceSpawn(level, anchor);
    }

    private static BlockPos findSpawnInChunk(ServerLevel level, int chunkX, int chunkZ) {
        level.getChunk(chunkX, chunkZ);
        BlockPos candidate = PlayerSpawnFinder.getSpawnPosInChunk(level, new ChunkPos(chunkX, chunkZ));
        if (candidate != null && isSpawnSafe(level, candidate)) {
            return candidate;
        }
        return null;
    }

    private static BlockPos fallbackSurfaceSpawn(ServerLevel level, BlockPos anchor) {
        BlockPos candidate = surfaceCandidate(level, anchor.getX(), anchor.getZ());
        if (isSpawnSafe(level, candidate)) {
            return candidate;
        }
        BlockPos columnSafe = findSafeInColumn(level, anchor.getX(), anchor.getZ());
        return columnSafe != null ? columnSafe : candidate;
    }

    private static BlockPos surfaceCandidate(ServerLevel level, int x, int z) {
        level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    private static BlockPos findSafeInColumn(ServerLevel level, int x, int z) {
        level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        for (int y = level.getMaxY() - 1; y > level.getMinY(); y--) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (isSpawnSafe(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isSpawnSafe(ServerLevel level, BlockPos pos) {
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        BlockPos ground = pos.below();
        BlockState groundState = level.getBlockState(ground);
        if (groundState.isAir() || !groundState.getFluidState().isEmpty()) {
            return false;
        }
        if (!level.noCollision(null, EntityType.PLAYER.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(pos)), true)) {
            return false;
        }
        return true;
    }

    public static void ensureOrinPresent(ServerLevel level) {
        if (level == null) {
            return;
        }
        AABB worldSearch = new AABB(-30000000.0, -2048.0, -30000000.0, 30000000.0, 2048.0, 30000000.0);
        List<OrinHollowmereEntity> existingOrin = level.getEntitiesOfClass(OrinHollowmereEntity.class, worldSearch,
                npcEntity -> npcEntity.getTags().contains(ORIN_HOLLOWMERE_TAG));
        if (!existingOrin.isEmpty()) {
            return;
        }

        OrinHollowmereEntity orin = ORIN_HOLLOWMERE.get().create(level, EntitySpawnReason.EVENT);
        if (orin == null) {
            LOGGER.warn("Failed to create Orin Hollowmere entity.");
            return;
        }

        orin.setPos(ORIN_HOLLOWMERE_POS.x, ORIN_HOLLOWMERE_POS.y, ORIN_HOLLOWMERE_POS.z);
        orin.setYRot(180.0f);
        orin.setXRot(0.0f);
        orin.setCustomName(Component.literal("Orin Hollowmere"));
        orin.setCustomNameVisible(true);
        orin.setPersistenceRequired();
        orin.setNoGravity(true);
        orin.setInvulnerable(true);
        orin.setSilent(true);
        orin.addTag(ORIN_HOLLOWMERE_TAG);

        level.addFreshEntity(orin);
    }

    public static void ensureVaelaPresent(ServerLevel level) {
        if (level == null) {
            return;
        }
        AABB worldSearch = new AABB(-30000000.0, -2048.0, -30000000.0, 30000000.0, 2048.0, 30000000.0);
        List<VaelaGrimshotEntity> existingVaela = level.getEntitiesOfClass(VaelaGrimshotEntity.class, worldSearch,
                npcEntity -> npcEntity.getTags().contains(VAELA_GRIMSHOT_TAG));
        if (!existingVaela.isEmpty()) {
            return;
        }

        VaelaGrimshotEntity vaela = VAELA_GRIMSHOT.get().create(level, EntitySpawnReason.EVENT);
        if (vaela == null) {
            LOGGER.warn("Failed to create Vaela Grimshot entity.");
            return;
        }

        vaela.setPos(VAELA_GRIMSHOT_POS.x, VAELA_GRIMSHOT_POS.y, VAELA_GRIMSHOT_POS.z);
        vaela.setYRot(90.0f);
        vaela.setXRot(0.0f);
        vaela.setCustomName(Component.literal("Vaela Grimshot"));
        vaela.setCustomNameVisible(true);
        vaela.setPersistenceRequired();
        vaela.setNoGravity(true);
        vaela.setInvulnerable(true);
        vaela.setSilent(true);
        vaela.addTag(VAELA_GRIMSHOT_TAG);

        level.addFreshEntity(vaela);
    }

    @SubscribeEvent
    public void onOrinInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getTarget() instanceof OrinHollowmereEntity)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!GuidanceQuestData.isAccepted(player)) {
            player.sendSystemMessage(Component.translatable("ashwake_quests_npcs.orin.locked"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (OrinQuestData.getStage(player) >= OrinQuestData.STAGE_Q2_ACCEPTED
                && !OrinQuestData.isWaystoneGiven(player)) {
            ItemStack waystone = new ItemStack(ASHWAKE_WAYSTONE.get());
            if (!player.getInventory().add(waystone)) {
                player.drop(waystone, false);
            }
            OrinQuestData.setWaystoneGiven(player, true);
            player.sendSystemMessage(Component.translatable("ashwake_quests_npcs.waystone.received"));
        }
        OrinNetwork.sendSync(player);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ensureFirstJoinSurfaceSpawn(player);
        GuidanceNetwork.sendSync(player, GuidanceQuestData.isAccepted(player), GuidanceQuestData.isCompleted(player));
        OrinNetwork.sendSync(player);
        if (GuidanceQuestData.isAccepted(player)) {
            ServerLevel overworld = player.level().getServer().getLevel(Level.OVERWORLD);
            ensureOrinPresent(overworld);
            if (OrinQuestData.getStage(player) >= OrinQuestData.STAGE_Q4_ACCEPTED) {
                ensureVaelaPresent(overworld);
            }
        }
    }

    private static void ensureFirstJoinSurfaceSpawn(ServerPlayer player) {
        if (player.getPersistentData().getBooleanOr(FIRST_JOIN_SURFACE_KEY, false)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        BlockPos anchor = player.blockPosition();
        BlockPos safe = findSurfaceSpawn(level, anchor);
        if (!isSpawnSafe(level, safe)) {
            return;
        }
        if (!safe.equals(anchor)) {
            player.teleportTo(
                    level,
                    safe.getX() + 0.5,
                    safe.getY(),
                    safe.getZ() + 0.5,
                    Set.of(),
                    player.getYRot(),
                    player.getXRot(),
                    true);
        }
        player.getPersistentData().putBoolean(FIRST_JOIN_SURFACE_KEY, true);
    }
}
