package com.qouteall.imm_ptl_peripheral.mixin.common.alternate_dimension;

import com.qouteall.imm_ptl_peripheral.alternate_dimension.AlternateDimensions;
import com.qouteall.immersive_portals.Global;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.Dimension;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.storage.IServerConfiguration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_A {
    @Shadow
    @Final
    protected IServerConfiguration field_240768_i_;
    
    @Shadow
    public abstract DynamicRegistries func_244267_aX();
    
    @Inject(method = "Lnet/minecraft/server/MinecraftServer;func_240787_a_(Lnet/minecraft/world/chunk/listener/IChunkStatusListener;)V", at = @At("HEAD"))
    private void onBeforeCreateWorlds(
        IChunkStatusListener worldGenerationProgressListener, CallbackInfo ci
    ) {
        SimpleRegistry<Dimension> registry = field_240768_i_.func_230418_z_().func_236224_e_();
        
        DynamicRegistries rm = func_244267_aX();
        
        long seed = field_240768_i_.func_230418_z_().func_236221_b_();
        
        if (Global.enableAlternateDimensions) {
            AlternateDimensions.addAlternateDimensions(registry, rm, seed);
        }
    
        AlternateDimensions.addMissingVanillaDimensions(registry, rm, seed);
    }
    
}
