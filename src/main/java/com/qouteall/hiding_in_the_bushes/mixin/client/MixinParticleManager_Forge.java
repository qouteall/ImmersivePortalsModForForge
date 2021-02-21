package com.qouteall.hiding_in_the_bushes.mixin.client;

import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ParticleManager.class, remap = false)
public class MixinParticleManager_Forge {
    @Redirect(
        method = "renderParticles(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer$Impl;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/ActiveRenderInfo;FLnet/minecraft/client/renderer/culling/ClippingHelper;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/Particle;shouldCull()Z"
        )
    )
    private boolean redirectShouldCull(Particle particle) {
        if (!RenderStates.shouldRenderParticle(particle)) {
            return true;
        }
        return particle.shouldCull();
    }
}
