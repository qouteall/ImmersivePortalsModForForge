package com.qouteall.hiding_in_the_bushes.fix_model_data;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.data.IModelData;

import java.util.Map;

public interface IEChunkRenderTask {
    void setModelData(Map<BlockPos, IModelData> data);
}
