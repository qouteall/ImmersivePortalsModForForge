package com.qouteall.imm_ptl_peripheral.altius_world;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.qouteall.imm_ptl_peripheral.ducks.IECreateWorldScreen;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.command.Commands;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.codec.DatapackCodec;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.WorldGenSettingsExport;
import net.minecraft.util.registry.WorldSettingsImport;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class WorldCreationDimensionHelper {
    public static IResourceManager fetchResourceManager(
        ResourcePackList resourcePackManager,
        DatapackCodec dataPackSettings
    ) {
        final Minecraft client = Minecraft.getInstance();
        
        Helper.log("Getting Dimension List");
        
        DatapackCodec dataPackSettings2 = MinecraftServer.func_240772_a_(
            resourcePackManager, dataPackSettings, true
        );
        CompletableFuture<DataPackRegistries> completableFuture =
            DataPackRegistries.func_240961_a_(
                resourcePackManager.func_232623_f_(),
                Commands.EnvironmentType.INTEGRATED,
                2, Util.getServerExecutor(), client
            );
        
        client.driveUntil(completableFuture::isDone);
        DataPackRegistries serverResourceManager = null;
        try {
            serverResourceManager = (DataPackRegistries) completableFuture.get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        
        return serverResourceManager.func_240970_h_();
    }
    
    public static DimensionGeneratorSettings getPopulatedGeneratorOptions(
        DynamicRegistries.Impl registryTracker, IResourceManager resourceManager,
        DimensionGeneratorSettings generatorOptions
    ) {
        WorldGenSettingsExport<JsonElement> registryReadingOps =
            WorldGenSettingsExport.func_240896_a_(JsonOps.INSTANCE, registryTracker);
        WorldSettingsImport<JsonElement> registryOps =
            WorldSettingsImport.func_244335_a(JsonOps.INSTANCE, (IResourceManager) resourceManager, registryTracker);
        DataResult<DimensionGeneratorSettings> dataResult =
            DimensionGeneratorSettings.field_236201_a_.encodeStart(registryReadingOps, generatorOptions)
                .setLifecycle(Lifecycle.stable())
                .flatMap((jsonElement) -> {
                    return DimensionGeneratorSettings.field_236201_a_.parse(registryOps, jsonElement);
                });
        
        DimensionGeneratorSettings result = (DimensionGeneratorSettings) dataResult.resultOrPartial(
            Util.func_240982_a_(
                "Error reading worldgen settings after loading data packs: ",
                Helper::log
            )
        ).orElse(generatorOptions);
        
        return result;
        
    }
    
    public static DimensionGeneratorSettings getPopulatedGeneratorOptions(CreateWorldScreen createWorldScreen, DimensionGeneratorSettings rawGeneratorOptions) {
        IECreateWorldScreen ieCreateWorldScreen = (IECreateWorldScreen) createWorldScreen;
        
        IResourceManager resourceManager = fetchResourceManager(
            ieCreateWorldScreen.portal_getResourcePackManager(),
            ieCreateWorldScreen.portal_getDataPackSettings()
        );
        
        final DynamicRegistries.Impl registryTracker =
            createWorldScreen.field_238934_c_.func_239055_b_();
        DimensionGeneratorSettings populatedGeneratorOptions = getPopulatedGeneratorOptions(
            registryTracker, resourceManager, rawGeneratorOptions
        );
        return populatedGeneratorOptions;
    }
}
