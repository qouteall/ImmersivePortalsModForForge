package com.qouteall.immersive_portals.mixin_client.far_scenery;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.math.vector.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class MixinGameRenderer_F {
    @Shadow
    private boolean debugView;
    
    @Redirect(
        method = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(Lnet/minecraft/client/renderer/ActiveRenderInfo;FZ)Lnet/minecraft/util/math/vector/Matrix4f;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/vector/Matrix4f;perspective(DFFF)Lnet/minecraft/util/math/vector/Matrix4f;"
        )
    )
    Matrix4f redirectProjectionMatrix(
        double fov,
        float aspectRatio,
        float cameraDepth,
        float viewDistance
    ) {
        if (debugView) {
            return Matrix4f.perspective(
                90,
                1,
                0.05F,
                viewDistance
            );
        }
        else {
            return Matrix4f.perspective(
                fov, aspectRatio, cameraDepth, viewDistance
            );
        }
    }
}
