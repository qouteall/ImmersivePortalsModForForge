package com.qouteall.immersive_portals.mixin_client.alternate_dimension;

import com.qouteall.immersive_portals.ModMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Vector3d;
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
            target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;getProjectedView()Lnet/minecraft/util/math/vector/Vector3d;"
        )
    )
    private static Vector3d redirectCameraGetPos(ActiveRenderInfo camera) {
        ClientWorld world = Minecraft.getInstance().world;
        if (world != null && ModMain.isAlternateDimension(world)) {
            return new Vector3d(
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
