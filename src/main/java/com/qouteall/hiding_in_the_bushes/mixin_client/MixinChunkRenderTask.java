package com.qouteall.hiding_in_the_bushes.mixin_client;

import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRenderTask;
import com.qouteall.hiding_in_the_bushes.fix_model_data.ModelDataHacker;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$ChunkRender$ChunkRenderTask")
public class MixinChunkRenderTask implements IEChunkRenderTask {
    @Shadow(remap = false)
    protected Map<BlockPos, IModelData> modelData;
    private Chunk portal_chunk;

    /**
     * @author qouteall
     * @reason Fix model data
     */
    @Overwrite(remap = false)
    public net.minecraftforge.client.model.data.IModelData getModelData(net.minecraft.util.math.BlockPos pos) {
        //when there are multiple model data in different dimensions' same coordinate
        //it will still not work but it's rare
        if (this.modelData == (Map) Collections.emptyMap()) {
            modelData = new HashMap<>();
        }
        return this.modelData.computeIfAbsent(pos, this::portal_getModelData);
    }

    private IModelData portal_getModelData(BlockPos pos_) {
        if (portal_chunk == null) {
            ModelDataHacker.log(() -> {
                Helper.err("Null chunk in render task " + pos_);
            });
            return net.minecraftforge.client.model.data.EmptyModelData.INSTANCE;
        }
        else {
            return ModelDataHacker.fetchMissingModelDataFromChunk(
                portal_chunk,
                pos_
            );
        }
    }
    
    @Override
    public void portal_setChunk(Chunk d) {
        portal_chunk = d;
    }
    
    @Override
    public Chunk portal_getDimension() {
        return portal_chunk;
    }

}
