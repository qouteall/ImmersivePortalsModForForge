package com.qouteall.hiding_in_the_bushes.fix_model_data;

import net.minecraft.world.chunk.Chunk;

public interface IEChunkRenderTask {
    
    void portal_setChunk(Chunk d);
    
    Chunk portal_getDimension();
}
