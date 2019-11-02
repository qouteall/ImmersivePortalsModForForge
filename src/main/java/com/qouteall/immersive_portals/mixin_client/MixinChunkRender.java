package com.qouteall.immersive_portals.mixin_client;

import net.minecraft.client.renderer.chunk.ChunkRender;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkRender.class, remap = false)
public class MixinChunkRender {
    @Shadow
    private volatile World world;
    
    @Inject(method = "shouldStayLoaded", at = @At("HEAD"), cancellable = true)
    private void onShouldRebuild(CallbackInfoReturnable<Boolean> cir) {
        if (world == null) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
    
    
}
