package com.qouteall.immersive_portals.ducks;

import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.entity.EntityRendererManager;

import java.util.List;

public interface IEWorldRenderer {
    ViewFrustum getChunkRenderDispatcher();
    
    List getChunkInfos();
    
    void setChunkInfos(List list);
    
    EntityRendererManager getEntityRenderDispatcher();
}
