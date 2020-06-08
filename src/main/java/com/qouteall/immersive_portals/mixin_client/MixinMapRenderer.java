package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.world.storage.MapData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapItemRenderer.class)
public class MixinMapRenderer {
    @Inject(
        method = "Lnet/minecraft/client/gui/MapItemRenderer;renderMap(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;Lnet/minecraft/world/storage/MapData;ZI)V",
        at = @At("HEAD")
    )
    private void onBeginDraw(
        MatrixStack matrixStack,
        IRenderTypeBuffer vertexConsumerProvider,
        MapData mapState,
        boolean bl,
        int i,
        CallbackInfo ci
    ) {
        if (PortalRendering.isRenderingOddNumberOfMirrors()) {
            RenderStates.shouldForceDisableCull = true;
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/gui/MapItemRenderer;renderMap(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;Lnet/minecraft/world/storage/MapData;ZI)V",
        at = @At("RETURN")
    )
    private void onEndDraw(
        MatrixStack matrixStack,
        IRenderTypeBuffer vertexConsumerProvider,
        MapData mapState,
        boolean bl,
        int i,
        CallbackInfo ci
    ) {
        if (vertexConsumerProvider instanceof IRenderTypeBuffer.Impl) {
            ((IRenderTypeBuffer.Impl) vertexConsumerProvider).finish();
        }
        if (PortalRendering.isRenderingOddNumberOfMirrors()) {
            RenderStates.shouldForceDisableCull = false;
        }
    }
}
