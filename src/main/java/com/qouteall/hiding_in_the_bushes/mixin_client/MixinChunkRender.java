package com.qouteall.hiding_in_the_bushes.mixin_client;

import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRender;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkRenderDispatcher.ChunkRender.class)
public class MixinChunkRender implements IEChunkRender {
    @Shadow(remap = false)
    private ChunkRenderDispatcher this$0;
    
    @Override
    public ChunkRenderDispatcher getParent() {
        return this$0;
    }
}
