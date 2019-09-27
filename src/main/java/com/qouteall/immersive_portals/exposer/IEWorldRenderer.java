package com.qouteall.immersive_portals.exposer;

import java.util.List;
import net.minecraft.client.renderer.AbstractChunkRenderContainer;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.entity.EntityRendererManager;

public interface IEWorldRenderer {
    ViewFrustum getChunkRenderDispatcher();
    
    AbstractChunkRenderContainer getChunkRenderList();
    
    List getChunkInfos();
    
    void setChunkInfos(List list);
    
    EntityRendererManager getEntityRenderDispatcher();
}
