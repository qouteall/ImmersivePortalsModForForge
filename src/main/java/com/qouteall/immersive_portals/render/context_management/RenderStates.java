package com.qouteall.immersive_portals.render.context_management;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.FPSMonitor;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameType;
import net.minecraft.world.dimension.DimensionType;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RenderStates {
    
    public static DimensionType originalPlayerDimension;
    public static Vec3d originalPlayerPos;
    public static Vec3d originalPlayerLastTickPos;
    public static GameType originalGameMode;
    public static float tickDelta = 0;
    
    public static Set<DimensionType> renderedDimensions = new HashSet<>();
    public static List<List<WeakReference<Portal>>> lastPortalRenderInfos = new ArrayList<>();
    public static List<List<WeakReference<Portal>>> portalRenderInfos = new ArrayList<>();
    
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
        
        Entity cameraEntity = MyRenderHelper.client.renderViewEntity;
        
        if (cameraEntity == null) {
            return;
        }
        
        originalPlayerDimension = cameraEntity.dimension;
        originalPlayerPos = cameraEntity.getPositionVec();
        originalPlayerLastTickPos = McHelper.lastTickPosOf(cameraEntity);
        NetworkPlayerInfo entry = CHelper.getClientPlayerListEntry();
        originalGameMode = entry != null ? entry.getGameType() : GameType.CREATIVE;
        tickDelta = tickDelta_;
        
        renderedDimensions.clear();
        lastPortalRenderInfos = portalRenderInfos;
        portalRenderInfos = new ArrayList<>();
        
        FogRendererContext.update();
        
        renderStartNanoTime = System.nanoTime();
        
        updateViewBobbingFactor(cameraEntity);
        
        projectionMatrix = null;
        originalCamera = MyRenderHelper.client.gameRenderer.getActiveRenderInfo();
        
        originalCameraLightPacked = MyRenderHelper.client.getRenderManager()
            .getPackedLight(MyRenderHelper.client.renderViewEntity, tickDelta);
        
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
                    MyRenderHelper.client.ingameGUI.setOverlayMessage(
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
        if (dimensionType == originalPlayerDimension) {
            return true;
        }
        return renderedDimensions.contains(dimensionType);
    }
    
}
