package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.Comparator;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class FrustumCuller {
    private static enum State {
        none,
        cullingInPortal,
        cullingOutsidePortal
    }
    
    private State currentState;
    private Vec3d[] downLeftUpRightPlaneNormals;
    private Vec3d nearPlanePosInLocalCoordinate;
    private Vec3d nearPlaneNormal;
    
    public FrustumCuller() {
    }
    
    public void update(double cameraX, double cameraY, double cameraZ) {
        if (!CGlobal.doUseAdvancedFrustumCulling) {
            currentState = State.none;
            return;
        }
        
        if (CGlobal.renderer.isRendering()) {
            Portal portal = CGlobal.renderer.getRenderingPortal();
            currentState = State.cullingInPortal;
    
            Vec3d portalOriginInLocalCoordinate = portal.destination.add(
                -cameraX,
                -cameraY,
                -cameraZ
            );
            downLeftUpRightPlaneNormals = getDownLeftUpRightPlaneNormals(
                portalOriginInLocalCoordinate,
                portal.getFourVerticesRelativeToCenter(0)
            );
        }
        else {
            if (!CGlobal.useSuperAdvancedFrustumCulling) {
                currentState = State.none;
                return;
            }
    
            Portal portal = getCurrentNearestVisibleCullablePortal();
            if (portal != null) {
                currentState = State.cullingOutsidePortal;
                
                Vec3d portalOrigin = portal.getPositionVec();
                Vec3d portalOriginInLocalCoordinate = portalOrigin.add(
                    -cameraX,
                    -cameraY,
                    -cameraZ
                );
                downLeftUpRightPlaneNormals = getDownLeftUpRightPlaneNormals(
                    portalOriginInLocalCoordinate,
                    portal.getFourVerticesCullableRelativeToCenter(0)
                );
                nearPlanePosInLocalCoordinate = portalOriginInLocalCoordinate;
                nearPlaneNormal = portal.getNormal().scale(-1);
            }
            else {
                currentState = State.none;
            }
        }
    }
    
    private Vec3d getDownPlane() {
        return downLeftUpRightPlaneNormals[0];
    }
    
    private Vec3d getLeftPlane() {
        return downLeftUpRightPlaneNormals[1];
    }
    
    private Vec3d getUpPlane() {
        return downLeftUpRightPlaneNormals[2];
    }
    
    private Vec3d getRightPlane() {
        return downLeftUpRightPlaneNormals[3];
    }
    
    public boolean canDetermineInvisible(Supplier<AxisAlignedBB> boxInLocalCoordinateSupplier) {
        if (currentState == State.none) {
            return false;
        }
        
        if (OFInterface.isShadowPass.getAsBoolean()) {
            return false;
        }
        
        //TODO implement frustum culling for mirror
        if (MyRenderHelper.isRenderingMirror()) {
            return false;
        }
        
        AxisAlignedBB boxInLocalCoordinate = boxInLocalCoordinateSupplier.get();
        
        Vec3d leftPlane = getLeftPlane();
        Vec3d rightPlane = getRightPlane();
        Vec3d upPlane = getUpPlane();
        Vec3d downPlane = getDownPlane();
        
        if (currentState == State.cullingInPortal) {
            return isFullyOutsideFrustum(
                boxInLocalCoordinate, leftPlane, rightPlane, upPlane, downPlane
            );
        }
        else {
            boolean isBehindNearPlane = testBoxAllTrue(
                boxInLocalCoordinate,
                (x, y, z) -> isInFrontOf(
                    x - nearPlanePosInLocalCoordinate.x,
                    y - nearPlanePosInLocalCoordinate.y,
                    z - nearPlanePosInLocalCoordinate.z,
                    nearPlaneNormal
                )
            );
            
            if (!isBehindNearPlane) {
                return false;
            }
            
            boolean fullyInFrustum = isFullyInFrustum(
                boxInLocalCoordinate, leftPlane, rightPlane, upPlane, downPlane
            );
            return fullyInFrustum;
        }
    }
    
    public Vec3d[] getDownLeftUpRightPlaneNormals(
        Vec3d portalOriginInLocalCoordinate,
        Vec3d[] fourVertices
    ) {
        Vec3d[] relativeVertices = {
            fourVertices[0].add(portalOriginInLocalCoordinate),
            fourVertices[1].add(portalOriginInLocalCoordinate),
            fourVertices[2].add(portalOriginInLocalCoordinate),
            fourVertices[3].add(portalOriginInLocalCoordinate)
        };
        
        //3  2
        //1  0
        return new Vec3d[]{
            relativeVertices[0].crossProduct(relativeVertices[1]),
            relativeVertices[1].crossProduct(relativeVertices[3]),
            relativeVertices[3].crossProduct(relativeVertices[2]),
            relativeVertices[2].crossProduct(relativeVertices[0])
        };
    }
    
    private static enum BatchTestResult {
        all_true,
        all_false,
        both
    }
    
    public static interface PosPredicate {
        boolean test(double x, double y, double z);
    }
    
    private static BatchTestResult testBox(AxisAlignedBB box, PosPredicate predicate) {
        boolean firstResult = predicate.test(box.minX, box.minY, box.minZ);
        if (predicate.test(box.minX, box.minY, box.maxZ) != firstResult) return BatchTestResult.both;
        if (predicate.test(box.minX, box.maxY, box.minZ) != firstResult) return BatchTestResult.both;
        if (predicate.test(box.minX, box.maxY, box.maxZ) != firstResult) return BatchTestResult.both;
        if (predicate.test(box.maxX, box.minY, box.minZ) != firstResult) return BatchTestResult.both;
        if (predicate.test(box.maxX, box.minY, box.maxZ) != firstResult) return BatchTestResult.both;
        if (predicate.test(box.maxX, box.maxY, box.minZ) != firstResult) return BatchTestResult.both;
        if (predicate.test(box.maxX, box.maxY, box.maxZ) != firstResult) return BatchTestResult.both;
        return firstResult ? BatchTestResult.all_true : BatchTestResult.all_false;
    }
    
    private static boolean testBoxAllTrue(AxisAlignedBB box, PosPredicate predicate) {
        if (!predicate.test(box.minX, box.minY, box.minZ)) return false;
        if (!predicate.test(box.minX, box.minY, box.maxZ)) return false;
        if (!predicate.test(box.minX, box.maxY, box.minZ)) return false;
        if (!predicate.test(box.minX, box.maxY, box.maxZ)) return false;
        if (!predicate.test(box.maxX, box.minY, box.minZ)) return false;
        if (!predicate.test(box.maxX, box.minY, box.maxZ)) return false;
        if (!predicate.test(box.maxX, box.maxY, box.minZ)) return false;
        if (!predicate.test(box.maxX, box.maxY, box.maxZ)) return false;
        return true;
    }
    
    private static boolean isInFrontOf(double x, double y, double z, Vec3d planeNormal) {
        return x * planeNormal.x + y * planeNormal.y + z * planeNormal.z > 0;
    }
    
    private static boolean isFullyOutsideFrustum(
        AxisAlignedBB boxInLocalCoordinate,
        Vec3d leftPlane,
        Vec3d rightPlane,
        Vec3d upPlane,
        Vec3d downPlane
    ) {
        BatchTestResult left = testBox(
            boxInLocalCoordinate, (x, y, z) -> isInFrontOf(x, y, z, leftPlane)
        );
        BatchTestResult right = testBox(
            boxInLocalCoordinate, (x, y, z) -> isInFrontOf(x, y, z, rightPlane)
        );
        if (left == BatchTestResult.all_false && right == BatchTestResult.all_true) {
            return true;
        }
        if (left == BatchTestResult.all_true && right == BatchTestResult.all_false) {
            return true;
        }
        
        BatchTestResult up = testBox(
            boxInLocalCoordinate, (x, y, z) -> isInFrontOf(x, y, z, upPlane)
        );
        BatchTestResult down = testBox(
            boxInLocalCoordinate, (x, y, z) -> isInFrontOf(x, y, z, downPlane)
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
        AxisAlignedBB boxInLocalCoordinate,
        Vec3d leftPlane,
        Vec3d rightPlane,
        Vec3d upPlane,
        Vec3d downPlane
    ) {
        return testBoxAllTrue(boxInLocalCoordinate, (x, y, z) -> isInFrontOf(x, y, z, leftPlane)) &&
            testBoxAllTrue(boxInLocalCoordinate, (x, y, z) -> isInFrontOf(x, y, z, rightPlane)) &&
            testBoxAllTrue(boxInLocalCoordinate, (x, y, z) -> isInFrontOf(x, y, z, upPlane)) &&
            testBoxAllTrue(boxInLocalCoordinate, (x, y, z) -> isInFrontOf(x, y, z, downPlane));
    }
    
    private static Portal getCurrentNearestVisibleCullablePortal() {
        Vec3d cameraPos = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView();
        return CHelper.getClientNearbyPortals(10).filter(
            portal -> portal.isInFrontOfPortal(cameraPos)
        ).filter(
            Portal::isCullable
        ).min(
            Comparator.comparingDouble(portal -> portal.getDistanceToNearestPointInPortal(cameraPos))
        ).orElse(null);
    }
}
