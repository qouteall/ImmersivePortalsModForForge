package com.qouteall.immersive_portals.mixin.client.particle;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class MixinParticleManager implements IEParticleManager {
    @Shadow
    protected ClientWorld world;
    
    // skip particle rendering for far portals
    @Inject(
        method = "Lnet/minecraft/client/particle/ParticleManager;renderParticles(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer$Impl;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/ActiveRenderInfo;F)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onBeginRenderParticles(
        MatrixStack matrixStack, IRenderTypeBuffer.Impl immediate,
        LightTexture lightmapTextureManager, ActiveRenderInfo camera, float f, CallbackInfo ci
    ) {
        if (PortalRendering.isRendering()) {
            if (RenderStates.getRenderedPortalNum() > 4) {
                ci.cancel();
            }
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/particle/ParticleManager;renderParticles(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer$Impl;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/ActiveRenderInfo;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lcom/mojang/blaze3d/vertex/IVertexBuilder;Lnet/minecraft/client/renderer/ActiveRenderInfo;F)V"
        ),
        require = 0
    )
    private void redirectBuildGeometry(Particle particle, IVertexBuilder vertexConsumer, ActiveRenderInfo camera, float tickDelta) {
        if (((IEParticle) particle).portal_getWorld() == Minecraft.getInstance().world) {
            if (RenderStates.shouldRenderParticle(particle)) {
                particle.renderParticle(vertexConsumer, camera, tickDelta);
            }
        }
    }
    
    // a lava ember particle can generate a smoke particle during ticking
    // avoid generating the particle into the wrong dimension
    @Inject(method = "Lnet/minecraft/client/particle/ParticleManager;tickParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void onTickParticle(Particle particle, CallbackInfo ci) {
        if (((IEParticle) particle).portal_getWorld() != Minecraft.getInstance().world) {
            ci.cancel();
        }
    }
    
    @Override
    public void mySetWorld(ClientWorld world_) {
        world = world_;
    }
    
}
