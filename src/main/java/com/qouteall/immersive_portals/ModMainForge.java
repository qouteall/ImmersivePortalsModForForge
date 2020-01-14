package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.alternate_dimension.AlternateDimensionEntry;
import com.qouteall.immersive_portals.portal.*;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.NewNetherPortalEntity;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ModDimension;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.RegisterDimensionsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("immersive_portals")
public class ModMainForge {
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    
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
    }
    
    private void setup(final FMLCommonSetupEvent event) {
        ModMain.onInitialize();
    }
    
    private void doClientStuff(final FMLClientSetupEvent event) {
        ModMainClient.onInitializeClient();
    
        Minecraft.getInstance().execute(() -> {
            if (ConfigClient.isInitialCompatibilityRenderMode()) {
                CGlobal.renderMode = CGlobal.RenderMode.compatibility;
                Helper.log("Initially Switched to Compatibility Render Mode");
            }
            ConfigClient.spec.save();
        });
    }
    
    private void enqueueIMC(final InterModEnqueueEvent event) {
    
    }
    
    private void processIMC(final InterModProcessEvent event) {
    
    }
    
    
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
    
    }
    
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        ModMain.checkMixinState();
    }
    
    @SubscribeEvent
    public void onRegisterDimensionsEvent(RegisterDimensionsEvent event) {
        ResourceLocation resourceLocation = new ResourceLocation("immersive_portals:alternate1");
        if (DimensionType.byName(resourceLocation) == null) {
            DimensionManager.registerDimension(
                resourceLocation,
                AlternateDimensionEntry.instance,
                null,
                true
            );
        }
        
        ModMain.alternate = DimensionType.byName(resourceLocation);
    }
    
    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            
            
            PortalPlaceholderBlock.instance.setRegistryName(
                new ResourceLocation("immersive_portals", "portal_placeholder")
            );
            blockRegistryEvent.getRegistry().register(
                PortalPlaceholderBlock.instance
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
                "immersive_portals:breakable_nether_portal"
            );
            event.getRegistry().register(
                NetherPortalEntity.entityType.setRegistryName(
                    "immersive_portals:breakable_nether_portal")
            );
    
            NewNetherPortalEntity.entityType = EntityType.Builder.create(
                NewNetherPortalEntity::new, EntityClassification.MISC
            ).size(
                1, 1
            ).immuneToFire().setCustomClientFactory((a, world) ->
                new NewNetherPortalEntity(NewNetherPortalEntity.entityType, world)
            ).build(
                "immersive_portals:nether_portal_new"
            );
            event.getRegistry().register(
                NewNetherPortalEntity.entityType.setRegistryName(
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
            AlternateDimensionEntry.instance = new AlternateDimensionEntry();
            AlternateDimensionEntry.instance.setRegistryName("immersive_portals:alternate1");
            event.getRegistry().register(AlternateDimensionEntry.instance);
        }
    }
}
