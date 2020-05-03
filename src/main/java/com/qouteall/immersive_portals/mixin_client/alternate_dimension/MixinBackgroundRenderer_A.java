package com.qouteall.immersive_portals.mixin_client.alternate_dimension;

import com.qouteall.hiding_in_the_bushes.alternate_dimension.AlternateDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FogRenderer.class)
public class MixinBackgroundRenderer_A {
    //avoid alternate dimension dark when seeing from overworld
    @Redirect(
        method = "Lnet/minecraft/client/renderer/FogRenderer;updateFogColor(Lnet/minecraft/client/renderer/ActiveRenderInfo;FLnet/minecraft/client/world/ClientWorld;IF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;getProjectedView()Lnet/minecraft/util/math/Vec3d;"
        )
    )
    private static Vec3d redirectCameraGetPos(ActiveRenderInfo camera) {
        ClientWorld world = Minecraft.getInstance().world;
        if (world != null && world.dimension instanceof AlternateDimension) {
            return new Vec3d(
                camera.getProjectedView().x,
                Math.max(32.0, camera.getProjectedView().y),
                camera.getProjectedView().z
            );
        }
        else {
            return camera.getProjectedView();
        }
    }
}
