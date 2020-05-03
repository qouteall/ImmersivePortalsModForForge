package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FogRenderer.class)
public class MixinBackgroundRenderer {
    @Shadow
    private static float red;
    @Shadow
    private static float green;
    @Shadow
    private static float blue;
    @Shadow
    private static int lastWaterFogColor = -1;
    @Shadow
    private static int waterFogColor = -1;
    @Shadow
    private static long waterFogUpdateTime = -1L;
    
   
    
    static {
        FogRendererContext.copyContextFromObject = context -> {
            red = context.red;
            green = context.green;
            blue = context.blue;
            lastWaterFogColor = context.waterFogColor;
            waterFogColor = context.nextWaterFogColor;
            waterFogUpdateTime = context.lastWaterFogColorUpdateTime;
        };
        
        FogRendererContext.copyContextToObject = context -> {
            context.red = red;
            context.green = green;
            context.blue = blue;
            context.waterFogColor = lastWaterFogColor;
            context.nextWaterFogColor = waterFogColor;
            context.lastWaterFogColorUpdateTime = waterFogUpdateTime;
        };
        
        FogRendererContext.getCurrentFogColor =
            () -> new Vec3d(red, green, blue);
        
        FogRendererContext.init();
    }
}
