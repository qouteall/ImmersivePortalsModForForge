package com.qouteall.immersive_portals.ducks;

import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.entity.EntityRendererManager;

import java.util.List;

public interface IEWorldRenderer {
    EntityRendererManager getEntityRenderDispatcher();
    
    ViewFrustum getBuiltChunkStorage();
    
    ObjectList getVisibleChunks();
    
    void setVisibleChunks(ObjectList l);
    
}
