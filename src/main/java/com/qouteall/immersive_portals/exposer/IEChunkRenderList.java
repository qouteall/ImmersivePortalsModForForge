package com.qouteall.immersive_portals.exposer;

import java.util.List;
import net.minecraft.client.renderer.chunk.ChunkRender;

public interface IEChunkRenderList {
    public void setCameraPos(double x, double y, double z);
    
    List<ChunkRender> getChunkRenderers();
    
    void setChunkRenderers(List<ChunkRender> arg);
}
