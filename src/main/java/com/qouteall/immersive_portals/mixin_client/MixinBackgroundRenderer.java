package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
            () -> new Vector3d(red, green, blue);
        
        FogRendererContext.init();
    }
    
    // avoid nether fog color being interfered by nether's weather
    // nether should not be raining. maybe another bug
    @Redirect(
        method = "Lnet/minecraft/client/renderer/FogRenderer;updateFogColor(Lnet/minecraft/client/renderer/ActiveRenderInfo;FLnet/minecraft/client/world/ClientWorld;IF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getRainStrength(F)F"
        )
    )
    private static float redirectGetRainGradient(ClientWorld world, float delta) {
        if (world.func_234923_W_() == World.field_234919_h_) {
            return 0.0f;
        }
        return world.getRainStrength(delta);
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/renderer/FogRenderer;updateFogColor(Lnet/minecraft/client/renderer/ActiveRenderInfo;FLnet/minecraft/client/world/ClientWorld;IF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getThunderStrength(F)F"
        )
    )
    private static float redirectGetThunderGradient(ClientWorld world, float delta) {
        if (world.func_234923_W_() == World.field_234919_h_) {
            return 0.0f;
        }
        return world.getThunderStrength(delta);
    }
}
