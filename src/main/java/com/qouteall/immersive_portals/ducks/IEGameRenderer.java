package com.qouteall.immersive_portals.ducks;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.LightTexture;

public interface IEGameRenderer {
    LightTexture getLightmapTextureManager();
    
    void setLightmapTextureManager(LightTexture manager);
    
    boolean getDoRenderHand();
    
    void setDoRenderHand(boolean e);
    
    void setCamera(ActiveRenderInfo camera);
    
    void setIsRenderingPanorama(boolean cond);
}
