package com.qouteall.immersive_portals.exposer;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LightTexture;

public interface IEGameRenderer {
    void applyCameraTransformations_(float float_1);
    
    LightTexture getLightmapTextureManager();
    
    void setLightmapTextureManager(LightTexture manager);
    
    FogRenderer getBackgroundRenderer();
    
    void setBackgroundRenderer(FogRenderer backgroundRenderer);
    
    boolean getDoRenderHand();
    
    void setDoRenderHand(boolean e);
    
    void renderCenter_(float partialTicks, long finishTimeNano);
    
    void setCamera(ActiveRenderInfo camera);
}
