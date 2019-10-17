package com.immersive_portals;

import com.qouteall.immersive_portals.ModMainClient;
import com.qouteall.immersive_portals.portal.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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
@Mod("assets/immersive_portals")
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
        
        ModMainClient.onInitializeClient();
    }
    
    private void setup(final FMLCommonSetupEvent event) {
        // some preinit code
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }
    
    private void doClientStuff(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
        LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().gameSettings);
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
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
    
    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            PortalPlaceholderBlock.instance.setRegistryName(
                new ResourceLocation("assets/immersive_portals", "portal_placeholder")
            );
            blockRegistryEvent.getRegistry().register(
                PortalPlaceholderBlock.instance
            );
        }
        
        @SubscribeEvent
        public static void onEntityRegistry(RegistryEvent.Register<EntityType<?>> event) {
            event.getRegistry().register(
                EntityType.Builder.create(
                    Portal::new, EntityClassification.MISC
                ).size(
                    1, 1
                ).immuneToFire().build(
                    "immersive_portals:portal"
                )
            );
    
            event.getRegistry().register(
                EntityType.Builder.create(
                    NetherPortalEntity::new, EntityClassification.MISC
                ).size(
                    1, 1
                ).immuneToFire().build(
                    "immersive_portals:breakable_nether_portal"
                )
            );
    
            event.getRegistry().register(
                EntityType.Builder.create(
                    EndPortalEntity::new, EntityClassification.MISC
                ).size(
                    1, 1
                ).immuneToFire().build(
                    "immersive_portals:end_portal"
                )
            );
    
            event.getRegistry().register(
                EntityType.Builder.create(
                    Mirror::new, EntityClassification.MISC
                ).size(
                    1, 1
                ).immuneToFire().build(
                    "immersive_portals:mirror"
                )
            );
    
            event.getRegistry().register(
                EntityType.Builder.create(
                    BreakableMirror::new, EntityClassification.MISC
                ).size(
                    1, 1
                ).immuneToFire().build(
                    "immersive_portals:breakable_mirror"
                )
            );
    
            event.getRegistry().register(
                EntityType.Builder.create(
                    LoadingIndicatorEntity::new, EntityClassification.MISC
                ).size(
                    1, 1
                ).immuneToFire().build(
                    "immersive_portals:loading_indicator"
                )
            );
        }
    }
}
