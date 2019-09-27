package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ModMainClient;
import com.qouteall.immersive_portals.exposer.IEGameRenderer;
import com.qouteall.immersive_portals.render.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements IEGameRenderer {
    @Shadow
    @Final
    @Mutable
    private LightTexture lightmapTextureManager;
    @Shadow
    @Final
    @Mutable
    private FogRenderer backgroundRenderer;
    @Shadow
    private boolean renderHand;
    @Shadow
    @Final
    @Mutable
    private ActiveRenderInfo camera;
    
    @Shadow
    public abstract void renderCenter(float float_1, long long_1);
    
    @Override
    public void renderCenter_(float partialTicks, long finishTimeNano) {
        renderCenter(partialTicks, finishTimeNano);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/render/GameRenderer;renderCenter(FJ)V",
        at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/render/WorldRenderer;renderEntities(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/VisibleRegion;F)V"
        )
    )
    private void afterRenderingEntities(
        float partialTicks,
        long finishTimeNano,
        CallbackInfo ci
    ) {
        if (Minecraft.getInstance().renderViewEntity != null) {
            CGlobal.renderer.onBeforeTranslucentRendering();
        }
    }
    
    @Inject(
        method = "renderCenter",
        at = @At(
            value = "INVOKE_STRING",
            target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V",
            args = {"ldc=hand"}
        )
    )
    private void beforeRenderingHand(float float_1, long long_1, CallbackInfo ci) {
        if (Minecraft.getInstance().renderViewEntity != null) {
            CGlobal.renderer.onAfterTranslucentRendering();
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/render/GameRenderer;renderCenter(FJ)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;clear(IZ)V"
        )
    )
    private void redirectClearing(int int_1, boolean boolean_1) {
        if (!CGlobal.renderer.shouldSkipClearing()) {
            GlStateManager.clear(int_1, boolean_1);
        }
    }
    
    //before rendering world (not triggered when rendering portal)
    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderCenter(FJ)V"
        )
    )
    private void onBeforeRenderingCenter(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        ModMainClient.switchToCorrectRenderer();
        ModMain.preRenderSignal.emit();
        RenderHelper.onTotalRenderBegin(partialTicks);
        CGlobal.renderer.prepareRendering();
    }
    
    //after rendering world (not triggered when rendering portal)
    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderCenter(FJ)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterRenderingCenter(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        CGlobal.renderer.finishRendering();
    
        RenderHelper.onTotalRenderEnd();
    }
    
    @Inject(method = "renderCenter", at = @At("TAIL"))
    private void onRenderCenterEnded(float partialTicks, long nanoTime, CallbackInfo ci) {
        CGlobal.renderer.onRenderCenterEnded();
    }
    
    @Shadow
    abstract protected void applyCameraTransformations(float float_1);
    
    @Override
    public void applyCameraTransformations_(float float_1) {
        applyCameraTransformations(float_1);
    }
    
    @Override
    public LightTexture getLightmapTextureManager() {
        return lightmapTextureManager;
    }
    
    @Override
    public void setLightmapTextureManager(LightTexture manager) {
        lightmapTextureManager = manager;
    }
    
    @Override
    public FogRenderer getBackgroundRenderer() {
        return backgroundRenderer;
    }
    
    @Override
    public void setBackgroundRenderer(FogRenderer renderer) {
        backgroundRenderer = renderer;
    }
    
    @Override
    public boolean getDoRenderHand() {
        return renderHand;
    }
    
    @Override
    public void setDoRenderHand(boolean e) {
        renderHand = e;
    }
    
    @Override
    public void setCamera(ActiveRenderInfo camera_) {
        camera = camera_;
    }
}
