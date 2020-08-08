package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderInfo;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
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
        Validate.isTrue(client.renderViewEntity.world == client.world);
        
        List<Portal> portalsNearbySorted = getPortalsNearbySorted();
        
        if (portalsNearbySorted.isEmpty()) {
            return;
        }
        
        ClippingHelper frustum = null;
        if (CGlobal.earlyFrustumCullingPortal) {
            frustum = new ClippingHelper(
                matrixStack.getLast().getMatrix(),
                RenderStates.projectionMatrix
            );
            
            Vector3d cameraPos = client.gameRenderer.getActiveRenderInfo().getProjectedView();
            frustum.setCameraPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        }
        
        for (Portal portal : portalsNearbySorted) {
            renderPortalIfRoughCheckPassed(portal, matrixStack, frustum);
        }
    }
    
    private void renderPortalIfRoughCheckPassed(
        Portal portal,
        MatrixStack matrixStack,
        ClippingHelper frustum
    ) {
        if (!portal.isPortalValid()) {
            Helper.err("rendering invalid portal " + portal);
            return;
        }
        
        if (RenderStates.getRenderedPortalNum() >= Global.portalRenderLimit) {
            return;
        }
        
        Vector3d thisTickEyePos = client.gameRenderer.getActiveRenderInfo().getProjectedView();
        
        if (!portal.isInFrontOfPortal(thisTickEyePos)) {
            return;
        }
        
        if (PortalRendering.isRendering()) {
            Portal outerPortal = PortalRendering.getRenderingPortal();
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
        if (PortalRendering.getPortalLayer() > 1) {
            //do not render deep layers of mirror when far away
            range /= (PortalRendering.getPortalLayer());
        }
        if (RenderStates.isLaggy) {
            range = 16;
        }
        return range;
    }
    
    private List<Portal> getPortalsNearbySorted() {
        Vector3d cameraPos = client.gameRenderer.getActiveRenderInfo().getProjectedView();
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
        if (PortalRendering.getPortalLayer() > PortalRendering.getMaxPortalLayer()) {
            return;
        }
        
        Entity cameraEntity = client.renderViewEntity;
        
        ClientWorld newWorld = CGlobal.clientWorldLoader.getWorld(portal.dimensionTo);
        
        ActiveRenderInfo camera = client.gameRenderer.getActiveRenderInfo();
        
        PortalRendering.onBeginPortalWorldRendering();
        
        invokeWorldRendering(new RenderInfo(
            newWorld,
            PortalRendering.getRenderingCameraPos(),
            getAdditionalCameraTransformation(portal),
            portal
        ));
        
        PortalRendering.onEndPortalWorldRendering();
        
        GlStateManager.enableDepthTest();
        GlStateManager.disableBlend();
        
        MyRenderHelper.restoreViewPort();
        
        
    }
    
    public void invokeWorldRendering(
        RenderInfo renderInfo
    ) {
        MyGameRenderer.renderWorldNew(
            renderInfo,
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
    
    // Scaling does not interfere camera transformation
    @Nullable
    public static Matrix4f getAdditionalCameraTransformation(Portal portal) {
        if (portal instanceof Mirror) {
            return TransformationManager.getMirrorTransformation(portal.getNormal());
        }
        else {
            if (portal.rotation != null) {
                Quaternion rot = portal.rotation.copy();
                rot.conjugate();
                return new Matrix4f(rot);
            }
            else {
                return null;
            }
        }
    }
    
}
