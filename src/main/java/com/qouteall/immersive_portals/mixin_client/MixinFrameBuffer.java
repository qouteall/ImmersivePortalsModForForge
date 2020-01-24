package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.platform.GLX;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEGlFrameBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Framebuffer.class)
public abstract class MixinFrameBuffer implements IEGlFrameBuffer {
    
    private boolean isStencilBufferEnabled;
    
    @Shadow
    public int framebufferTextureWidth;
    @Shadow
    public int framebufferTextureHeight;
    
    @Shadow
    public abstract void func_216492_b(int int_1, int int_2, boolean boolean_1);
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(
        int int_1,
        int int_2,
        boolean boolean_1,
        boolean boolean_2,
        CallbackInfo ci
    ) {
        isStencilBufferEnabled = false;
    }
    
    @Inject(
        method = "func_216492_b",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GLX;glBindRenderbuffer(II)V"),
        cancellable = true
    )
    private void onInitFrameBuffer(int int_1, int int_2, boolean isMac, CallbackInfo ci) {
        if (isStencilBufferEnabled) {
            Framebuffer this_ = (Framebuffer) (Object) this;
        
            GLX.glBindRenderbuffer(GLX.GL_RENDERBUFFER, this_.depthBuffer);
            GLX.glRenderbufferStorage(
                GLX.GL_RENDERBUFFER,
                org.lwjgl.opengl.EXTPackedDepthStencil.GL_DEPTH24_STENCIL8_EXT,
                this_.framebufferTextureWidth,
                this_.framebufferTextureHeight
            );
            GLX.glFramebufferRenderbuffer(
                GLX.GL_FRAMEBUFFER,
                org.lwjgl.opengl.EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
                GLX.GL_RENDERBUFFER,
                this_.depthBuffer
            );
            GLX.glFramebufferRenderbuffer(
                GLX.GL_FRAMEBUFFER,
                org.lwjgl.opengl.EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT,
                GLX.GL_RENDERBUFFER,
                this_.depthBuffer
            );
    
            this_.checkFramebufferComplete();
            this_.framebufferClear(isMac);
            this_.unbindFramebuffer();
    
            CHelper.checkGlError();
    
            Helper.log("Frame Buffer Reloaded with Stencil Buffer");
    
            ci.cancel();
        }
    }
    
    @Override
    public boolean getIsStencilBufferEnabled() {
        return isStencilBufferEnabled;
    }
    
    @Override
    public void setIsStencilBufferEnabledAndReload(boolean cond) {
        if (isStencilBufferEnabled != cond) {
            isStencilBufferEnabled = cond;
            func_216492_b(
                framebufferTextureWidth,
                framebufferTextureHeight,
                Minecraft.IS_RUNNING_ON_MAC
            );
        }
    }
}
