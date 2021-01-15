package com.qouteall.immersive_portals.mixin.client.gui_portal;

import com.qouteall.immersive_portals.render.GuiPortalRendering;
import net.minecraft.client.MainWindow;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MainWindow.class)
public class MixinWindow {
    @Inject(method = "Lnet/minecraft/client/MainWindow;getFramebufferWidth()I", at = @At("HEAD"), cancellable = true)
    private void onGetFramebufferWidth(CallbackInfoReturnable<Integer> cir) {
        Framebuffer guiPortalRenderingFb = GuiPortalRendering.getRenderingFrameBuffer();
        if (guiPortalRenderingFb != null) {
            cir.setReturnValue(guiPortalRenderingFb.framebufferWidth);
        }
    }
    
    @Inject(method = "Lnet/minecraft/client/MainWindow;getFramebufferHeight()I", at = @At("HEAD"), cancellable = true)
    private void onGetFramebufferHeight(CallbackInfoReturnable<Integer> cir) {
        Framebuffer guiPortalRenderingFb = GuiPortalRendering.getRenderingFrameBuffer();
        if (guiPortalRenderingFb != null) {
            cir.setReturnValue(guiPortalRenderingFb.framebufferHeight);
        }
    }
}
