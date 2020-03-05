package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.render.FogRendererContext;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FogRenderer.class)
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
    
    
    //avoid alternate dimension dark when seeing from overworld
//    @Redirect(
//        method = "updateFogColor",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;getProjectedView()Lnet/minecraft/util/math/Vec3d;"
//        )
//    )
//    private static Vec3d redirectCameraGetPos(ActiveRenderInfo camera) {
//        ClientWorld world = Minecraft.getInstance().world;
//        if (world != null && world.dimension instanceof AlternateDimension) {
//            return new Vec3d(
//                camera.getProjectedView().x,
//                Math.max(32.0, camera.getProjectedView().y),
//                camera.getProjectedView().z
//            );
//        }
//        else {
//            return camera.getProjectedView();
//        }
//    }
    
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
    
    
    //TODO remove boss fog to make end transition seamless
}
