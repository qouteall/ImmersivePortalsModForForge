package com.qouteall.hiding_in_the_bushes.mixin_client;

import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRenderTask;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$ChunkRender$ChunkRenderTask")
public class MixinChunkRenderTask implements IEChunkRenderTask {
    @Shadow
    protected Map<BlockPos, IModelData> modelData;
    
    @Override
    public void setModelData(Map<BlockPos, IModelData> data) {
        modelData = data;
    }
}
