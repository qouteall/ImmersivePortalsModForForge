package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.GL_NEAREST;

public class RendererDeferred extends PortalRenderer {
    SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    public void onBeforeTranslucentRendering(MatrixStack matrixStack) {
    
    }
    
    @Override
    public void onAfterTranslucentRendering(MatrixStack matrixStack) {
        if (isRendering()) {
            return;
        }
        OFHelper.copyFromShaderFbTo(deferredBuffer.fb, GL11.GL_DEPTH_BUFFER_BIT);
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    @Override
    public void prepareRendering() {
        if (CGlobal.shaderManager == null) {
            CGlobal.shaderManager = new ShaderManager();
        }
        
        deferredBuffer.prepare();
        
        deferredBuffer.fb.setFramebufferColor(1, 0, 0, 0);
        deferredBuffer.fb.framebufferClear(Minecraft.IS_RUNNING_ON_MAC);
        
        OFInterface.bindToShaderFrameBuffer.run();
        
    }
    
    @Override
    protected void doRenderPortal(Portal portal, MatrixStack matrixStack) {
        if (isRendering()) {
            //currently only support one-layer portal
            return;
        }
        
        if (!testShouldRenderPortal(portal)) {
            return;
        }
        
        portalLayers.push(portal);
        
        manageCameraAndRenderPortalContent(portal);
        //it will bind the gbuffer of rendered dimension
        
        portalLayers.pop();
        
        deferredBuffer.fb.bindFramebuffer(true);
        
        MyRenderHelper.drawFrameBufferUp(
            portal,
            mc.getFramebuffer(),
            CGlobal.shaderManager,
            matrixStack
        );
        
        OFInterface.bindToShaderFrameBuffer.run();
    }
    
    @Override
    protected void renderPortalContentWithContextSwitched(
        Portal portal, Vec3d oldCameraPos, ClientWorld oldWorld
    ) {
        OFGlobal.shaderContextManager.switchContextAndRun(
            () -> {
                OFInterface.bindToShaderFrameBuffer.run();
                super.renderPortalContentWithContextSwitched(portal, oldCameraPos, oldWorld);
            }
        );
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
//        if (shouldRenderPortalInEntityRenderer(portal)) {
//            ViewAreaRenderer.drawPortalViewTriangle(portal);
//        }
    }
    
    private boolean shouldRenderPortalInEntityRenderer(Portal portal) {
        Entity cameraEntity = Minecraft.getInstance().renderViewEntity;
        if (cameraEntity == null) {
            return false;
        }
        Vec3d cameraPos = cameraEntity.getPositionVec();
        if (Shaders.isShadowPass) {
            return true;
        }
        if (isRendering()) {
            return portal.isInFrontOfPortal(cameraPos);
        }
        return false;
    }
    
    //NOTE it will write to shader depth buffer
    private boolean testShouldRenderPortal(Portal portal) {
        return QueryManager.renderAndGetDoesAnySamplePassed(() -> {
            GlStateManager.enableDepthTest();
            GlStateManager.disableTexture();
            GlStateManager.colorMask(false, false, false, false);
            GlStateManager.depthMask(false);
            assert false;
            //MyRenderHelper.setupCameraTransformation();
            GL20.glUseProgram(0);
    
            //ViewAreaRenderer.drawPortalViewTriangle(portal);
    
            GlStateManager.enableTexture();
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.depthMask(true);
        });
    }
    
    @Override
    public void onRenderCenterEnded(MatrixStack matrixStack) {
        if (isRendering()) {
            return;
        }
        
        //OFHelper.copyFromShaderFbTo(deferredBuffer.fb, GL11.GL_COLOR_BUFFER_BIT);
        
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mc.getFramebuffer().framebufferObject);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, deferredBuffer.fb.framebufferObject);
        
        GL30.glBlitFramebuffer(
            0, 0, deferredBuffer.fb.framebufferWidth, deferredBuffer.fb.framebufferHeight,
            0, 0, deferredBuffer.fb.framebufferWidth, deferredBuffer.fb.framebufferHeight,
            GL11.GL_COLOR_BUFFER_BIT, GL_NEAREST
        );
    
        CHelper.checkGlError();
        
        renderPortals(matrixStack);

//        if (MyRenderHelper.getRenderedPortalNum() == 0) {
//            return;
//        }
    
        GlStateManager.enableAlphaTest();
        Framebuffer mainFrameBuffer = mc.getFramebuffer();
        mainFrameBuffer.bindFramebuffer(true);
    
        MyRenderHelper.myDrawFrameBuffer(
            deferredBuffer.fb,
            false,
            false
        );
    }
}
