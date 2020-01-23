package com.qouteall.immersive_portals.mixin;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEMinecraftServer;
import net.minecraft.command.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.FrameTimer;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.Proxy;
import java.util.function.BooleanSupplier;

@Mixin(value = MinecraftServer.class)
public class MixinMinecraftServer implements IEMinecraftServer {
    @Shadow
    @Final
    private FrameTimer frameTimer;
    private boolean portal_areAllWorldsLoaded;
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onServerConstruct(
        File file_1,
        Proxy proxy_1,
        DataFixer dataFixer_1,
        Commands commandManager_1,
        YggdrasilAuthenticationService yggdrasilAuthenticationService_1,
        MinecraftSessionService minecraftSessionService_1,
        GameProfileRepository gameProfileRepository_1,
        PlayerProfileCache userCache_1,
        IChunkStatusListenerFactory worldGenerationProgressListenerFactory_1,
        String string_1,
        CallbackInfo ci
    ) {
        Helper.refMinecraftServer = new WeakReference<>((MinecraftServer) ((Object) this));
        portal_areAllWorldsLoaded = false;
    }
    
    @Inject(
        method = "updateTimeLightAndEntities",
        at = @At("TAIL")
    )
    private void onServerTick(BooleanSupplier booleanSupplier_1, CallbackInfo ci) {
        ModMain.postServerTickSignal.emit();
    }
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;run()V",
        at = @At("RETURN")
    )
    private void onServerClose(CallbackInfo ci) {
        NewChunkTrackingGraph.cleanup();
        ModMain.serverTaskList.forceClearTasks();
    }
    
    @Inject(
        method = "loadAllWorlds",
        at = @At("RETURN")
    )
    private void onFinishedLoadingAllWorlds(
        String saveName,
        String worldNameIn,
        long seed,
        WorldType type,
        JsonElement generatorOptions,
        CallbackInfo ci
    ) {
        portal_areAllWorldsLoaded = true;
    }
    
    @Override
    public boolean portal_getAreAllWorldsLoaded() {
        return portal_areAllWorldsLoaded;
    }
    
    @Override
    public FrameTimer getMetricsDataNonClientOnly() {
        return frameTimer;
    }
}
