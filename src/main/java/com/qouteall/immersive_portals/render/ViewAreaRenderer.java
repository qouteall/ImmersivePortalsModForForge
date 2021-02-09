package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.GeometryPortalShape;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalLike;
import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import org.lwjgl.opengl.GL32;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Vector3d;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

public class ViewAreaRenderer {
    private static void buildPortalViewAreaTrianglesBuffer(
        Vector3d fogColor, PortalLike portal, BufferBuilder bufferbuilder,
        Vector3d cameraPos, float tickDelta, float layerWidth
    ) {
        bufferbuilder.begin(GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        
        Vector3d posInPlayerCoordinate = portal.getOriginPos().subtract(cameraPos);
        
        Consumer<Vector3d> vertexOutput = p -> putIntoVertex(
            bufferbuilder, p, fogColor
        );
        
        portal.renderViewAreaMesh(posInPlayerCoordinate, vertexOutput);
        
    }
    
    public static void generateViewAreaTriangles(Portal portal, Vector3d posInPlayerCoordinate, Consumer<Vector3d> vertexOutput) {
        if (portal.specialShape == null) {
            if (portal.getIsGlobal()) {
                generateTriangleForGlobalPortal(
                    vertexOutput,
                    portal,
                    posInPlayerCoordinate
                );
            }
            else {
                generateTriangleForNormalShape(
                    vertexOutput,
                    portal,
                    posInPlayerCoordinate
                );
            }
        }
        else {
            generateTriangleForSpecialShape(
                vertexOutput,
                portal,
                posInPlayerCoordinate
            );
        }
    }
    
    public static void generateTriangleForSpecialShape(
        Consumer<Vector3d> vertexOutput,
        Portal portal,
        Vector3d posInPlayerCoordinate
    ) {
        generateTriangleSpecial(
            vertexOutput, portal, posInPlayerCoordinate
        );
    }
    
    public static void generateTriangleSpecial(
        Consumer<Vector3d> vertexOutput,
        Portal portal,
        Vector3d posInPlayerCoordinate
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
        Vector3d posInPlayerCoordinate
    ) {
        //avoid floating point error for converted global portal
        final double w = Math.min(portal.width, 23333);
        final double h = Math.min(portal.height, 23333);
        Vector3d v0 = portal.getPointInPlaneLocal(
            w / 2 - (double) 0,
            -h / 2 + (double) 0
        );
        Vector3d v1 = portal.getPointInPlaneLocal(
            -w / 2 + (double) 0,
            -h / 2 + (double) 0
        );
        Vector3d v2 = portal.getPointInPlaneLocal(
            w / 2 - (double) 0,
            h / 2 - (double) 0
        );
        Vector3d v3 = portal.getPointInPlaneLocal(
            -w / 2 + (double) 0,
            h / 2 - (double) 0
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
        Vector3d posInPlayerCoordinate
    ) {
        Vector3d cameraPosLocal = posInPlayerCoordinate.scale(-1);
        
        double cameraLocalX = cameraPosLocal.dotProduct(portal.axisW);
        double cameraLocalY = cameraPosLocal.dotProduct(portal.axisH);
        
        double r = Minecraft.getInstance().gameSettings.renderDistanceChunks * 16 - 16;
        if (TransformationManager.isIsometricView) {
            r *= 2;
        }
        
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
        PortalLike portal,
        MatrixStack matrixStack,
        boolean doFrontCulling,
        boolean doFaceCulling
    ) {
        
        Minecraft.getInstance().getProfiler().startSection("render_view_triangle");
        
        Vector3d fogColor = FogRendererContext.getCurrentFogColor.get();
        
        if (doFaceCulling) {
            GlStateManager.enableCull();
        }
        else {
            GlStateManager.disableCull();
        }
        
        //should not affect shader pipeline
        FrontClipping.disableClipping();
        
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
                FrontClipping.setupInnerClipping(
                    matrixStack, PortalRendering.getRenderingPortal(), false
                );
            }
        }
        
        Minecraft.getInstance().getProfiler().startSection("draw");
        glEnable(GL32.GL_DEPTH_CLAMP);
        CHelper.checkGlError();
        McHelper.runWithTransformation(
            matrixStack,
            tessellator::draw
        );
        glDisable(GL32.GL_DEPTH_CLAMP);
        Minecraft.getInstance().getProfiler().endSection();
        
        if (shouldReverseCull) {
            MyRenderHelper.recoverFaceCulling();
        }
        if (doFrontCulling) {
            if (PortalRendering.isRendering()) {
                FrontClipping.disableClipping();
            }
        }
        
        //this is important
        GlStateManager.enableCull();
        
        Minecraft.getInstance().getProfiler().endSection();
    }
    
}
