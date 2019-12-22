package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.SpecialPortalShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.GL_CLIP_PLANE0;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

public class ViewAreaRenderer {
    public static interface VertexOutput {
        //to avoid temporal Vec3d object creation
        //to improve cache friendliness
        void accept(double x, double y, double z);
    }
    
    private static void buildPortalViewAreaTrianglesBuffer(
        Vec3d fogColor, Portal portal, BufferBuilder bufferbuilder,
        Vec3d cameraPos, float partialTicks, float layerWidth
    ) {
        //if layerWidth is small, the teleportation will not be seamless
        
        //counter-clockwise triangles are front-faced in default
        
        bufferbuilder.begin(GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        
        Vec3d posInPlayerCoordinate = portal.getPositionVec().subtract(cameraPos);
        
        if (portal instanceof Mirror) {
            posInPlayerCoordinate = posInPlayerCoordinate.add(portal.getNormal().scale(-0.001));
        }
        
        VertexOutput vertexOutput = (x, y, z) -> putIntoVertexFast(
            bufferbuilder, x, y, z, fogColor
        );
        
        if (portal.specialShape == null) {
            generateTriangleBiLayered(
                vertexOutput,
                portal,
                layerWidth,
                posInPlayerCoordinate
            );
        }
        else {
            generateTriangleSpecialBiLayered(
                vertexOutput,
                portal,
                layerWidth,
                posInPlayerCoordinate
            );
        }
    }
    
    private static void generateTriangleSpecialBiLayered(
        VertexOutput vertexOutput,
        Portal portal,
        float layerWidth,
        Vec3d posInPlayerCoordinate
    ) {
        generateTriangleSpecialWithOffset(
            vertexOutput, portal, posInPlayerCoordinate,
            Vec3d.ZERO
        );

        generateTriangleSpecialWithOffset(
            vertexOutput, portal, posInPlayerCoordinate,
            portal.getNormal().scale(-layerWidth)
        );
    }
    
    private static void generateTriangleSpecialWithOffset(
        VertexOutput vertexOutput,
        Portal portal,
        Vec3d posInPlayerCoordinate,
        Vec3d offset
    ) {
        SpecialPortalShape specialShape = portal.specialShape;
        
        for (SpecialPortalShape.TriangleInPlane triangle : specialShape.triangles) {
            //the face must be flipped
            putIntoLocalVertex(
                vertexOutput, portal, offset, posInPlayerCoordinate,
                triangle.x1, triangle.y1
            );
            putIntoLocalVertex(
                vertexOutput, portal, offset, posInPlayerCoordinate,
                triangle.x3, triangle.y3
            );
            putIntoLocalVertex(
                vertexOutput, portal, offset, posInPlayerCoordinate,
                triangle.x2, triangle.y2
            );
        }
    }
    
    private static void putIntoLocalVertex(
        VertexOutput vertexOutput,
        Portal portal,
        Vec3d offset,
        Vec3d posInPlayerCoordinate,
        double localX, double localY
    ) {
        vertexOutput.accept(
            posInPlayerCoordinate.x + portal.axisW.x * localX + portal.axisH.x * localY + offset.x,
            posInPlayerCoordinate.y + portal.axisW.y * localX + portal.axisH.y * localY + offset.y,
            posInPlayerCoordinate.z + portal.axisW.z * localX + portal.axisH.z * localY + offset.z
        );
    }
    
    //this does not produce much Vec3d objects
    //too lazy to optimize this
    private static void generateTriangleBiLayered(
        VertexOutput vertexOutput,
        Portal portal,
        float layerWidth,
        Vec3d posInPlayerCoordinate
    ) {
        Vec3d layerOffsest = portal.getNormal().scale(-layerWidth);
        
        Vec3d[] frontFace = Arrays.stream(portal.getFourVerticesRelativeToCenter(0))
            .map(pos -> pos.add(posInPlayerCoordinate))
            .toArray(Vec3d[]::new);
        
        Vec3d[] backFace = Arrays.stream(portal.getFourVerticesRelativeToCenter(0))
            .map(pos -> pos.add(posInPlayerCoordinate).add(layerOffsest))
            .toArray(Vec3d[]::new);
        
        putIntoQuad(
            vertexOutput,
            backFace[0],
            backFace[2],
            backFace[3],
            backFace[1]
        );
        
        putIntoQuad(
            vertexOutput,
            frontFace[0],
            frontFace[2],
            frontFace[3],
            frontFace[1]
        );
    }
    
    static private void putIntoVertexFast(
        BufferBuilder bufferBuilder,
        double x, double y, double z,
        Vec3d fogColor
    ) {
        bufferBuilder
            .pos(x, y, z)
            .color((float) fogColor.x, (float) fogColor.y, (float) fogColor.z, 1.0f)
            .endVertex();
    }
    
    //a d
    //b c
    private static void putIntoQuad(
        VertexOutput vertexOutput,
        Vec3d a,
        Vec3d b,
        Vec3d c,
        Vec3d d
    ) {
        //counter-clockwise triangles are front-faced in default
        
        vertexOutput.accept(b.x, b.y, b.z);
        vertexOutput.accept(c.x, c.y, c.z);
        vertexOutput.accept(d.x, d.y, d.z);
        
        vertexOutput.accept(d.x, d.y, d.z);
        vertexOutput.accept(a.x, a.y, a.z);
        vertexOutput.accept(b.x, b.y, b.z);
        
    }
    
    public static void drawPortalViewTriangle(Portal portal) {
        Minecraft.getInstance().getProfiler().startSection("render_view_triangle");
        
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(portal.dimensionTo);
        
        Vec3d fogColor = helper.getFogColor();
        
        //important
        GlStateManager.enableCull();
        
        //In OpenGL, if you forget to set one rendering state and the result will be abnormal
        //this design is bug-prone (DirectX is better in this aspect)
        RenderHelper.disableStandardItemLighting();
        GlStateManager.color4f(1, 1, 1, 1);
        GlStateManager.disableFog();
        GlStateManager.disableAlphaTest();
        GlStateManager.disableTexture();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();
        
        GL11.glDisable(GL_CLIP_PLANE0);
    
        if (OFInterface.isShaders.getAsBoolean()) {
            fogColor = Vec3d.ZERO;
        }
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        buildPortalViewAreaTrianglesBuffer(
            fogColor,
            portal,
            bufferbuilder,
            PortalRenderer.mc.gameRenderer.getActiveRenderInfo().getProjectedView(),
            MyRenderHelper.partialTicks,
            portal instanceof Mirror ? 0 : 0.45F
        );
        
        tessellator.draw();
        
        GlStateManager.enableCull();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
        GlStateManager.enableLighting();
        
        Minecraft.getInstance().getProfiler().endSection();
    }
    
    private static boolean shouldRenderAdditionalBox(
        Portal portal,
        Vec3d cameraPos
    ) {
        return (portal.getDistanceToPlane(cameraPos) < 0.5) &&
            portal.isPointInPortalProjection(cameraPos);
    }
    
    //this view area rendering method is incorrect
    @Deprecated
    private static void renderAdditionalBox(
        Portal portal,
        Vec3d cameraPos,
        VertexOutput vertexOutput
    ) {
        Vec3d projected = portal.getPointInPortalProjection(cameraPos).subtract(cameraPos);
        Vec3d normal = portal.getNormal();
        
        final double boxRadius = 1;
        final double correctionFactor = 0;
        Vec3d correction = normal.scale(correctionFactor);
        
        Vec3d dx = portal.axisW.scale(boxRadius);
        Vec3d dy = portal.axisH.scale(boxRadius);
        
        Vec3d a = projected.add(dx).add(dy).add(correction);
        Vec3d b = projected.subtract(dx).add(dy).add(correction);
        Vec3d c = projected.subtract(dx).subtract(dy).add(correction);
        Vec3d d = projected.add(dx).subtract(dy).add(correction);
    
        Vec3d mid = projected.add(normal.scale(-0.5));
        
        Consumer<Vec3d> compactVertexOutput = pos -> vertexOutput.accept(pos.x, pos.y, pos.z);
        
        compactVertexOutput.accept(b);
        compactVertexOutput.accept(mid);
        compactVertexOutput.accept(a);
        
        compactVertexOutput.accept(c);
        compactVertexOutput.accept(mid);
        compactVertexOutput.accept(b);
        
        compactVertexOutput.accept(d);
        compactVertexOutput.accept(mid);
        compactVertexOutput.accept(c);
        
        compactVertexOutput.accept(a);
        compactVertexOutput.accept(mid);
        compactVertexOutput.accept(d);
        
    }
}
