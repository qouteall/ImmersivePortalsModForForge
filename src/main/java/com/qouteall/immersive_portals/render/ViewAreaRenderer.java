package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.portal.GeometryPortalShape;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.render.context_management.DimensionRenderHelper;
import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Vector3d;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

public class ViewAreaRenderer {
    private static void buildPortalViewAreaTrianglesBuffer(
        Vector3d fogColor, Portal portal, BufferBuilder bufferbuilder,
        Vector3d cameraPos, float tickDelta, float layerWidth
    ) {
        bufferbuilder.begin(GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        
        Vector3d posInPlayerCoordinate = portal.getPositionVec().subtract(cameraPos);
        
        if (portal instanceof Mirror) {
            //rendering portal behind translucent objects with shader is broken
            double mirrorOffset =
                (OFInterface.isShaders.getAsBoolean() || Global.pureMirror) ? 0.01 : -0.01;
            posInPlayerCoordinate = posInPlayerCoordinate.add(
                portal.getNormal().scale(mirrorOffset));
        }
        
        Consumer<Vector3d> vertexOutput = p -> putIntoVertex(
            bufferbuilder, p, fogColor
        );
        
        boolean isClose = isCloseToPortal(portal, cameraPos);
        
        if (portal.specialShape == null) {
            if (portal instanceof GlobalTrackedPortal) {
                generateTriangleForGlobalPortal(
                    vertexOutput,
                    portal,
                    layerWidth,
                    posInPlayerCoordinate
                );
            }
            else {
                generateTriangleForNormalShape(
                    vertexOutput,
                    portal,
                    layerWidth,
                    posInPlayerCoordinate
                );
            }
        }
        else {
            generateTriangleForSpecialShape(
                vertexOutput,
                portal,
                layerWidth,
                posInPlayerCoordinate
            );
        }
        
        if (isClose) {
            renderAdditionalBox(portal, cameraPos, vertexOutput);
        }
    }
    
    private static void generateTriangleForSpecialShape(
        Consumer<Vector3d> vertexOutput,
        Portal portal,
        float layerWidth,
        Vector3d posInPlayerCoordinate
    ) {
        generateTriangleSpecial(
            vertexOutput, portal, posInPlayerCoordinate,
            portal.getNormal().scale(-0.5)
        );
    }
    
    private static void generateTriangleSpecial(
        Consumer<Vector3d> vertexOutput,
        Portal portal,
        Vector3d posInPlayerCoordinate,
        Vector3d innerOffset
    ) {
        GeometryPortalShape specialShape = portal.specialShape;
        
        for (GeometryPortalShape.TriangleInPlane triangle : specialShape.triangles) {
            Vector3d a = posInPlayerCoordinate
                .add(portal.axisW.scale(triangle.x1))
                .add(portal.axisH.scale(triangle.y1));
            
            Vector3d b = posInPlayerCoordinate
                .add(portal.axisW.scale(triangle.x3))
                .add(portal.axisH.scale(triangle.y3));
            
            Vector3d c = posInPlayerCoordinate
                .add(portal.axisW.scale(triangle.x2))
                .add(portal.axisH.scale(triangle.y2));
            
            vertexOutput.accept(a);
            vertexOutput.accept(b);
            vertexOutput.accept(c);
        }
    }
    
    //according to https://stackoverflow.com/questions/43002528/when-can-hotspot-allocate-objects-on-the-stack
    //this will not generate gc pressure
    private static void putIntoLocalVertex(
        Consumer<Vector3d> vertexOutput,
        Portal portal,
        Vector3d offset,
        Vector3d posInPlayerCoordinate,
        double localX, double localY
    ) {
        vertexOutput.accept(
            posInPlayerCoordinate
                .add(portal.axisW.scale(localX))
                .add(portal.axisH.scale(localY))
                .add(offset)
        );
    }
    
    private static void generateTriangleForNormalShape(
        Consumer<Vector3d> vertexOutput,
        Portal portal,
        float layerWidth,
        Vector3d posInPlayerCoordinate
    ) {
        Vector3d v0 = portal.getPointInPlaneLocal(
            portal.width / 2 - (double) 0,
            -portal.height / 2 + (double) 0
        );
        Vector3d v1 = portal.getPointInPlaneLocal(
            -portal.width / 2 + (double) 0,
            -portal.height / 2 + (double) 0
        );
        Vector3d v2 = portal.getPointInPlaneLocal(
            portal.width / 2 - (double) 0,
            portal.height / 2 - (double) 0
        );
        Vector3d v3 = portal.getPointInPlaneLocal(
            -portal.width / 2 + (double) 0,
            portal.height / 2 - (double) 0
        );
        
        putIntoQuad(
            vertexOutput,
            v0.add(posInPlayerCoordinate),
            v2.add(posInPlayerCoordinate),
            v3.add(posInPlayerCoordinate),
            v1.add(posInPlayerCoordinate)
        );
        
    }
    
    private static void generateTriangleForGlobalPortal(
        Consumer<Vector3d> vertexOutput,
        Portal portal,
        float layerWidth,
        Vector3d posInPlayerCoordinate
    ) {
        Vector3d cameraPosLocal = posInPlayerCoordinate.scale(-1);
        
        double cameraLocalX = cameraPosLocal.dotProduct(portal.axisW);
        double cameraLocalY = cameraPosLocal.dotProduct(portal.axisH);
        
        double r = Minecraft.getInstance().gameSettings.renderDistanceChunks * 16 - 16;
        
        double distance = Math.abs(cameraPosLocal.dotProduct(portal.getNormal()));
        if (distance > 200) {
            r = r * 200 / distance;
        }
        
        Vector3d v0 = portal.getPointInPlaneLocalClamped(
            r + cameraLocalX,
            -r + cameraLocalY
        );
        Vector3d v1 = portal.getPointInPlaneLocalClamped(
            -r + cameraLocalX,
            -r + cameraLocalY
        );
        Vector3d v2 = portal.getPointInPlaneLocalClamped(
            r + cameraLocalX,
            r + cameraLocalY
        );
        Vector3d v3 = portal.getPointInPlaneLocalClamped(
            -r + cameraLocalX,
            r + cameraLocalY
        );
        
        putIntoQuad(
            vertexOutput,
            v0.add(posInPlayerCoordinate),
            v2.add(posInPlayerCoordinate),
            v3.add(posInPlayerCoordinate),
            v1.add(posInPlayerCoordinate)
        );
    }
    
    static private void putIntoVertex(BufferBuilder bufferBuilder, Vector3d pos, Vector3d fogColor) {
        bufferBuilder
            .pos(pos.x, pos.y, pos.z)
            .color((float) fogColor.x, (float) fogColor.y, (float) fogColor.z, 1.0f)
            .endVertex();
    }
    
    //a d
    //b c
    private static void putIntoQuad(
        Consumer<Vector3d> vertexOutput,
        Vector3d a,
        Vector3d b,
        Vector3d c,
        Vector3d d
    ) {
        //counter-clockwise triangles are front-faced in default
        
        vertexOutput.accept(b);
        vertexOutput.accept(c);
        vertexOutput.accept(d);
        
        vertexOutput.accept(d);
        vertexOutput.accept(a);
        vertexOutput.accept(b);
        
    }
    
    public static void drawPortalViewTriangle(
        Portal portal,
        MatrixStack matrixStack,
        boolean doFrontCulling,
        boolean doFaceCulling
    ) {
        
        Minecraft.getInstance().getProfiler().startSection("render_view_triangle");
        
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(portal.dimensionTo);
        
        Vector3d fogColor = getCurrentFogColor(portal);
        
        if (doFaceCulling) {
            GlStateManager.enableCull();
        }
        else {
            GlStateManager.disableCull();
        }
        
        //should not affect shader pipeline
        GlStateManager.disableTexture();
        PixelCuller.endCulling();
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        buildPortalViewAreaTrianglesBuffer(
            fogColor,
            portal,
            bufferbuilder,
            PortalRenderer.client.gameRenderer.getActiveRenderInfo().getProjectedView(),
            RenderStates.tickDelta,
            portal instanceof Mirror ? 0 : 0.45F
        );
        
        boolean shouldReverseCull = PortalRendering.isRenderingOddNumberOfMirrors();
        if (shouldReverseCull) {
            MyRenderHelper.applyMirrorFaceCulling();
        }
        if (doFrontCulling) {
            if (PortalRendering.isRendering()) {
                PixelCuller.updateCullingPlaneInner(
                    matrixStack, PortalRendering.getRenderingPortal(), false
                );
                PixelCuller.loadCullingPlaneClassical(matrixStack);
                PixelCuller.startClassicalCulling();
            }
        }
        
        Minecraft.getInstance().getProfiler().startSection("draw");
        McHelper.runWithTransformation(
            matrixStack,
            () -> tessellator.draw()
        );
        Minecraft.getInstance().getProfiler().endSection();
        
        if (shouldReverseCull) {
            MyRenderHelper.recoverFaceCulling();
        }
        if (doFrontCulling) {
            if (PortalRendering.isRendering()) {
                PixelCuller.endCulling();
            }
        }
        
        GlStateManager.enableTexture();
        
        //this is important
        GlStateManager.enableCull();
        
        Minecraft.getInstance().getProfiler().endSection();
    }
    
    private static Vector3d getCurrentFogColor(Portal portal) {
        
        if (OFInterface.isShaders.getAsBoolean()) {
            return Vector3d.ZERO;
        }
        
        //TODO handle edgelessSky option
        
        return FogRendererContext.getFogColorOf(
            ((ClientWorld) portal.getDestinationWorld()),
            portal.transformPoint(McHelper.getCurrentCameraPos())
        );
    }
    
    
    private static boolean isCloseToPortal(
        Portal portal,
        Vector3d cameraPos
    ) {
        return (portal.getDistanceToPlane(cameraPos) < 0.2) &&
            portal.isPointInPortalProjection(cameraPos);
    }
    
    private static void renderAdditionalBox(
        Portal portal,
        Vector3d cameraPos,
        Consumer<Vector3d> vertexOutput
    ) {
        Vector3d projected = portal.getPointInPortalProjection(cameraPos).subtract(cameraPos);
        Vector3d normal = portal.getNormal();
        
        renderHood(portal, vertexOutput, projected, normal, 0.4);
    }
    
    private static void renderHood(
        Portal portal,
        Consumer<Vector3d> vertexOutput,
        Vector3d projected,
        Vector3d normal,
        double boxRadius
    ) {
        Vector3d dx = portal.axisW.scale(boxRadius);
        Vector3d dy = portal.axisH.scale(boxRadius);
        
        Vector3d a = projected.add(dx).add(dy);
        Vector3d b = projected.subtract(dx).add(dy);
        Vector3d c = projected.subtract(dx).subtract(dy);
        Vector3d d = projected.add(dx).subtract(dy);
        
        Vector3d mid = projected.add(normal.scale(-0.5));
        
        vertexOutput.accept(b);
        vertexOutput.accept(mid);
        vertexOutput.accept(a);
        
        vertexOutput.accept(c);
        vertexOutput.accept(mid);
        vertexOutput.accept(b);
        
        vertexOutput.accept(d);
        vertexOutput.accept(mid);
        vertexOutput.accept(c);
        
        vertexOutput.accept(a);
        vertexOutput.accept(mid);
        vertexOutput.accept(d);
    }
    
    
    @Deprecated
    private static void renderAdditionalBoxExperimental(
        Portal portal,
        Consumer<Vector3d> vertexOutput,
        Vector3d projected,
        Vector3d normal
    ) {
        final double boxRadius = 1.5;
        final double boxDepth = 0.5;
        
        //b  a
        //c  d
        
        Vector3d dx = portal.axisW.scale(boxRadius);
        Vector3d dy = portal.axisH.scale(boxRadius);
        
        Vector3d a = projected.add(dx).add(dy);
        Vector3d b = projected.subtract(dx).add(dy);
        Vector3d c = projected.subtract(dx).subtract(dy);
        Vector3d d = projected.add(dx).subtract(dy);
        
        Vector3d dz = normal.scale(-boxDepth);
        
        Vector3d as = a.add(dz);
        Vector3d bs = b.add(dz);
        Vector3d cs = c.add(dz);
        Vector3d ds = d.add(dz);
        
        putIntoQuad(vertexOutput, a, b, bs, as);
        putIntoQuad(vertexOutput, b, c, cs, bs);
        putIntoQuad(vertexOutput, c, d, ds, cs);
        putIntoQuad(vertexOutput, d, a, as, ds);
        
        putIntoQuad(vertexOutput, a, b, c, d);
    }
    
}
