package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ModMainClient;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
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
    private LightTexture lightmapTexture;
    @Shadow
    private boolean renderHand;
    @Shadow
    @Final
    @Mutable
    private ActiveRenderInfo activeRender;
    
    @Shadow
    @Final
    private Minecraft mc;
    
    @Shadow
    private int rendererUpdateCount;
    
    @Shadow
    private boolean debugView;
    
    @Inject(method = "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJZ)V", at = @At("HEAD"))
    private void onFarBeforeRendering(
        float partialTicks,
        long nanoTime,
        boolean renderWorldIn,
        CallbackInfo ci
    ) {
        MyRenderHelper.updatePreRenderInfo(partialTicks);
        ModMain.preRenderSignal.emit();
        if (CGlobal.earlyClientLightUpdate) {
            MyRenderHelper.earlyUpdateLight();
        }
    }
    
    //before rendering world (not triggered when rendering portal)
    @Inject(
        method = "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderWorld(FJLcom/mojang/blaze3d/matrix/MatrixStack;)V"
        )
    )
    private void onBeforeRenderingCenter(
        float float_1,
        long long_1,
        boolean boolean_1,
        CallbackInfo ci
    ) {
        ModMainClient.switchToCorrectRenderer();
    
        CGlobal.renderer.prepareRendering();
    }
    
    //after rendering world (not triggered when rendering portal)
    @Inject(
        method = "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderWorld(FJLcom/mojang/blaze3d/matrix/MatrixStack;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterRenderingCenter(
        float float_1,
        long long_1,
        boolean boolean_1,
        CallbackInfo ci
    ) {
        CGlobal.renderer.finishRendering();
    
        MyRenderHelper.onTotalRenderEnd();
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/GameRenderer;renderWorld(FJLcom/mojang/blaze3d/matrix/MatrixStack;)V", at = @At("TAIL"))
    private void onRenderCenterEnded(
        float float_1,
        long long_1,
        MatrixStack matrixStack_1,
        CallbackInfo ci
    ) {
        CGlobal.renderer.onRenderCenterEnded(matrixStack_1);
    }
    
    //resize all world renderers when resizing window
    @Inject(method = "Lnet/minecraft/client/renderer/GameRenderer;updateShaderGroupSize(II)V", at = @At("RETURN"))
    private void onOnResized(int int_1, int int_2, CallbackInfo ci) {
        if (CGlobal.clientWorldLoader != null) {
            CGlobal.clientWorldLoader.worldRendererMap.values().stream()
                .filter(
                    worldRenderer -> worldRenderer != mc.worldRenderer
                )
                .forEach(
                    worldRenderer -> worldRenderer.createBindEntityOutlineFbs(int_1, int_2)
                );
        }
    }
    
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
    
    @Override
    public void setLightmapTextureManager(LightTexture manager) {
        lightmapTexture = manager;
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
    
    @Override
    public void setIsRenderingPanorama(boolean cond) {
        debugView = cond;
    }
}
