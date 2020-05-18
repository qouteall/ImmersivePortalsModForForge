package com.qouteall.hiding_in_the_bushes;

import com.qouteall.hiding_in_the_bushes.alternate_dimension.AlternateDimension;
import com.qouteall.hiding_in_the_bushes.alternate_dimension.AlternateDimensionEntry;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ModMainClient;
import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import com.qouteall.immersive_portals.portal.BreakableMirror;
import com.qouteall.immersive_portals.portal.CustomizablePortalGeneration;
import com.qouteall.immersive_portals.portal.EndPortalEntity;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import com.qouteall.immersive_portals.portal.nether_portal.GeneralBreakablePortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalEntity;
import com.qouteall.immersive_portals.render.LoadingIndicatorRenderer;
import com.qouteall.immersive_portals.render.PortalEntityRenderer;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ModDimension;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.RegisterDimensionsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("immersive_portals")
public class ModMainForge {
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static boolean isServerMixinApplied = false;
    
    public ModMainForge() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        ConfigClient.init();
        ConfigServer.init();
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void initPortalRenderers() {
        EntityRendererManager manager = Minecraft.getInstance().getRenderManager();
        
        Arrays.stream(new EntityType<?>[]{
            Portal.entityType,
            NetherPortalEntity.entityType,
            EndPortalEntity.entityType,
            Mirror.entityType,
            BreakableMirror.entityType,
            GlobalTrackedPortal.entityType,
            BorderPortal.entityType,
            VerticalConnectingPortal.entityType,
            GeneralBreakablePortal.entityType
        }).peek(
            Validate::notNull
        ).forEach(
            entityType -> manager.register(
                entityType,
                (EntityRenderer) new PortalEntityRenderer(manager)
            )
        );
        
        manager.register(
            LoadingIndicatorEntity.entityType,
            new LoadingIndicatorRenderer(manager)
        );
    }
    
    private void setup(final FMLCommonSetupEvent event) {
        ModMain.init();
    }
    
    private void doClientStuff(final FMLClientSetupEvent event) {
        ModMainClient.init();
        
        Minecraft.getInstance().execute(() -> {
            if (ConfigClient.instance.compatibilityRenderMode.get()) {
                Global.renderMode = Global.RenderMode.compatibility;
                Helper.log("Initially Switched to Compatibility Render Mode");
            }
            Global.doCheckGlError = ConfigClient.instance.doCheckGlError.get();
            Helper.log("Do Check Gl Error: " + Global.doCheckGlError);
            Global.renderYourselfInPortal = ConfigClient.instance.renderYourselfInPortal.get();
            Global.maxPortalLayer = ConfigClient.instance.maxPortalLayer.get();
            Global.correctCrossPortalEntityRendering =
                ConfigClient.instance.correctCrossPortalEntityRendering.get();
            Global.edgelessSky = ConfigClient.instance.edgelessSky.get();
            RenderDimensionRedirect.updateIdMap(
                ConfigClient.listToMap(
                    Arrays.asList(
                        ConfigClient.instance.renderDimensionRedirect.get().split("\n")
                    )
                )
            );
        });
        
        initPortalRenderers();
    }
    
    private void enqueueIMC(final InterModEnqueueEvent event) {
    
    }
    
    private void processIMC(final InterModProcessEvent event) {
    
    }
    
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        Global.netherPortalFindingRadius = ConfigServer.instance.portalSearchingRange.get();
        Global.longerReachInCreative = ConfigServer.instance.longReachInCreative.get();
        Global.activeLoading = ConfigServer.instance.activeLoadRemoteChunks.get();
        Global.teleportationDebugEnabled = ConfigServer.instance.teleportationDebug.get();
        Global.loadFewerChunks = ConfigServer.instance.loadFewerChunks.get();
        Global.multiThreadedNetherPortalSearching = ConfigServer.instance.multiThreadedNetherPortalSearching.get();
        Global.looseMovementCheck = ConfigServer.instance.looseMovementCheck.get();
        
        CustomizablePortalGeneration.onConfigChanged(
            Arrays.asList(
                ConfigServer.instance.customPortalFrame.get().split("\n")
            )
        );
    }
    
    @SubscribeEvent
    public void onModelRegistry(ModelRegistryEvent event) {
    
    }
    
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.getGui() instanceof MainMenuScreen) {
            if (!isMixinInClasspath()) {
                Minecraft.getInstance().displayGuiScreen(new MissingMixinScreen());
                event.setCanceled(true);
            }
        }
    }
    
    public static void checkMixinState() {
        if (!isServerMixinApplied) {
            String message =
                "Mixin is NOT loaded. Install MixinBootstrap." +
                    " https://www.curseforge.com/minecraft/mc-mods/immersive-portals-for-forge";
            
            try {
                Class.forName("org.spongepowered.asm.launch.Phases");
                Helper.err("What? Mixin is in classpath???");
            }
            catch (ClassNotFoundException e) {
                Helper.err("Mixin is not in classpath");
            }
            
            Helper.err(message);
            throw new IllegalStateException(message);
        }
    }
    
    public static boolean isMixinInClasspath() {
        try {
            Class.forName("org.spongepowered.asm.launch.Phases");
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        checkMixinState();
    }
    
    @SubscribeEvent
    public void onRegisterDimensionsEvent(RegisterDimensionsEvent event) {
        ResourceLocation resourceLocation = new ResourceLocation("immersive_portals:alternate1");
        if (DimensionType.byName(resourceLocation) == null) {
            DimensionManager.registerDimension(
                resourceLocation,
                AlternateDimensionEntry.instance1,
                null,
                true
            );
        }
        ModMain.alternate1 = DimensionType.byName(resourceLocation);
        
        resourceLocation = new ResourceLocation("immersive_portals:alternate2");
        if (DimensionType.byName(resourceLocation) == null) {
            DimensionManager.registerDimension(
                resourceLocation,
                AlternateDimensionEntry.instance2,
                null,
                true
            );
        }
        ModMain.alternate2 = DimensionType.byName(resourceLocation);
        
        resourceLocation = new ResourceLocation("immersive_portals:alternate3");
        if (DimensionType.byName(resourceLocation) == null) {
            DimensionManager.registerDimension(
                resourceLocation,
                AlternateDimensionEntry.instance3,
                null,
                true
            );
        }
        ModMain.alternate3 = DimensionType.byName(resourceLocation);
        
        resourceLocation = new ResourceLocation("immersive_portals:alternate4");
        if (DimensionType.byName(resourceLocation) == null) {
            DimensionManager.registerDimension(
                resourceLocation,
                AlternateDimensionEntry.instance4,
                null,
                true
            );
        }
        ModMain.alternate4 = DimensionType.byName(resourceLocation);
        
        resourceLocation = new ResourceLocation("immersive_portals:alternate5");
        if (DimensionType.byName(resourceLocation) == null) {
            DimensionManager.registerDimension(
                resourceLocation,
                AlternateDimensionEntry.instance5,
                null,
                true
            );
        }
        ModMain.alternate5 = DimensionType.byName(resourceLocation);
    }
    
    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!Global.serverTeleportationManager.isFiringMyChangeDimensionEvent) {
            PlayerEntity player = event.getPlayer();
            if (player instanceof ServerPlayerEntity) {
                GlobalPortalStorage.onPlayerLoggedIn((ServerPlayerEntity) player);
            }
        }
    }
    
    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            IForgeRegistry<Block> registry = blockRegistryEvent.getRegistry();
            
            PortalPlaceholderBlock.instance = new PortalPlaceholderBlock(
                Block.Properties.create(Material.PORTAL)
                    .doesNotBlockMovement()
                    .sound(SoundType.GLASS)
                    .hardnessAndResistance(99999, 0)
                    .lightValue(15)
            );
            PortalPlaceholderBlock.instance.setRegistryName(
                new ResourceLocation("immersive_portals", "portal_placeholder")
            );
            registry.register(
                PortalPlaceholderBlock.instance
            );
            
            ModMain.portalHelperBlock = new Block(Block.Properties.create(Material.IRON));
            ModMain.portalHelperBlock.setRegistryName(
                new ResourceLocation("immersive_portals", "portal_helper")
            );
            registry.register(
                ModMain.portalHelperBlock
            );
        }
        
        @SubscribeEvent
        public static void onItemRegistry(final RegistryEvent.Register<Item> event) {
            IForgeRegistry<Item> registry = event.getRegistry();
            
            ModMain.portalHelperBlockItem = new BlockItem(
                ModMain.portalHelperBlock,
                new Item.Properties().group(ItemGroup.MISC)
            );
            ModMain.portalHelperBlockItem.setRegistryName(
                new ResourceLocation("immersive_portals", "portal_helper")
            );
            registry.register(
                ModMain.portalHelperBlockItem
            );
        }
        
        @SubscribeEvent
        public static void onEntityRegistry(RegistryEvent.Register<EntityType<?>> event) {
            Portal.entityType = EntityType.Builder.create(
                Portal::new, EntityClassification.MISC
            ).size(
                1, 1
            ).immuneToFire().setCustomClientFactory((a, world) -> new Portal(
                    Portal.entityType,
                    world
                )
            ).build(
                "immersive_portals:portal"
            );
            event.getRegistry().register(
                Portal.entityType.setRegistryName(
                    "immersive_portals:portal"
                )
            );
            
            
            NetherPortalEntity.entityType = EntityType.Builder.create(
                NetherPortalEntity::new, EntityClassification.MISC
            ).size(
                1, 1
            ).immuneToFire().setCustomClientFactory((a, world) ->
                new NetherPortalEntity(NetherPortalEntity.entityType, world)
            ).build(
                "immersive_portals:nether_portal_new"
            );
            event.getRegistry().register(
                NetherPortalEntity.entityType.setRegistryName(
                    "immersive_portals:nether_portal_new")
            );
            
            EndPortalEntity.entityType = EntityType.Builder.create(
                EndPortalEntity::new, EntityClassification.MISC
            ).size(
                1, 1
            ).immuneToFire().setCustomClientFactory((a, world) ->
                new EndPortalEntity(EndPortalEntity.entityType, world)
            ).build(
                "immersive_portals:end_portal"
            );
            event.getRegistry().register(
                EndPortalEntity.entityType.setRegistryName("immersive_portals:end_portal")
            );
            
            Mirror.entityType = EntityType.Builder.create(
                Mirror::new, EntityClassification.MISC
            ).size(
                1, 1
            ).immuneToFire().setCustomClientFactory((a, world) ->
                new Mirror(Mirror.entityType, world)
            ).build(
                "immersive_portals:mirror"
            );
            event.getRegistry().register(
                Mirror.entityType.setRegistryName("immersive_portals:mirror")
            );
            
            BreakableMirror.entityType = EntityType.Builder.create(
                BreakableMirror::new, EntityClassification.MISC
            ).size(
                1, 1
            ).immuneToFire().setCustomClientFactory((a, world) ->
                new BreakableMirror(BreakableMirror.entityType, world)
            ).build(
                "immersive_portals:breakable_mirror"
            );
            event.getRegistry().register(
                BreakableMirror.entityType.setRegistryName("immersive_portals:breakable_mirror")
            );
            
            GlobalTrackedPortal.entityType = EntityType.Builder.create(
                GlobalTrackedPortal::new, EntityClassification.MISC
            ).size(
                1, 1
            ).immuneToFire().setCustomClientFactory((a, world) ->
                new GlobalTrackedPortal(GlobalTrackedPortal.entityType, world)
            ).build(
                "immersive_portals:global_tracked_portal"
            );
            event.getRegistry().register(
                GlobalTrackedPortal.entityType.setRegistryName(
                    "immersive_portals:global_tracked_portal")
            );
            
            BorderPortal.entityType = EntityType.Builder.create(
                BorderPortal::new, EntityClassification.MISC
            ).size(
                1, 1
            ).immuneToFire().setCustomClientFactory((a, world) ->
                new BorderPortal(BorderPortal.entityType, world)
            ).build(
                "immersive_portals:border_portal"
            );
            event.getRegistry().register(
                BorderPortal.entityType.setRegistryName("immersive_portals:border_portal")
            );
            
            VerticalConnectingPortal.entityType = EntityType.Builder.create(
                VerticalConnectingPortal::new, EntityClassification.MISC
            ).size(
                1, 1
            ).immuneToFire().setCustomClientFactory((a, world) ->
                new VerticalConnectingPortal(VerticalConnectingPortal.entityType, world)
            ).build(
                "immersive_portals:end_floor_portal"
            );
            event.getRegistry().register(
                VerticalConnectingPortal.entityType.setRegistryName(
                    "immersive_portals:end_floor_portal")
            );
            
            GeneralBreakablePortal.entityType = EntityType.Builder.create(
                GeneralBreakablePortal::new, EntityClassification.MISC
            ).size(
                1, 1
            ).immuneToFire().setCustomClientFactory((a, world) ->
                new GeneralBreakablePortal(GeneralBreakablePortal.entityType, world)
            ).build(
                "immersive_portals:general_breakable_portal"
            );
            event.getRegistry().register(
                GeneralBreakablePortal.entityType.setRegistryName(
                    "immersive_portals:general_breakable_portal")
            );
            
            LoadingIndicatorEntity.entityType = EntityType.Builder.create(
                LoadingIndicatorEntity::new, EntityClassification.MISC
            ).size(
                1, 1
            ).immuneToFire().setCustomClientFactory((a, world) ->
                new LoadingIndicatorEntity(LoadingIndicatorEntity.entityType, world)
            ).build(
                "immersive_portals:loading_indicator"
            );
            event.getRegistry().register(
                LoadingIndicatorEntity.entityType.setRegistryName(
                    "immersive_portals:loading_indicator")
            );
        }
        
        @SubscribeEvent
        public static void onDimensionRegistry(RegistryEvent.Register<ModDimension> event) {
            AlternateDimensionEntry.instance1 = new AlternateDimensionEntry(
                AlternateDimension::getChunkGenerator1
            );
            AlternateDimensionEntry.instance1.setRegistryName("immersive_portals:alternate1");
            event.getRegistry().register(AlternateDimensionEntry.instance1);
            
            AlternateDimensionEntry.instance2 = new AlternateDimensionEntry(
                AlternateDimension::getChunkGenerator2
            );
            AlternateDimensionEntry.instance2.setRegistryName("immersive_portals:alternate2");
            event.getRegistry().register(AlternateDimensionEntry.instance2);
            
            AlternateDimensionEntry.instance3 = new AlternateDimensionEntry(
                AlternateDimension::getChunkGenerator3
            );
            AlternateDimensionEntry.instance3.setRegistryName("immersive_portals:alternate3");
            event.getRegistry().register(AlternateDimensionEntry.instance3);
            
            AlternateDimensionEntry.instance4 = new AlternateDimensionEntry(
                AlternateDimension::getChunkGenerator4
            );
            AlternateDimensionEntry.instance4.setRegistryName("immersive_portals:alternate4");
            event.getRegistry().register(AlternateDimensionEntry.instance4);
            
            AlternateDimensionEntry.instance5 = new AlternateDimensionEntry(
                AlternateDimension::getChunkGenerator5
            );
            AlternateDimensionEntry.instance5.setRegistryName("immersive_portals:alternate5");
            event.getRegistry().register(AlternateDimensionEntry.instance5);
        }
        
        @SubscribeEvent
        public static void onEffectRegistry(RegistryEvent.Register<Effect> event) {
            Effect.class.hashCode();
            
            if (HandReachTweak.statusEffectConstructor == null) {
                Helper.err("Status Effect Constructor is null");
                return;
            }
            
            HandReachTweak.longerReachEffect = HandReachTweak.statusEffectConstructor
                .apply(EffectType.BENEFICIAL, 0)
                .addAttributesModifier(
                    HandReachTweak.handReachMultiplierAttribute,
                    "91AEAA56-2333-2333-2333-2F7F68070635",
                    0.5,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
                );
            Registry.register(
                Registry.EFFECTS,
                new ResourceLocation("immersive_portals", "longer_reach"),
                HandReachTweak.longerReachEffect
            );
        }
        
        @SubscribeEvent
        public static void onPotionRegistry(RegistryEvent.Register<Potion> event) {
            if (HandReachTweak.longerReachEffect == null) {
                return;
            }
            
            HandReachTweak.longerReachPotion = new Potion(
                new EffectInstance(
                    HandReachTweak.longerReachEffect, 7200, 1
                )
            );
            Registry.register(
                Registry.POTION,
                new ResourceLocation("immersive_portals", "longer_reach_potion"),
                HandReachTweak.longerReachPotion
            );
        }
    }
    
}
