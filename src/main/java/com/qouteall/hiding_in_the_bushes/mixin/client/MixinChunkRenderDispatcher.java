package com.qouteall.hiding_in_the_bushes.mixin.client;

import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkRenderDispatcher.class)
public class MixinChunkRenderDispatcher implements IEChunkRenderDispatcher {
    @Shadow
    private World world;
    
    @Override
    public World myGetWorld() {
        return world;
    }
}
