package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEMatrix4f;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Matrix3f;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.util.LWJGLMemoryUntracker;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;

public class MyRenderHelper {
    //switching context is really bug-prone
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
    
    public static void updatePreRenderInfo(
        float partialTicks_
    ) {
        Entity cameraEntity = Minecraft.getInstance().renderViewEntity;
    
        if (cameraEntity == null) {
            return;
        }
    
        MyRenderHelper.originalPlayerDimension = cameraEntity.dimension;
        MyRenderHelper.originalPlayerPos = cameraEntity.getPositionVec();
        MyRenderHelper.originalPlayerLastTickPos = McHelper.lastTickPosOf(cameraEntity);
        NetworkPlayerInfo entry = CHelper.getClientPlayerListEntry();
        MyRenderHelper.originalGameMode = entry != null ? entry.getGameType() : GameType.CREATIVE;
        partialTicks = partialTicks_;
    
        renderedDimensions.clear();
        lastPortalRenderInfos = portalRenderInfos;
        portalRenderInfos = new ArrayList<>();
    
        FogRendererContext.update();
    }
    
    public static void onTotalRenderEnd() {
        Minecraft mc = Minecraft.getInstance();
        IEGameRenderer gameRenderer = (IEGameRenderer) Minecraft.getInstance().gameRenderer;
        gameRenderer.setLightmapTextureManager(CGlobal.clientWorldLoader
            .getDimensionRenderHelper(mc.world.dimension.getType()).lightmapTexture);
    
        if (getRenderedPortalNum() != 0) {
            //recover chunk renderer dispatcher
            ((IEWorldRenderer) mc.worldRenderer).getBuiltChunkStorage().updateChunkPositions(
                mc.renderViewEntity.getPosX(),
                mc.renderViewEntity.getPosZ()
            );
        }
    
        Vec3d currCameraPos = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
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
        
        CHelper.checkGlError();
    }
    
    public static void restoreViewPort() {
        Minecraft mc = Minecraft.getInstance();
        GlStateManager.viewport(
            0,
            0,
            mc.getMainWindow().getFramebufferWidth(),
            mc.getMainWindow().getFramebufferHeight()
        );
    }
    
    public static void drawFrameBufferUp(
        Portal portal,
        Framebuffer textureProvider,
        ShaderManager shaderManager,
        MatrixStack matrixStack
    ) {
        CHelper.checkGlError();
        McHelper.runWithTransformation(
            matrixStack,
            () -> {
                shaderManager.loadContentShaderAndShaderVars(0);
                
                if (OFInterface.isShaders.getAsBoolean()) {
                    GlStateManager.viewport(
                        0,
                        0,
                        PortalRenderer.mc.getFramebuffer().framebufferWidth,
                        PortalRenderer.mc.getFramebuffer().framebufferHeight
                    );
                }
                
                GlStateManager.enableTexture();
                GlStateManager.activeTexture(GL13.GL_TEXTURE0);
                
                GlStateManager.bindTexture(textureProvider.framebufferTexture);
                GlStateManager.texParameter(3553, 10241, 9729);
                GlStateManager.texParameter(3553, 10240, 9729);
                GlStateManager.texParameter(3553, 10242, 10496);
                GlStateManager.texParameter(3553, 10243, 10496);
                
                ViewAreaRenderer.drawPortalViewTriangle(portal, matrixStack);
                
                shaderManager.unloadShader();
                
                OFInterface.resetViewport.run();
            }
        );
        CHelper.checkGlError();
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
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
    
        bufferbuilder.pos(1, -1, 0).color(255, 255, 255, 255)
            .endVertex();
        bufferbuilder.pos(1, 1, 0).color(255, 255, 255, 255)
            .endVertex();
        bufferbuilder.pos(-1, 1, 0).color(255, 255, 255, 255)
            .endVertex();
    
        bufferbuilder.pos(-1, 1, 0).color(255, 255, 255, 255)
            .endVertex();
        bufferbuilder.pos(-1, -1, 0).color(255, 255, 255, 255)
            .endVertex();
        bufferbuilder.pos(1, -1, 0).color(255, 255, 255, 255)
            .endVertex();
        
        tessellator.draw();
        
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.popMatrix();
        
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.popMatrix();
        
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
    }
    
    /**
     * {@link Framebuffer#draw(int, int)}
     */
    public static void myDrawFrameBuffer(
        Framebuffer textureProvider,
        boolean doEnableAlphaTest,
        boolean doEnableModifyAlpha
    ) {
        CHelper.checkGlError();
    
        int int_1 = textureProvider.framebufferWidth;
        int int_2 = textureProvider.framebufferHeight;
    
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
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
        GlStateManager.ortho(0.0D, (double) int_1, (double) int_2, 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        GlStateManager.translatef(0.0F, 0.0F, -2000.0F);
        GlStateManager.viewport(0, 0, int_1, int_2);
        GlStateManager.enableTexture();
        GlStateManager.disableLighting();
        if (doEnableAlphaTest) {
            RenderSystem.enableAlphaTest();
        }
        else {
            GlStateManager.disableAlphaTest();
        }
        GlStateManager.enableBlend();
        GlStateManager.disableColorMaterial();
    
    
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        textureProvider.bindFramebufferTexture();
        float float_1 = (float) int_1;
        float float_2 = (float) int_2;
        float float_3 = (float) textureProvider.framebufferWidth / (float) textureProvider.framebufferTextureWidth;
        float float_4 = (float) textureProvider.framebufferHeight / (float) textureProvider.framebufferTextureHeight;
        Tessellator tessellator_1 = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder_1 = tessellator_1.getBuffer();
        bufferBuilder_1.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        bufferBuilder_1.pos(0.0D, (double) float_2, 0.0D).tex(0.0F, 0.0F).color(
            255,
            255,
            255,
            255
        ).endVertex();
        bufferBuilder_1.pos((double) float_1, (double) float_2, 0.0D).tex(
            float_3,
            0.0F
        ).color(255, 255, 255, 255).endVertex();
        bufferBuilder_1.pos((double) float_1, 0.0D, 0.0D).tex(float_3, float_4).color(
            255,
            255,
            255,
            255
        ).endVertex();
        bufferBuilder_1.pos(0.0D, 0.0D, 0.0D).tex(0.0F, float_4).color(
            255,
            255,
            255,
            255
        ).endVertex();
        tessellator_1.draw();
        textureProvider.unbindFramebufferTexture();
        GlStateManager.depthMask(true);
        GlStateManager.colorMask(true, true, true, true);
    
        CHelper.checkGlError();
    }
    
    //If I don't do so JVM will crash
    private static final FloatBuffer matrixBuffer = (FloatBuffer) GLX.make(MemoryUtil.memAllocFloat(
        16), (p_209238_0_) -> {
        LWJGLMemoryUntracker.untrack(MemoryUtil.memAddress(p_209238_0_));
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
    
    public static void setupTransformationForMirror(
        ActiveRenderInfo camera,
        MatrixStack matrixStack
    ) {
        if (CGlobal.renderer.isRendering()) {
            Portal renderingPortal = CGlobal.renderer.getRenderingPortal();
            if (renderingPortal instanceof Mirror) {
                Mirror mirror = (Mirror) renderingPortal;
                Vec3d relativePos = mirror.getPositionVec().subtract(camera.getProjectedView());
                
                matrixStack.translate(relativePos.x, relativePos.y, relativePos.z);
                
                float[] arr = getMirrorTransformation(mirror.getNormal());
                Matrix4f matrix = new Matrix4f();
                ((IEMatrix4f) (Object) matrix).loadFromArray(arr);
                matrixStack.getLast().getPositionMatrix().multiply(matrix);
                matrixStack.getLast().getNormalMatrix().mul(new Matrix3f(matrix));
                
                matrixStack.translate(-relativePos.x, -relativePos.y, -relativePos.z);
            }
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
