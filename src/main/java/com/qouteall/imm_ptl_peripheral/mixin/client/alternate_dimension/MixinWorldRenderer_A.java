package com.qouteall.imm_ptl_peripheral.mixin.client.alternate_dimension;

import com.qouteall.immersive_portals.ModMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer_A {
    //avoid alternate dimension dark sky in low y
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;renderSky(Lcom/mojang/blaze3d/matrix/MatrixStack;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld$ClientWorldInfo;func_239159_f_()D"
        )
    )
    private double redirectGetSkyDarknessHeight(ClientWorld.ClientWorldInfo properties) {
        if (ModMain.isAlternateDimension(Minecraft.getInstance().world)) {
            return -10000;
        }
        return properties.func_239159_f_();
    }
}
