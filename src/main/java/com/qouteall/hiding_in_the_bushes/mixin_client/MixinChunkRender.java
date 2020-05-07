package com.qouteall.hiding_in_the_bushes.mixin_client;

import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRender;
import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRenderDispatcher;
import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRenderTask;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkRenderDispatcher.ChunkRender.class)
public class MixinChunkRender implements IEChunkRender {
    @Shadow(remap = false, aliases = "this$0")
    private ChunkRenderDispatcher this$0;
    
    @Inject(
        method = "makeCompileTaskChunk",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onMakeCompileTaskChunk(CallbackInfoReturnable<ChunkRenderDispatcher.ChunkRender.ChunkRenderTask> cir) {
        ChunkRenderDispatcher.ChunkRender.ChunkRenderTask task = cir.getReturnValue();
        World world = ((IEChunkRenderDispatcher) this$0).myGetWorld();
        ((IEChunkRenderTask) task).portal_setDimension(world.dimension.getType());
    }
    
//    @Override
//    public ChunkRenderDispatcher getParent() {
//        return null;
//    }
}
