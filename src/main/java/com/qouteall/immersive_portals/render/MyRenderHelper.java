package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameType;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_CLIP_PLANE0;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_SMOOTH;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glCullFace;

public class MyRenderHelper {
    
    public static final Minecraft client = Minecraft.getInstance();
    
    public static DimensionType originalPlayerDimension;
    public static Vec3d originalPlayerPos;
    public static Vec3d originalPlayerLastTickPos;
    public static GameType originalGameMode;
    public static float tickDelta = 0;
    
    private static Set<DimensionType> renderedDimensions = new HashSet<>();
    public static List<List<WeakReference<Portal>>> lastPortalRenderInfos = new ArrayList<>();
    private static List<List<WeakReference<Portal>>> portalRenderInfos = new ArrayList<>();
    
    public static Vec3d lastCameraPos = Vec3d.ZERO;
    public static Vec3d cameraPosDelta = Vec3d.ZERO;
    
    public static boolean shouldForceDisableCull = false;
    public static long renderStartNanoTime;
    public static double viewBobFactor;
    
    //null indicates not gathered
    public static Matrix4f projectionMatrix;
    
    public static ActiveRenderInfo originalCamera;
    public static int originalCameraLightPacked;
    
    public static String debugText;
    
    public static boolean isLaggy = false;
    
    public static boolean isRenderingEntities = false;
    
    public static void updatePreRenderInfo(
        float tickDelta_
    ) {
        
        Entity cameraEntity = client.renderViewEntity;
        
        if (cameraEntity == null) {
            return;
        }
        
        MyRenderHelper.originalPlayerDimension = cameraEntity.dimension;
        MyRenderHelper.originalPlayerPos = cameraEntity.getPositionVec();
        MyRenderHelper.originalPlayerLastTickPos = McHelper.lastTickPosOf(cameraEntity);
        NetworkPlayerInfo entry = CHelper.getClientPlayerListEntry();
        MyRenderHelper.originalGameMode = entry != null ? entry.getGameType() : GameType.CREATIVE;
        tickDelta = tickDelta_;
        
        renderedDimensions.clear();
        lastPortalRenderInfos = portalRenderInfos;
        portalRenderInfos = new ArrayList<>();
        
        FogRendererContext.update();
        
        renderStartNanoTime = System.nanoTime();
        
        updateViewBobbingFactor(cameraEntity);
        
        projectionMatrix = null;
        originalCamera = client.gameRenderer.getActiveRenderInfo();
        
        originalCameraLightPacked = client.getRenderManager()
            .getPackedLight(client.renderViewEntity, tickDelta);
        
        updateIsLaggy();
        
        debugText = "";
//        MyRenderHelper.debugText = String.valueOf(((IEEntity) client.player).getCollidingPortal());
    
//        if (ClientTeleportationManager.isTeleportingTick) {
//            Helper.log("frame "+tickDelta_);
//        }
    }
    
    //protect the player from mirror room lag attack
    private static void updateIsLaggy() {
        if (!Global.lagAttackProof) {
            isLaggy = false;
            return;
        }
        if (isLaggy) {
            if (FPSMonitor.getMinimumFps() > 15) {
                isLaggy = false;
            }
        }
        else {
            if (lastPortalRenderInfos.size() > 10) {
                if (FPSMonitor.getAverageFps() < 8 || FPSMonitor.getMinimumFps() < 6) {
                    client.ingameGUI.setOverlayMessage(
                        new TranslationTextComponent("imm_ptl.laggy"),
                        false
                    );
                    isLaggy = true;
                }
            }
        }
    }
    
    private static void updateViewBobbingFactor(Entity cameraEntity) {
        Vec3d cameraPosVec = cameraEntity.getEyePosition(tickDelta);
        double minPortalDistance = CHelper.getClientNearbyPortals(10)
            .map(portal -> portal.getDistanceToNearestPointInPortal(cameraPosVec))
            .min(Double::compareTo).orElse(1.0);
        if (minPortalDistance < 1) {
            if (minPortalDistance > 0.5) {
                setViewBobFactor((minPortalDistance - 0.5) * 2);
            }
            else {
                setViewBobFactor(0);
            }
        }
        else {
            setViewBobFactor(1);
        }
    }
    
    private static void setViewBobFactor(double arg) {
        if (arg < viewBobFactor) {
            viewBobFactor = arg;
        }
        else {
            viewBobFactor = MathHelper.lerp(0.1, viewBobFactor, arg);
        }
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
                        PortalRenderer.client.getFramebuffer().framebufferWidth,
                        PortalRenderer.client.getFramebuffer().framebufferHeight
                    );
                }
                
                GlStateManager.enableTexture();
                GlStateManager.activeTexture(GL13.GL_TEXTURE0);
                
                GlStateManager.bindTexture(textureProvider.framebufferTexture);
                GlStateManager.texParameter(3553, 10241, 9729);
                GlStateManager.texParameter(3553, 10240, 9729);
                GlStateManager.texParameter(3553, 10242, 10496);
                GlStateManager.texParameter(3553, 10243, 10496);
                
                ViewAreaRenderer.drawPortalViewTriangle(portal, matrixStack, false, false);
                
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
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, (double) int_1, (double) int_2, 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.translatef(0.0F, 0.0F, -2000.0F);
        GlStateManager.viewport(0, 0, int_1, int_2);
        GlStateManager.enableTexture();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        if (doEnableAlphaTest) {
            RenderSystem.enableAlphaTest();
        }
        else {
            GlStateManager.disableAlphaTest();
        }
        GlStateManager.disableBlend();
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
        
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.popMatrix();
        
        CHelper.checkGlError();
    }
    
    public static void earlyUpdateLight() {
        if (CGlobal.clientWorldLoader == null) {
            return;
        }
        
        CGlobal.clientWorldLoader.clientWorldMap.values().forEach(
            world -> {
                if (world != Minecraft.getInstance().world) {
                    int updateNum = world.getChunkProvider().getLightManager().tick(
                        1000, true, true
                    );
                }
            }
        );
    }
    
    public static void applyMirrorFaceCulling() {
        glCullFace(GL_FRONT);
    }
    
    public static void recoverFaceCulling() {
        glCullFace(GL_BACK);
    }
    
    public static boolean isRenderingOddNumberOfMirrors() {
        Stack<Portal> portalLayers = CGlobal.renderer.portalLayers;
        int number = 0;
        for (Portal portal : portalLayers) {
            if (portal instanceof Mirror) {
                number++;
            }
        }
        return number % 2 == 1;
    }
    
    public static void adjustCameraPos(ActiveRenderInfo camera) {
        Vec3d pos = originalCamera.getProjectedView();
        for (Portal portal : CGlobal.renderer.portalLayers) {
            pos = portal.transformPoint(pos);
        }
        ((IECamera) camera).mySetPos(pos);
    }
    
    public static void clearAlphaTo1(Framebuffer mcFrameBuffer) {
        mcFrameBuffer.bindFramebuffer(true);
        RenderSystem.colorMask(false, false, false, true);
        RenderSystem.clearColor(0, 0, 0, 1.0f);
        RenderSystem.clear(GL_COLOR_BUFFER_BIT, true);
        RenderSystem.colorMask(true, true, true, true);
    }
}
