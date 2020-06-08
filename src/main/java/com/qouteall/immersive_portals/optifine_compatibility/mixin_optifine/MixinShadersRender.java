package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.optifine.shaders.ShadersRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShadersRender.class, remap = false)
public class MixinShadersRender {
    
    @Inject(
        method = "updateActiveRenderInfo",
        at = @At("RETURN")
    )
    private static void onUpdateCameraForRenderingShadow(
        ActiveRenderInfo camera,
        Minecraft mc,
        float partialTicks,
        CallbackInfo ci
    ) {
        PortalRendering.adjustCameraPos(camera);
    }
}
