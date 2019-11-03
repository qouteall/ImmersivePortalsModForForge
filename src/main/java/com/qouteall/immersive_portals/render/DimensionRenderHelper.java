package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.ducks.IEBackgroundRenderer;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DimensionRenderHelper {
    private Minecraft mc;
    public World world;
    
    public final FogRenderer fogRenderer;
    
    public final LightTexture lightmapTexture;
    
    public DimensionRenderHelper(World world) {
        mc = Minecraft.getInstance();
        this.world = world;
    
        if (mc.world == world) {
            IEGameRenderer gameRenderer = (IEGameRenderer) mc.gameRenderer;
        
            lightmapTexture = gameRenderer.getLightmapTextureManager();
            fogRenderer = gameRenderer.getBackgroundRenderer();
        }
        else {
            lightmapTexture = new LightTexture(mc.gameRenderer);
            fogRenderer = new FogRenderer(mc.gameRenderer);
        }
    
        ((IEBackgroundRenderer) fogRenderer).setDimensionConstraint(world.dimension.getType());
    }
    
    public Vec3d getFogColor() {
        return ((IEBackgroundRenderer) fogRenderer).getFogColor();
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
    
    public void switchToMe() {
        IEGameRenderer gameRenderer = (IEGameRenderer) mc.gameRenderer;
        gameRenderer.setBackgroundRenderer(fogRenderer);
        gameRenderer.setLightmapTextureManager(lightmapTexture);
    }
}
