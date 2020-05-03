package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityRendererDispatcher.class)
public class MixinBlockEntityRenderDispatcher {
    @Inject(
        method = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;renderTileEntity(Lnet/minecraft/tileentity/TileEntity;FLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private <E extends TileEntity> void onRenderTileEntity(
        E blockEntity,
        float tickDelta,
        MatrixStack matrix,
        IRenderTypeBuffer vertexConsumerProvider,
        CallbackInfo ci
    ) {
        if (OFInterface.isShadowPass.getAsBoolean()) {
            return;
        }
        if (CGlobal.renderer.isRendering()) {
            Portal renderingPortal = CGlobal.renderer.getRenderingPortal();
            boolean canRender = renderingPortal.canRenderEntityInsideMe(
                new Vec3d(blockEntity.getPos()),
                0
            );
            if (!canRender) {
                ci.cancel();
            }
        }
    }
}
