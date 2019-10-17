package com.qouteall.immersive_portals.mixin_client;

import net.minecraft.client.renderer.chunk.ChunkRenderCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkRenderCache.class)
public class MixinChunkRenderCache {
    //will this avoid that random crash? I don't know.
    @Inject(
        method = "generateCache",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onCreate(
        World worldIn,
        BlockPos from,
        BlockPos to,
        int padding,
        CallbackInfoReturnable<ChunkRenderCache> cir
    ) {
        if (worldIn == null) {
            cir.setReturnValue(null);
            cir.cancel();
        }
    }
    
    
}
