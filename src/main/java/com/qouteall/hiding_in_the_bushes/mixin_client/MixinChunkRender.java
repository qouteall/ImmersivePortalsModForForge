package com.qouteall.hiding_in_the_bushes.mixin_client;

import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRender;
import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRenderDispatcher;
import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRenderTask;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkRenderDispatcher.ChunkRender.class)
public class MixinChunkRender implements IEChunkRender {
    @Shadow(remap = false, aliases = "this$0")
    private ChunkRenderDispatcher this$0;
    
    @Shadow
    @Final
    private BlockPos.Mutable position;
    
    @Inject(
        method = "makeCompileTaskChunk",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onMakeCompileTaskChunk(CallbackInfoReturnable<ChunkRenderDispatcher.ChunkRender.ChunkRenderTask> cir) {
        ChunkRenderDispatcher.ChunkRender.ChunkRenderTask task = cir.getReturnValue();
        World world = ((IEChunkRenderDispatcher) this$0).myGetWorld();
        IChunk chunk = world.getChunk(position);
        if (chunk instanceof Chunk) {
            ((IEChunkRenderTask) task).portal_setChunk(((Chunk) chunk));
        }
    }
    
}
