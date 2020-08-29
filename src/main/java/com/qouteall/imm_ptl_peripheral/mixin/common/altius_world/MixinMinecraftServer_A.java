package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import com.qouteall.imm_ptl_peripheral.altius_world.AltiusInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_A {
    @Shadow
    public abstract ServerWorld getWorld(RegistryKey<World> dimensionType);
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;loadInitialChunks(Lnet/minecraft/world/chunk/listener/IChunkStatusListener;)V",
        at = @At("RETURN")
    )
    private void onStartRegionPrepared(
        IChunkStatusListener worldGenerationProgressListener,
        CallbackInfo ci
    ) {
        AltiusInfo info = AltiusInfo.getInfoFromServer();
        if (info != null) {
            info.createPortals();
        }
    }
}
