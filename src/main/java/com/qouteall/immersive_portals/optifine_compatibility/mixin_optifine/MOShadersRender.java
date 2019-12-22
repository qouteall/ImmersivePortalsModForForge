package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.optifine_compatibility.OFGlobal;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.optifine.shaders.ShadersRender", remap = false)
public class MOShadersRender {
    @Inject(method = "renderShadowMap", at = @At("HEAD"), cancellable = true)
    private static void onRenderShadowMap(
        GameRenderer entityRenderer,
        ActiveRenderInfo activeRenderInfo,
        int pass,
        float partialTicks,
        long finishTimeNano,
        CallbackInfo ci
    ) {
        if (!OFGlobal.alwaysRenderShadowMap) {
            if (OFGlobal.shaderContextManager.isCurrentDimensionRendered()) {
                ci.cancel();
            }
        }
    }
}
