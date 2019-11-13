package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ModMainClient;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.render.MyRenderHelper;
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

@Mixin(value = GameRenderer.class)
public abstract class MixinGameRenderer implements IEGameRenderer {
    @Shadow
    @Final
    @Mutable
    private LightTexture lightmapTexture;
    @Shadow
    @Final
    @Mutable
    private FogRenderer fogRenderer;
    @Shadow
    private boolean renderHand;
    @Shadow
    @Final
    @Mutable
    private ActiveRenderInfo activeRender;
    
    @Shadow
    public abstract void updateCameraAndRender(float float_1, long long_1);
    
    @Override
    public void renderCenter_(float partialTicks, long finishTimeNano) {
        updateCameraAndRender(partialTicks, finishTimeNano);
    }
    
    //render() and renderCenter() in yarn both correspond to updateCameraAndRender in mcp
    //easy to get confused
    
    @Inject(
        method = "updateCameraAndRender(FJ)V",
        at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderEntities(Lnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/culling/ICamera;F)V"
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
        method = "updateCameraAndRender(FJ)V",
        at = @At(
            value = "INVOKE_STRING",
            target = "Lnet/minecraft/profiler/IProfiler;endStartSection(Ljava/lang/String;)V",
            args = {"ldc=hand"}
        )
    )
    private void beforeRenderingHand(float float_1, long long_1, CallbackInfo ci) {
        if (Minecraft.getInstance().renderViewEntity != null) {
            CGlobal.renderer.onAfterTranslucentRendering();
        }
    }
    
    @Redirect(
        method = "updateCameraAndRender(FJ)V",
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
            target = "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJ)V"
        )
    )
    private void onBeforeRenderingCenter(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        ModMainClient.switchToCorrectRenderer();
        ModMain.preRenderSignal.emit();
        MyRenderHelper.onTotalRenderBegin(partialTicks);
        CGlobal.renderer.prepareRendering();
    }
    
    //after rendering world (not triggered when rendering portal)
    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJ)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterRenderingCenter(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        CGlobal.renderer.finishRendering();
    
        MyRenderHelper.onTotalRenderEnd();
    }
    
    @Inject(method = "updateCameraAndRender(FJ)V", at = @At("TAIL"))
    private void onRenderCenterEnded(float partialTicks, long nanoTime, CallbackInfo ci) {
        CGlobal.renderer.onRenderCenterEnded();
    }
    
    @Shadow
    abstract protected void setupCameraTransform(float float_1);
    
    @Override
    public void applyCameraTransformations_(float float_1) {
        setupCameraTransform(float_1);
    }
    
    @Override
    public LightTexture getLightmapTextureManager() {
        return lightmapTexture;
    }
    
    @Override
    public void setLightmapTextureManager(LightTexture manager) {
        lightmapTexture = manager;
    }
    
    @Override
    public FogRenderer getBackgroundRenderer() {
        return fogRenderer;
    }
    
    @Override
    public void setBackgroundRenderer(FogRenderer renderer) {
        fogRenderer = renderer;
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
        activeRender = camera_;
    }
}
