package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ducks.IEEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.OverlayRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OverlayRenderer.class)
public class MixinInGameOverlayRenderer {
    //avoid rendering suffocating when colliding with portal
    @Inject(
        method = "Lnet/minecraft/client/renderer/OverlayRenderer;renderTexture(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lcom/mojang/blaze3d/matrix/MatrixStack;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onRenderInWallOverlay(
        Minecraft minecraftClient,
        TextureAtlasSprite sprite,
        MatrixStack matrixStack,
        CallbackInfo ci
    ) {
        if (CGlobal.renderer.isRendering()) {
            ci.cancel();
        }
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null) {
            if (((IEEntity) player).getCollidingPortal() != null) {
                ci.cancel();
            }
        }
    }
}
