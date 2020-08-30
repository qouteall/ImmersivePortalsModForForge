package com.qouteall.immersive_portals.mixin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IRenderTypeBuffer.Impl.class)
public class MixinVertexConsumerProviderImmediate {
    @Inject(
        method = "Lnet/minecraft/client/renderer/IRenderTypeBuffer$Impl;finish(Lnet/minecraft/client/renderer/RenderType;)V",
        at = @At("HEAD")
    )
    private void onBeginDraw(RenderType layer, CallbackInfo ci) {
        if (PortalRendering.isRenderingOddNumberOfMirrors()) {
            RenderStates.shouldForceDisableCull = true;
            GlStateManager.disableCull();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/IRenderTypeBuffer$Impl;finish(Lnet/minecraft/client/renderer/RenderType;)V",
        at = @At("RETURN")
    )
    private void onEndDraw(RenderType layer, CallbackInfo ci) {
        RenderStates.shouldForceDisableCull = false;
        GlStateManager.enableCull();
    }
}
