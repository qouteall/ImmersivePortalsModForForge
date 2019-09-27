package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.exposer.IEGameRenderer;
import com.qouteall.immersive_portals.exposer.IEWorldRenderer;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.optifine_compatibility.OFGlobal;
import com.qouteall.immersive_portals.optifine_compatibility.OFHelper;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.util.Untracker;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.dimension.DimensionType;
import net.optifine.shaders.Shaders;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;

public class RenderHelper {
    public static DimensionType originalPlayerDimension;
    public static Vec3d originalPlayerPos;
    public static Vec3d originalPlayerLastTickPos;
    public static GameType originalGameMode;
    public static float partialTicks = 0;
    
    private static Set<DimensionType> renderedDimensions = new HashSet<>();
    public static List<List<WeakReference<Portal>>> lastPortalRenderInfos = new ArrayList<>();
    private static List<List<WeakReference<Portal>>> portalRenderInfos = new ArrayList<>();
    
    public static Vec3d lastCameraPos = Vec3d.ZERO;
    public static Vec3d cameraPosDelta = Vec3d.ZERO;
    
    public static void onTotalRenderBegin(
        float partialTicks_
    ) {
        Entity cameraEntity = Minecraft.getInstance().renderViewEntity;
        
        if (cameraEntity == null) {
            return;
        }
        
        RenderHelper.originalPlayerDimension = cameraEntity.dimension;
        RenderHelper.originalPlayerPos = cameraEntity.getPositionVec();
        RenderHelper.originalPlayerLastTickPos = Helper.lastTickPosOf(cameraEntity);
        NetworkPlayerInfo entry = CHelper.getClientPlayerListEntry();
        RenderHelper.originalGameMode = entry != null ? entry.getGameMode() : GameType.CREATIVE;
        partialTicks = partialTicks_;
        
        renderedDimensions.clear();
        lastPortalRenderInfos = portalRenderInfos;
        portalRenderInfos = new ArrayList<>();
    }
    
    public static void onTotalRenderEnd() {
        Minecraft mc = Minecraft.getInstance();
        CGlobal.clientWorldLoader
            .getDimensionRenderHelper(mc.world.dimension.getType())
            .switchToMe();
    
        if (getRenderedPortalNum() != 0) {
            //recover chunk renderer dispatcher
            ((IEWorldRenderer) mc.worldRenderer).getChunkRenderDispatcher().updateCameraPosition(
                mc.renderViewEntity.posX,
                mc.renderViewEntity.posZ
            );
        }
        
        Vec3d currCameraPos = mc.gameRenderer.getCamera().getPositionVec();
        cameraPosDelta = currCameraPos.subtract(lastCameraPos);
        if (cameraPosDelta.lengthSquared() > 1) {
            cameraPosDelta = Vec3d.ZERO;
        }
        lastCameraPos = currCameraPos;
    }
    
    public static int getRenderedPortalNum() {
        return portalRenderInfos.size();
    }
    
    public static boolean isDimensionRendered(DimensionType dimensionType) {
        return renderedDimensions.contains(dimensionType);
    }
    
    public static void onBeginPortalWorldRendering(Stack<Portal> portalLayers) {
        List<WeakReference<Portal>> currRenderInfo = portalLayers.stream().map(
            (Function<Portal, WeakReference<Portal>>) WeakReference::new
        ).collect(Collectors.toList());
        portalRenderInfos.add(currRenderInfo);
        renderedDimensions.add(portalLayers.peek().dimensionTo);
    }
    
    public static void setupCameraTransformation() {
        ((IEGameRenderer) PortalRenderer.mc.gameRenderer).applyCameraTransformations_(partialTicks);
        ActiveRenderInfo camera = PortalRenderer.mc.gameRenderer.getCamera();
        camera.update(
            PortalRenderer.mc.world,
            (Entity) (PortalRenderer.mc.getCameraEntity() == null ? PortalRenderer.mc.player : PortalRenderer.mc.getCameraEntity()),
            PortalRenderer.mc.gameSettings.thirdPersonView > 0,
            PortalRenderer.mc.gameSettings.thirdPersonView == 2,
            partialTicks
        );
        
    }
    
    public static void drawFrameBufferUp(
        Portal portal,
        Framebuffer textureProvider,
        ShaderManager shaderManager
    ) {
        setupCameraTransformation();
        
        shaderManager.loadContentShaderAndShaderVars(0);
        
        if (OFHelper.getIsUsingShader()) {
            GlStateManager.viewport(
                0,
                0,
                PortalRenderer.mc.getFramebuffer().framebufferWidth,
                PortalRenderer.mc.getFramebuffer().framebufferHeight
            );
        }
        
        GlStateManager.enableTexture();
        
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
        
        GlStateManager.bindTexture(textureProvider.framebufferTexture);
        GlStateManager.texParameter(3553, 10241, 9729);
        GlStateManager.texParameter(3553, 10240, 9729);
        GlStateManager.texParameter(3553, 10242, 10496);
        GlStateManager.texParameter(3553, 10243, 10496);
    
        ViewAreaRenderer.drawPortalViewTriangle(portal);
        
        shaderManager.unloadShader();
        
        if (OFHelper.getIsUsingShader()) {
            GlStateManager.viewport(0, 0, Shaders.renderWidth, Shaders.renderHeight);
        }
    }
    
    static void renderScreenTriangle() {
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        
        GlStateManager.disableAlphaTest();
        GlStateManager.disableTexture();
        
        GlStateManager.shadeModel(GL_SMOOTH);
        
        GL20.glUseProgram(0);
        GL11.glDisable(GL_CLIP_PLANE0);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBufferBuilder();
        bufferbuilder.begin(GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        
        bufferbuilder.vertex(1, -1, 0).color(255, 255, 255, 255)
            .next();
        bufferbuilder.vertex(1, 1, 0).color(255, 255, 255, 255)
            .next();
        bufferbuilder.vertex(-1, 1, 0).color(255, 255, 255, 255)
            .next();
        
        bufferbuilder.vertex(-1, 1, 0).color(255, 255, 255, 255)
            .next();
        bufferbuilder.vertex(-1, -1, 0).color(255, 255, 255, 255)
            .next();
        bufferbuilder.vertex(1, -1, 0).color(255, 255, 255, 255)
            .next();
        
        tessellator.draw();
        
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.popMatrix();
        
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.popMatrix();
        
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
    }
    
    public static void copyFromShaderFbTo(Framebuffer destFb, int copyComponent) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, OFGlobal.getDfb.get());
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destFb.framebufferObject);
        
        GL30.glBlitFramebuffer(
            0, 0, Shaders.renderWidth, Shaders.renderHeight,
            0, 0, destFb.framebufferWidth, destFb.framebufferHeight,
            copyComponent, GL_NEAREST
        );
        
        OFHelper.bindToShaderFrameBuffer();
    }
    
    /**
     * {@link GlFramebuffer#draw(int, int)}
     */
    public static void myDrawFrameBuffer(
        Framebuffer textureProvider,
        boolean doEnableAlphaTest,
        boolean doEnableModifyAlpha
    ) {
        Validate.isTrue(GLX.isUsingFBOs());
        
        if (doEnableModifyAlpha) {
            GlStateManager.colorMask(true, true, true, true);
        }
        else {
            GlStateManager.colorMask(true, true, true, false);
        }
        GlStateManager.disableDepthTest();
        GlStateManager.depthMask(false);
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(
            0.0D,
            (double) textureProvider.framebufferWidth,
            (double) textureProvider.framebufferHeight,
            0.0D,
            1000.0D,
            3000.0D
        );
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        GlStateManager.translatef(0.0F, 0.0F, -2000.0F);
        GlStateManager.viewport(0, 0, textureProvider.framebufferWidth, textureProvider.framebufferHeight);
        GlStateManager.enableTexture();
        GlStateManager.disableLighting();
        if (doEnableAlphaTest) {
            GlStateManager.enableAlphaTest();
        }
        else {
            GlStateManager.disableAlphaTest();
        }
        
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        textureProvider.beginRead();
        float float_1 = (float) textureProvider.framebufferWidth;
        float float_2 = (float) textureProvider.framebufferHeight;
        float float_3 = (float) textureProvider.framebufferWidth / (float) textureProvider.framebufferTextureWidth;
        float float_4 = (float) textureProvider.framebufferHeight / (float) textureProvider.framebufferTextureHeight;
        Tessellator tessellator_1 = Tessellator.getInstance();
        BufferBuilder bufferBuilder_1 = tessellator_1.getBufferBuilder();
        bufferBuilder_1.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        bufferBuilder_1.vertex(0.0D, (double) float_2, 0.0D).texture(0.0D, 0.0D).color(
            255,
            255,
            255,
            255
        ).next();
        bufferBuilder_1.vertex((double) float_1, (double) float_2, 0.0D).texture(
            (double) float_3,
            0.0D
        ).color(255, 255, 255, 255).next();
        bufferBuilder_1.vertex((double) float_1, 0.0D, 0.0D).texture(
            (double) float_3,
            (double) float_4
        ).color(255, 255, 255, 255).next();
        bufferBuilder_1.vertex(0.0D, 0.0D, 0.0D).texture(0.0D, (double) float_4).color(
            255,
            255,
            255,
            255
        ).next();
        tessellator_1.draw();
        textureProvider.endRead();
        GlStateManager.depthMask(true);
        GlStateManager.colorMask(true, true, true, true);
    }
    
    //If I don't do so JVM will crash
    private static final FloatBuffer matrixBuffer = (FloatBuffer) GLX.make(MemoryUtil.memAllocFloat(
        16), (p_209238_0_) -> {
        Untracker.untrack(MemoryUtil.memAddress(p_209238_0_));
    });
    
    public static void multMatrix(float[] arr) {
        matrixBuffer.put(arr);
        matrixBuffer.rewind();
        GlStateManager.multMatrix(matrixBuffer);
    }
    
    public static boolean isRenderingMirror() {
        return CGlobal.renderer.isRendering() &&
            CGlobal.renderer.getRenderingPortal() instanceof Mirror;
    }
    
    public static void setupTransformationForMirror(ActiveRenderInfo camera) {
        if (CGlobal.renderer.isRendering()) {
            Portal renderingPortal = CGlobal.renderer.getRenderingPortal();
            if (renderingPortal instanceof Mirror) {
                Mirror mirror = (Mirror) renderingPortal;
                Vec3d relativePos = mirror.getPositionVec().subtract(camera.getPositionVec());
                
                GlStateManager.translated(relativePos.x, relativePos.y, relativePos.z);
                
                float[] arr = getMirrorTransformation(mirror.getNormal());
                multMatrix(arr);
                
                GlStateManager.translated(-relativePos.x, -relativePos.y, -relativePos.z);
                
                GlStateManager.cullFace(GlStateManager.CullFace.FRONT);
            }
            else {
                GlStateManager.cullFace(GlStateManager.CullFace.BACK);
            }
        }
        else {
            GlStateManager.cullFace(GlStateManager.CullFace.BACK);
        }
    }
    
    
    //https://en.wikipedia.org/wiki/Householder_transformation
    private static float[] getMirrorTransformation(
        Vec3d mirrorNormal
    ) {
        Vec3d normal = mirrorNormal.normalize();
        float x = (float) normal.x;
        float y = (float) normal.y;
        float z = (float) normal.z;
        return new float[]{
            1 - 2 * x * x, 0 - 2 * x * y, 0 - 2 * x * z, 0,
            0 - 2 * y * x, 1 - 2 * y * y, 0 - 2 * y * z, 0,
            0 - 2 * z * x, 0 - 2 * z * y, 1 - 2 * z * z, 0,
            0, 0, 0, 1
        };
    }
}
