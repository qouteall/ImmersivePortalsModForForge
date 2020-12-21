package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.my_util.BoxPredicate;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Comparator;

@OnlyIn(Dist.CLIENT)
public class FrustumCuller {
    
    
    private BoxPredicate canDetermineInvisibleFunc;
    
    public FrustumCuller() {
    }
    
    public void update(double cameraX, double cameraY, double cameraZ) {
        canDetermineInvisibleFunc = getCanDetermineInvisibleFunc(cameraX, cameraY, cameraZ);
    }
    
    public boolean canDetermineInvisible(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ
    ) {
        return canDetermineInvisibleFunc.test(
            minX, minY, minZ, maxX, maxY, maxZ
        );
    }
    
    private BoxPredicate getCanDetermineInvisibleFunc(
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        if (!CGlobal.doUseAdvancedFrustumCulling) {
            return BoxPredicate.nonePredicate;
        }
        
        if (PortalRendering.isRendering()) {
            return PortalRendering.getRenderingPortal().getInnerFrustumCullingFunc(cameraX, cameraY, cameraZ);
        }
        else {
            if (!CGlobal.useSuperAdvancedFrustumCulling) {
                return BoxPredicate.nonePredicate;
            }
            
            Portal portal = getCurrentNearestVisibleCullablePortal();
            if (portal != null) {
                
                Vector3d portalOrigin = portal.getOriginPos();
                Vector3d portalOriginInLocalCoordinate = portalOrigin.add(
                    -cameraX, -cameraY, -cameraZ
                );
                final Vector3d[] outerFrustumCullingVertices = portal.getOuterFrustumCullingVertices();
                if (outerFrustumCullingVertices == null) {
                    return BoxPredicate.nonePredicate;
                }
                Vector3d[] downLeftUpRightPlaneNormals = getDownLeftUpRightPlaneNormals(
                    portalOriginInLocalCoordinate,
                    outerFrustumCullingVertices
                );
                
                Vector3d downPlane = downLeftUpRightPlaneNormals[0];
                Vector3d leftPlane = downLeftUpRightPlaneNormals[1];
                Vector3d upPlane = downLeftUpRightPlaneNormals[2];
                Vector3d rightPlane = downLeftUpRightPlaneNormals[3];
                
                Vector3d nearPlanePosInLocalCoordinate = portalOriginInLocalCoordinate;
                Vector3d nearPlaneNormal = portal.getNormal().scale(-1);
                
                return
                    (double minX, double minY, double minZ, double maxX, double maxY, double maxZ) -> {
                        boolean isBehindNearPlane = testBoxTwoVertices(
                            minX, minY, minZ, maxX, maxY, maxZ,
                            nearPlaneNormal.x, nearPlaneNormal.y, nearPlaneNormal.z,
                            nearPlanePosInLocalCoordinate.x,
                            nearPlanePosInLocalCoordinate.y,
                            nearPlanePosInLocalCoordinate.z
                        ) == BatchTestResult.all_true;
                        
                        if (!isBehindNearPlane) {
                            return false;
                        }
                        
                        boolean fullyInFrustum = isFullyInFrustum(
                            minX, minY, minZ, maxX, maxY, maxZ,
                            leftPlane, rightPlane, upPlane, downPlane
                        );
                        return fullyInFrustum;
                    };
            }
            else {
                return BoxPredicate.nonePredicate;
            }
        }
    }
    
    public static Vector3d[] getDownLeftUpRightPlaneNormals(
        Vector3d portalOriginInLocalCoordinate,
        Vector3d[] fourVertices
    ) {
        Vector3d[] relativeVertices = {
            fourVertices[0].add(portalOriginInLocalCoordinate),
            fourVertices[1].add(portalOriginInLocalCoordinate),
            fourVertices[2].add(portalOriginInLocalCoordinate),
            fourVertices[3].add(portalOriginInLocalCoordinate)
        };
        
        //3  2
        //1  0
        return new Vector3d[]{
            relativeVertices[0].crossProduct(relativeVertices[1]),
            relativeVertices[1].crossProduct(relativeVertices[3]),
            relativeVertices[3].crossProduct(relativeVertices[2]),
            relativeVertices[2].crossProduct(relativeVertices[0])
        };
    }
    
    public static enum BatchTestResult {
        all_true,
        all_false,
        both
    }
    
    public static BatchTestResult testBoxTwoVertices(
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        double planeNormalX, double planeNormalY, double planeNormalZ,
        double planePosX, double planePosY, double planePosZ
    ) {
        double p1x;
        double p1y;
        double p1z;
        double p2x;
        double p2y;
        double p2z;
        
        if (planeNormalX > 0) {
            p1x = minX;
            p2x = maxX;
        }
        else {
            p1x = maxX;
            p2x = minX;
        }
        
        if (planeNormalY > 0) {
            p1y = minY;
            p2y = maxY;
        }
        else {
            p1y = maxY;
            p2y = minY;
        }
        
        if (planeNormalZ > 0) {
            p1z = minZ;
            p2z = maxZ;
        }
        else {
            p1z = maxZ;
            p2z = minZ;
        }
        
        boolean r1 = isInFrontOf(
            p1x - planePosX, p1y - planePosY, p1z - planePosZ,
            planeNormalX, planeNormalY, planeNormalZ
        );
        
        boolean r2 = isInFrontOf(
            p2x - planePosX, p2y - planePosY, p2z - planePosZ,
            planeNormalX, planeNormalY, planeNormalZ
        );
        
        if (r1 && r2) {
            return BatchTestResult.all_true;
        }
        
        if ((!r1) && (!r2)) {
            return BatchTestResult.all_false;
        }
        
        return BatchTestResult.both;
    }
    
    public static BatchTestResult testBoxTwoVertices(
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        Vector3d planeNormal
    ) {
        return testBoxTwoVertices(
            minX, minY, minZ, maxX, maxY, maxZ,
            planeNormal.x, planeNormal.y, planeNormal.z,
            0, 0, 0
        );
    }
    
    private static boolean isInFrontOf(double x, double y, double z, Vector3d planeNormal) {
        return x * planeNormal.x + y * planeNormal.y + z * planeNormal.z >= 0;
    }
    
    private static boolean isInFrontOf(
        double x, double y, double z,
        double planeNormalX, double planeNormalY, double planeNormalZ
    ) {
        return x * planeNormalX + y * planeNormalY + z * planeNormalZ >= 0;
    }
    
    public static boolean isFullyOutsideFrustum(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
        Vector3d leftPlane,
        Vector3d rightPlane,
        Vector3d upPlane,
        Vector3d downPlane
    ) {
        BatchTestResult left = testBoxTwoVertices(
            minX, minY, minZ, maxX, maxY, maxZ, leftPlane
        );
        BatchTestResult right = testBoxTwoVertices(
            minX, minY, minZ, maxX, maxY, maxZ, rightPlane
        );
        if (left == BatchTestResult.all_false && right == BatchTestResult.all_true) {
            return true;
        }
        if (left == BatchTestResult.all_true && right == BatchTestResult.all_false) {
            return true;
        }
        
        BatchTestResult up = testBoxTwoVertices(
            minX, minY, minZ, maxX, maxY, maxZ, upPlane
        );
        BatchTestResult down = testBoxTwoVertices(
            minX, minY, minZ, maxX, maxY, maxZ, downPlane
        );
        if (up == BatchTestResult.all_false && down == BatchTestResult.all_true) {
            return true;
        }
        if (up == BatchTestResult.all_true && down == BatchTestResult.all_false) {
            return true;
        }
        
        return false;
    }
    
    private static boolean isFullyInFrustum(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
        Vector3d leftPlane,
        Vector3d rightPlane,
        Vector3d upPlane,
        Vector3d downPlane
    ) {
        return testBoxTwoVertices(minX, minY, minZ, maxX, maxY, maxZ, leftPlane)
            == BatchTestResult.all_true
            && testBoxTwoVertices(minX, minY, minZ, maxX, maxY, maxZ, rightPlane)
            == BatchTestResult.all_true
            && testBoxTwoVertices(minX, minY, minZ, maxX, maxY, maxZ, upPlane)
            == BatchTestResult.all_true
            && testBoxTwoVertices(minX, minY, minZ, maxX, maxY, maxZ, downPlane)
            == BatchTestResult.all_true;
    }
    
    private static Portal getCurrentNearestVisibleCullablePortal() {
        Vector3d cameraPos = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView();
        return CHelper.getClientNearbyPortals(16).filter(
            portal -> portal.isInFrontOfPortal(cameraPos)
        ).filter(
            Portal::canDoOuterFrustumCulling
        ).min(
            Comparator.comparingDouble(portal -> portal.getDistanceToNearestPointInPortal(cameraPos))
        ).orElse(null);
    }
    
    public static boolean isTouchingInsideContentArea(Portal renderingPortal, AxisAlignedBB boundingBox) {
        Vector3d planeNormal = renderingPortal.getContentDirection();
        Vector3d planePos = renderingPortal.getDestPos();
        BatchTestResult batchTestResult = testBoxTwoVertices(
            boundingBox.minX, boundingBox.minY, boundingBox.minZ,
            boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ,
            planeNormal.x, planeNormal.y, planeNormal.z,
            planePos.x, planePos.y, planePos.z
        );
        return batchTestResult != BatchTestResult.all_false;
    }
}
