package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.PortalLayers;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class PortalRenderer {
    
    public static final Minecraft client = Minecraft.getInstance();
    
    //this WILL be called when rendering portal
    public abstract void onBeforeTranslucentRendering(MatrixStack matrixStack);
    
    //this WILL be called when rendering portal
    public abstract void onAfterTranslucentRendering(MatrixStack matrixStack);
    
    //this WILL be called when rendering portal
    public abstract void onRenderCenterEnded(MatrixStack matrixStack);
    
    //this will NOT be called when rendering portal
    public abstract void prepareRendering();
    
    //this will NOT be called when rendering portal
    public abstract void finishRendering();
    
    //this will be called when rendering portal entities
    public abstract void renderPortalInEntityRenderer(Portal portal);
    
    public abstract boolean shouldSkipClearing();
    
    protected void renderPortals(MatrixStack matrixStack) {
        assert client.renderViewEntity.world == client.world;
        assert client.renderViewEntity.dimension == client.world.dimension.getType();
        
        List<Portal> portalsNearbySorted = getPortalsNearbySorted();
        
        if (portalsNearbySorted.isEmpty()) {
            return;
        }
        
        ClippingHelperImpl frustum = null;
        if (CGlobal.earlyFrustumCullingPortal) {
            frustum = new ClippingHelperImpl(
                matrixStack.getLast().getMatrix(),
                RenderStates.projectionMatrix
            );
            
            Vec3d cameraPos = client.gameRenderer.getActiveRenderInfo().getProjectedView();
            frustum.setCameraPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        }
        
        for (Portal portal : portalsNearbySorted) {
            renderPortalIfRoughCheckPassed(portal, matrixStack, frustum);
        }
    }
    
    private void renderPortalIfRoughCheckPassed(
        Portal portal,
        MatrixStack matrixStack,
        ClippingHelperImpl frustum
    ) {
        if (!portal.isPortalValid()) {
            Helper.err("rendering invalid portal " + portal);
            return;
        }
        
        if (RenderStates.getRenderedPortalNum() >= Global.portalRenderLimit) {
            return;
        }
        
        Vec3d thisTickEyePos = client.gameRenderer.getActiveRenderInfo().getProjectedView();
        
        if (!portal.isInFrontOfPortal(thisTickEyePos)) {
            return;
        }
        
        if (PortalLayers.isRendering()) {
            Portal outerPortal = PortalLayers.getRenderingPortal();
            if (Portal.isParallelPortal(portal, outerPortal)) {
                return;
            }
        }
        
        if (isOutOfDistance(portal)) {
            return;
        }
        
        if (CGlobal.earlyFrustumCullingPortal) {
            if (!frustum.isBoundingBoxInFrustum(portal.getBoundingBox())) {
                return;
            }
        }
        
        doRenderPortal(portal, matrixStack);
    }
    
    protected final double getRenderRange() {
        double range = client.gameSettings.renderDistanceChunks * 16;
        if (PortalLayers.getPortalLayer() > 1) {
            //do not render deep layers of mirror when far away
            range /= (PortalLayers.getPortalLayer());
        }
        if (RenderStates.isLaggy) {
            range = 16;
        }
        return range;
    }
    
    private List<Portal> getPortalsNearbySorted() {
        Vec3d cameraPos = client.gameRenderer.getActiveRenderInfo().getProjectedView();
        return CHelper.getClientNearbyPortals(getRenderRange())
            .sorted(
                Comparator.comparing(portalEntity ->
                    portalEntity.getDistanceToNearestPointInPortal(cameraPos)
                )
            ).collect(Collectors.toList());
    }
    
    protected abstract void doRenderPortal(
        Portal portal,
        MatrixStack matrixStack
    );
    
    protected final void renderPortalContent(
        Portal portal
    ) {
        if (PortalLayers.getPortalLayer() > PortalLayers.getMaxPortalLayer()) {
            return;
        }
        
        Entity cameraEntity = client.renderViewEntity;
        
        Vec3d newEyePos = portal.transformPoint(McHelper.getEyePos(cameraEntity));
        Vec3d newLastTickEyePos = portal.transformPoint(McHelper.getLastTickEyePos(cameraEntity));
        
        ClientWorld newWorld = CGlobal.clientWorldLoader.getWorld(portal.dimensionTo);
        
        ActiveRenderInfo camera = client.gameRenderer.getActiveRenderInfo();
        
        assert cameraEntity.world == client.world;
        
        PortalLayers.onBeginPortalWorldRendering();
        
        invokeWorldRendering(newEyePos, newLastTickEyePos, newWorld);
        
        PortalLayers.onEndPortalWorldRendering();
        
        GlStateManager.enableDepthTest();
        GlStateManager.disableBlend();
        
        MyRenderHelper.restoreViewPort();
        
        MyGameRenderer.updateFogColor();
        MyGameRenderer.resetFogState();
    }
    
    protected void invokeWorldRendering(
        Vec3d newEyePos,
        Vec3d newLastTickEyePos,
        ClientWorld newWorld
    ) {
        MyGameRenderer.switchAndRenderTheWorld(
            newWorld, newEyePos,
            newLastTickEyePos,
            Runnable::run
        );
    }
    
    private boolean isOutOfDistance(Portal portal) {
        
        return false;
//        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
//        if (portal.getDistanceToNearestPointInPortal(cameraPos) > getRenderRange()) {
//            return true;
//        }
//
//        if (getPortalLayer() >= 1 &&
//            portal.getDistanceToNearestPointInPortal(cameraPos) >
//                (16 * maxPortalLayer.get())
//        ) {
//            return true;
//        }
//        return false;
    }
    
//    @Deprecated
//    protected void renderPortalContentWithContextSwitched(
//        Portal portal, Vec3d oldCameraPos, ClientWorld oldWorld
//    ) {
//
//
//        WorldRenderer worldRenderer = CGlobal.clientWorldLoader.getWorldRenderer(portal.dimensionTo);
//        ClientWorld destClientWorld = CGlobal.clientWorldLoader.getWorld(portal.dimensionTo);
//
//        CHelper.checkGlError();
//
//        MyGameRenderer.renderWorldDeprecated(
//            worldRenderer, destClientWorld, oldCameraPos, oldWorld
//        );
//
//        CHelper.checkGlError();
//
//    }
    
}
