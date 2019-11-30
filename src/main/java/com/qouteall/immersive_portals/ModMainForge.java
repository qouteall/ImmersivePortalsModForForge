package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.portal.*;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.EndFloorPortal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.NewNetherPortalEntity;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

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
    
        NetherPortalEntity.init();
    
        NewNetherPortalEntity.init();
        
        ModMainClient.onInitializeClient();
    
        ModMain.onInitialize();
    }
    
    private void setup(final FMLCommonSetupEvent event) {
    
    }
    
    private void doClientStuff(final FMLClientSetupEvent event) {
    
    }
    
    private void enqueueIMC(final InterModEnqueueEvent event) {
        // some example code to dispatch IMC to another mod
        InterModComms.sendTo("examplemod", "helloworld", () -> {
            LOGGER.info("Hello world from the MDK");
            return "Hello world";
        });
    }
    
    private void processIMC(final InterModProcessEvent event) {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
            map(m -> m.getMessageSupplier().get()).
            collect(Collectors.toList()));
    }
    
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
    
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
            ).immuneToFire().setCustomClientFactory((a, world) -> new Portal(Portal.entityType,
                world)
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
    
            EndFloorPortal.entityType = EntityType.Builder.create(
                EndFloorPortal::new, EntityClassification.MISC
            ).size(
                1, 1
            ).immuneToFire().setCustomClientFactory((a, world) ->
                new EndFloorPortal(EndFloorPortal.entityType, world)
            ).build(
                "immersive_portals:end_floor_portal"
            );
            event.getRegistry().register(
                EndFloorPortal.entityType.setRegistryName("immersive_portals:end_floor_portal")
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
    }
}
