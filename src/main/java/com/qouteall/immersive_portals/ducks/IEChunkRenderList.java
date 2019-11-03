package com.qouteall.immersive_portals.ducks;

import net.minecraft.client.renderer.chunk.ChunkRender;

import java.util.List;

public interface IEChunkRenderList {
    public void setCameraPos(double x, double y, double z);
    
    List<ChunkRender> getChunkRenderers();
    
    void setChunkRenderers(List<ChunkRender> arg);
}
