package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public abstract class PortalRenderer {
    
    public static final Minecraft client = Minecraft.getInstance();
    
    
    // this WILL be called when rendering portal
    public abstract void onBeforeTranslucentRendering(MatrixStack matrixStack);
    
    // this WILL be called when rendering portal
    public abstract void onAfterTranslucentRendering(MatrixStack matrixStack);
    
    // this WILL be called when rendering portal
    public abstract void onRenderCenterEnded(MatrixStack matrixStack);
    
    // this will NOT be called when rendering portal
    public abstract void prepareRendering();
    
    // this will NOT be called when rendering portal
    public abstract void finishRendering();
    
    // this will be called when rendering portal entities
    public abstract void renderPortalInEntityRenderer(Portal portal);
    
    // return true to skip framebuffer clear
    // this will also be called in outer world rendering
    public abstract boolean replaceFrameBufferClearing();
    
    protected void renderPortals(MatrixStack matrixStack) {
        Validate.isTrue(client.renderViewEntity.world == client.world);
        
        Supplier<ClippingHelper> frustumSupplier = Helper.cached(() -> {
            ClippingHelper frustum = new ClippingHelper(
                matrixStack.getLast().getMatrix(),
                RenderStates.projectionMatrix
            );
            
            Vector3d cameraPos = client.gameRenderer.getActiveRenderInfo().getProjectedView();
            frustum.setCameraPosition(cameraPos.x, cameraPos.y, cameraPos.z);
            
            return frustum;
        });
        
        double renderRange = getRenderRange();
        
        List<Portal> portalsToRender = new ArrayList<>();
        List<GlobalTrackedPortal> globalPortals = McHelper.getGlobalPortals(client.world);
        for (GlobalTrackedPortal globalPortal : globalPortals) {
            if (!shouldSkipRenderingPortal(globalPortal, frustumSupplier)) {
                portalsToRender.add(globalPortal);
            }
        }
        
        client.world.getAllEntities().forEach(e -> {
            if (e instanceof Portal) {
                Portal portal = (Portal) e;
                if (!shouldSkipRenderingPortal(portal, frustumSupplier)) {
                    portalsToRender.add(portal);
                }
            }
        });
        
        Vector3d cameraPos = McHelper.getCurrentCameraPos();
        portalsToRender.sort(Comparator.comparingDouble(portalEntity ->
            portalEntity.getDistanceToNearestPointInPortal(cameraPos)
        ));
        
        for (Portal portal : portalsToRender) {
            doRenderPortal(portal, matrixStack);
        }
    }
    
    private boolean shouldSkipRenderingPortal(Portal portal, Supplier<ClippingHelper> frustumSupplier) {
        if (!portal.isPortalValid()) {
            return true;
        }
        
        if (RenderStates.getRenderedPortalNum() >= Global.portalRenderLimit) {
            return true;
        }
        
        Vector3d cameraPos = McHelper.getCurrentCameraPos();
        
        if (!portal.isInFrontOfPortal(cameraPos)) {
            return true;
        }
        
        if (PortalRendering.isRendering()) {
            Portal outerPortal = PortalRendering.getRenderingPortal();
            if (Portal.isParallelOrientedPortal(portal, outerPortal)) {
                return true;
            }
        }
        
        if (isOutOfDistance(portal)) {
            return true;
        }
        
        if (CGlobal.earlyFrustumCullingPortal) {
            ClippingHelper frustum = frustumSupplier.get();
            if (!frustum.isBoundingBoxInFrustum(portal.getExactBoundingBox())) {
                return true;
            }
        }
        return false;
    }
    
    protected final double getRenderRange() {
        double range = client.gameSettings.renderDistanceChunks * 16;
        if (RenderStates.isLaggy || Global.reducedPortalRendering) {
            range = 16;
        }
        if (PortalRendering.getPortalLayer() > 1) {
            //do not render deep layers of mirror when far away
            range /= (PortalRendering.getPortalLayer());
        }
        if (PortalRendering.getPortalLayer() >= 1) {
            range *= PortalRendering.getRenderingPortal().scaling;
            range = Math.min(range, 32 * 16);
        }
        return range;
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
        
        int renderDistance = getPortalRenderDistance(portal);
        
        invokeWorldRendering(new RenderInfo(
            newWorld,
            PortalRendering.getRenderingCameraPos(),
            getAdditionalCameraTransformation(portal),
            portal.getUniqueID(),
            renderDistance
        ));
        
        PortalRendering.onEndPortalWorldRendering();
        
        GlStateManager.enableDepthTest();
        GlStateManager.disableBlend();
        
        MyRenderHelper.restoreViewPort();
        
        
    }
    
    private static int getPortalRenderDistance(Portal portal) {
        if (portal.scaling > 2) {
            double radiusBlocks = portal.getDestAreaRadius() * 1.4;
    
            return Math.max((int) (radiusBlocks / 16), client.gameSettings.renderDistanceChunks);
        }
        if (Global.reducedPortalRendering) {
            return client.gameSettings.renderDistanceChunks / 3;
        }
        return client.gameSettings.renderDistanceChunks;
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
        
        Vector3d cameraPos = McHelper.getCurrentCameraPos();
        if (portal.getDistanceToNearestPointInPortal(cameraPos) > getRenderRange()) {
            return true;
        }
        
        return false;
    }
    
    // Scaling does not interfere camera transformation
    @Nullable
    public static Matrix4f getAdditionalCameraTransformation(Portal portal) {
        
        Matrix4f rot = getPortalRotationMatrix(portal);
        
        Matrix4f mirror = portal instanceof Mirror ?
            TransformationManager.getMirrorTransformation(portal.getNormal()) : null;
        
        return combineNullable(rot, mirror);
    }
    
    @Nullable
    private static Matrix4f getPortalRotationMatrix(Portal portal) {
        if (portal.rotation == null) {
            return null;
        }
        
        Quaternion rot = portal.rotation.copy();
        rot.conjugate();
        return new Matrix4f(rot);
    }
    
    @Nullable
    private static Matrix4f combineNullable(@Nullable Matrix4f a, @Nullable Matrix4f b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        a.mul(b);
        return a;
    }
    
}
