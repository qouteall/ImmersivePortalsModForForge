package com.qouteall.immersive_portals.mixin.common;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.dimension_sync.DimensionIdManagement;
import com.qouteall.immersive_portals.ducks.IEMinecraftServer;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import net.minecraft.world.storage.IServerConfiguration;
import net.minecraft.world.storage.SaveFormat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.WeakReference;
import java.net.Proxy;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements IEMinecraftServer {
    @Shadow
    @Final
    private FrameTimer frameTimer;
    
    @Shadow public abstract IProfiler getProfiler();
    
    private boolean portal_areAllWorldsLoaded;
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onServerConstruct(
        Thread thread, DynamicRegistries.Impl impl,
        SaveFormat.LevelSave session, IServerConfiguration saveProperties,
        ResourcePackList resourcePackManager, Proxy proxy, DataFixer dataFixer,
        DataPackRegistries serverResourceManager, MinecraftSessionService minecraftSessionService,
        GameProfileRepository gameProfileRepository, PlayerProfileCache userCache,
        IChunkStatusListenerFactory worldGenerationProgressListenerFactory, CallbackInfo ci
    ) {
        McHelper.refMinecraftServer = new WeakReference<>((MinecraftServer) ((Object) this));
        
        O_O.loadConfigFabric();
        O_O.onServerConstructed();
    }
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;updateTimeLightAndEntities(Ljava/util/function/BooleanSupplier;)V",
        at = @At("TAIL")
    )
    private void onServerTick(BooleanSupplier booleanSupplier_1, CallbackInfo ci) {
        getProfiler().startSection("imm_ptl_tick");
        ModMain.postServerTickSignal.emit();
        getProfiler().endSection();
    }
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;func_240802_v_()V",
        at = @At("RETURN")
    )
    private void onServerClose(CallbackInfo ci) {
        ModMain.serverCleanupSignal.emit();
    }
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;func_240787_a_(Lnet/minecraft/world/chunk/listener/IChunkStatusListener;)V",
        at = @At("RETURN")
    )
    private void onFinishedLoadingAllWorlds(
        CallbackInfo ci
    ) {
        portal_areAllWorldsLoaded = true;
        DimensionIdManagement.onServerStarted();
    }
    
    @Override
    public FrameTimer getMetricsDataNonClientOnly() {
        return frameTimer;
    }
    
    @Override
    public boolean portal_getAreAllWorldsLoaded() {
        return portal_areAllWorldsLoaded;
    }
}
