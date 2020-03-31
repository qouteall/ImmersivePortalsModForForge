package com.qouteall.immersive_portals.mixin_client.block_manipulation;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class MixinGameRenderer_B {
    
    //do not update target when rendering portal
    @Redirect(
        method = "Lnet/minecraft/client/renderer/GameRenderer;renderWorld(FJLcom/mojang/blaze3d/matrix/MatrixStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;getMouseOver(F)V"
        )
    )
    private void redirectUpdateTargetedEntity(GameRenderer gameRenderer, float tickDelta) {
        if (!CGlobal.renderer.isRendering()) {
            gameRenderer.getMouseOver(tickDelta);
            BlockManipulationClient.onPointedBlockUpdated(tickDelta);
        }
    }
}
