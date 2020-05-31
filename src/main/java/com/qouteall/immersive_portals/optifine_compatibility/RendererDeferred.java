package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.QueryManager;
import com.qouteall.immersive_portals.render.SecondaryFrameBuffer;
import com.qouteall.immersive_portals.render.ShaderManager;
import com.qouteall.immersive_portals.render.ViewAreaRenderer;
import com.qouteall.immersive_portals.render.context_management.PortalLayers;
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
    private SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();
    private MatrixStack modelView = new MatrixStack();
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    public void onBeforeTranslucentRendering(MatrixStack matrixStack) {
    
    }
    
    @Override
    public void onAfterTranslucentRendering(MatrixStack matrixStack) {
        if (PortalLayers.isRendering()) {
            return;
        }
//        OFHelper.copyFromShaderFbTo(deferredBuffer.fb, GL11.GL_DEPTH_BUFFER_BIT);
        modelView.push();
        modelView.getLast().getMatrix().mul(matrixStack.getLast().getMatrix());
        modelView.getLast().getNormal().mul(matrixStack.getLast().getNormal());
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
        
        OFGlobal.bindToShaderFrameBuffer.run();
        
    }
    
    @Override
    protected void doRenderPortal(Portal portal, MatrixStack matrixStack) {
        if (PortalLayers.isRendering()) {
            //currently only support one-layer portal
            return;
        }
        
        OFHelper.copyFromShaderFbTo(deferredBuffer.fb, GL11.GL_DEPTH_BUFFER_BIT);
        
        if (!testShouldRenderPortal(portal, matrixStack)) {
            return;
        }
    
        PortalLayers.pushPortalLayer(portal);
        
        mustRenderPortalHere(portal);
        //it will bind the gbuffer of rendered dimension
    
        PortalLayers.popPortalLayer();
        
        deferredBuffer.fb.bindFramebuffer(true);
    
//        RenderSystem.disableBlend();
//        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
//        RenderSystem.colorMask(true,true,true,true);
        RenderSystem.disableAlphaTest();
        MyRenderHelper.drawFrameBufferUp(
            portal,
            client.getFramebuffer(),
            CGlobal.shaderManager,
            matrixStack
        );
        
        OFGlobal.bindToShaderFrameBuffer.run();
    }
    
    @Override
    protected void invokeWorldRendering(
        Vec3d newEyePos, Vec3d newLastTickEyePos, ClientWorld newWorld
    ) {
        MyGameRenderer.depictTheFascinatingWorld(
            newWorld, newEyePos,
            newLastTickEyePos,
            runnable -> {
                OFGlobal.shaderContextManager.switchContextAndRun(()->{
                    OFGlobal.bindToShaderFrameBuffer.run();
                    runnable.run();
                });
            }
        );
    }
    
//    @Override
//    protected void renderPortalContentWithContextSwitched(
//        Portal portal, Vec3d oldCameraPos, ClientWorld oldWorld
//    ) {
//        OFGlobal.shaderContextManager.switchContextAndRun(
//            () -> {
//                OFGlobal.bindToShaderFrameBuffer.run();
//                super.renderPortalContentWithContextSwitched(portal, oldCameraPos, oldWorld);
//            }
//        );
//    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
//        if (shouldRenderPortalInEntityRenderer(portal)) {
//            assert false;
//            //ViewAreaRenderer.drawPortalViewTriangle(portal);
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
        if (PortalLayers.isRendering()) {
            return portal.isInFrontOfPortal(cameraPos);
        }
        return false;
    }
    
    //NOTE it will write to shader depth buffer
    private boolean testShouldRenderPortal(Portal portal, MatrixStack matrixStack) {
        return QueryManager.renderAndGetDoesAnySamplePassed(() -> {
            GlStateManager.enableDepthTest();
//            GlStateManager.disableDepthTest();//test
            GlStateManager.disableTexture();
//            GlStateManager.colorMask(false, false, false, false);
            GlStateManager.depthMask(true);
            GL20.glUseProgram(0);
            
            ViewAreaRenderer.drawPortalViewTriangle(
                portal, matrixStack, true, true
            );
            
            GlStateManager.enableTexture();
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.depthMask(true);
        });
    }
    
    @Override
    public void onRenderCenterEnded(MatrixStack matrixStack) {
        if (PortalLayers.isRendering()) {
            return;
        }
        
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, client.getFramebuffer().framebufferObject);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, deferredBuffer.fb.framebufferObject);
        
        GL30.glBlitFramebuffer(
            0, 0, deferredBuffer.fb.framebufferTextureWidth, deferredBuffer.fb.framebufferTextureHeight,
            0, 0, deferredBuffer.fb.framebufferTextureWidth, deferredBuffer.fb.framebufferTextureHeight,
            GL11.GL_COLOR_BUFFER_BIT, GL_NEAREST
        );
        
        CHelper.checkGlError();
        
        renderPortals(modelView);
        modelView.pop();
        
        GlStateManager.enableAlphaTest();
        Framebuffer mainFrameBuffer = client.getFramebuffer();
        mainFrameBuffer.bindFramebuffer(true);
        
        MyRenderHelper.myDrawFrameBuffer(
            deferredBuffer.fb,
            false,
            false
        );
    }
}
