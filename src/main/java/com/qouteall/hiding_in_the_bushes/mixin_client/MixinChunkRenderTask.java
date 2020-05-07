package com.qouteall.hiding_in_the_bushes.mixin_client;

import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRenderTask;
import com.qouteall.hiding_in_the_bushes.fix_model_data.ModelDataHacker;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collections;
import java.util.Map;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$ChunkRender$ChunkRenderTask")
public class MixinChunkRenderTask implements IEChunkRenderTask {
    @Shadow(remap = false)
    protected Map<BlockPos, IModelData> modelData;
    private DimensionType portal_dimension;
    
    /**
     * @author qouteall
     * @reason Fix model data
     */
    @Overwrite(remap = false)
    public net.minecraftforge.client.model.data.IModelData getModelData(net.minecraft.util.math.BlockPos pos) {
        //when there are multiple model data in different dimensions' same coordinate
        //it will still not work but it's rare
        Map<BlockPos, IModelData> modelData = this.modelData;
        if (modelData == (Map) Collections.emptyMap()) {
            return portal_getModelData(pos);
        }
        return modelData.computeIfAbsent(pos, this::portal_getModelData);
    }
    
    private IModelData portal_getModelData(BlockPos pos_) {
        if (portal_dimension == null) {
            ModelDataHacker.log(() -> {
                Helper.err("Null dimension in render task " + pos_);
            });
            return net.minecraftforge.client.model.data.EmptyModelData.INSTANCE;
        }else {
            return ModelDataHacker.fetchMissingModelData(
                CGlobal.clientWorldLoader.getWorld(portal_dimension),
                pos_
            );
        }
    }
    
    @Override
    public void portal_setDimension(DimensionType d) {
        portal_dimension = d;
    }
    
    @Override
    public DimensionType portal_getDimension() {
        return portal_dimension;
    }

//    @Override
//    public void setModelData(Map<BlockPos, IModelData> data) {
//        modelData = data;
//    }
}
