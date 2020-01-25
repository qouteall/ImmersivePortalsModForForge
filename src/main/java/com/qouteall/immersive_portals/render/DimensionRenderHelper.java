package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.World;

public class DimensionRenderHelper {
    private Minecraft mc;
    public World world;
    
    public final LightTexture lightmapTexture;
    
    public DimensionRenderHelper(World world) {
        mc = Minecraft.getInstance();
        this.world = world;
    
        if (mc.world == world) {
            IEGameRenderer gameRenderer = (IEGameRenderer) mc.gameRenderer;
        
            lightmapTexture = gameRenderer.getLightmapTextureManager();
        }
        else {
            lightmapTexture = new LightTexture(mc.gameRenderer, mc);
        }
    }
    
    public void tick() {
        lightmapTexture.tick();
    }
    
    //TODO cleanup it
    public void cleanUp() {
        if (world != mc.world) {
            lightmapTexture.close();
        }
    }
    
}
