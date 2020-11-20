package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.my_util.Plane;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalLike;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;

public class FrontClipping {
    private static final Minecraft client = Minecraft.getInstance();
    private static double[] activeClipPlaneEquation;
    public static boolean isClippingEnabled = false;
    
    public static void disableClipping() {
        GL11.glDisable(GL11.GL_CLIP_PLANE0);
        isClippingEnabled = false;
    }
    
    private static void startClassicalClipping() {
        GL11.glEnable(GL11.GL_CLIP_PLANE0);
        isClippingEnabled = true;
    }
    
    //NOTE the actual culling plane is related to current model view matrix
    public static void setupInnerClipping(
        MatrixStack matrixStack, PortalLike portalLike, boolean doCompensate
    ) {
        if (!CGlobal.useFrontClipping) {
            return;
        }
        
        final Plane clipping = portalLike.getInnerClipping();
        
        if (clipping != null) {
            activeClipPlaneEquation = getClipEquationInner(doCompensate, clipping.pos, clipping.normal);
            
            loadClippingPlaneClassical(matrixStack);
            startClassicalClipping();
        }
        else {
            activeClipPlaneEquation = null;
            disableClipping();
        }
    }
    
    public static boolean isShaderClipping() {
        return OFInterface.isShaders.getAsBoolean() &&
            !RenderDimensionRedirect.isNoShader(
                Minecraft.getInstance().world.func_234923_W_()
            );
    }
    
    private static void loadClippingPlaneClassical(MatrixStack matrixStack) {
        McHelper.runWithTransformation(
            matrixStack,
            () -> {
                GL11.glClipPlane(GL11.GL_CLIP_PLANE0, activeClipPlaneEquation);
            }
        );
    }
    
    private static double[] getClipEquationInner(boolean doCompensate, Vector3d clippingPoint, Vector3d clippingDirection) {
        
        Vector3d cameraPos = McHelper.getCurrentCameraPos();
        
        
        Vector3d planeNormal = clippingDirection;
        
        double correction;
        
        if (doCompensate) {
            correction = clippingPoint.subtract(cameraPos)
                .dotProduct(clippingDirection) / 150.0;
        }
        else {
            correction = 0;
        }
        
        Vector3d portalPos = clippingPoint
            .subtract(planeNormal.scale(correction))//avoid z fighting
            .subtract(cameraPos);
        
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.scale(-1).dotProduct(portalPos);
        
        return new double[]{
            planeNormal.x, planeNormal.y, planeNormal.z, c
        };
    }
    
    public static void setupOuterClipping(MatrixStack matrixStack, PortalLike portalLike) {
        if (!CGlobal.useFrontClipping) {
            return;
        }
        
        if (portalLike instanceof Portal) {
            activeClipPlaneEquation = getClipEquationOuter(((Portal) portalLike));
            if (!isShaderClipping()) {
                loadClippingPlaneClassical(matrixStack);
            }
            startClassicalClipping();
        }
        else {
            disableClipping();
        }
    }
    
    private static double[] getClipEquationOuter(Portal portal) {
        Vector3d planeNormal = portal.getNormal();
        
        Vector3d portalPos = portal.getOriginPos()
            //.subtract(planeNormal.multiply(0.01))//avoid z fighting
            .subtract(client.gameRenderer.getActiveRenderInfo().getProjectedView());
        
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.scale(-1).dotProduct(portalPos);
        
        return new double[]{
            planeNormal.x, planeNormal.y, planeNormal.z, c
        };
    }
    
    public static double[] getActiveClipPlaneEquation() {
        return activeClipPlaneEquation;
    }
}
