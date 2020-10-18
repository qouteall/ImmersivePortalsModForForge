package com.qouteall.immersive_portals.teleportation;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.List;
import java.util.function.Function;

public class CollisionHelper {
    
    //cut a box with a plane
    //the facing that normal points to will be remained
    //return null for empty box
    private static AxisAlignedBB clipBox(AxisAlignedBB box, Vector3d planePos, Vector3d planeNormal) {
        
        boolean xForward = planeNormal.x > 0;
        boolean yForward = planeNormal.y > 0;
        boolean zForward = planeNormal.z > 0;
        
        Vector3d pushedPos = new Vector3d(
            xForward ? box.minX : box.maxX,
            yForward ? box.minY : box.maxY,
            zForward ? box.minZ : box.maxZ
        );
        Vector3d staticPos = new Vector3d(
            xForward ? box.maxX : box.minX,
            yForward ? box.maxY : box.minY,
            zForward ? box.maxZ : box.minZ
        );
        
        double tOfPushedPos = Helper.getCollidingT(planePos, planeNormal, pushedPos, planeNormal);
        boolean isPushedPosInFrontOfPlane = tOfPushedPos < 0;
        if (isPushedPosInFrontOfPlane) {
            //the box is not cut by plane
            return box;
        }
        boolean isStaticPosInFrontOfPlane = Helper.isInFrontOfPlane(
            staticPos, planePos, planeNormal
        );
        if (!isStaticPosInFrontOfPlane) {
            //the box is fully cut by plane
            return null;
        }
        
        //the plane cut the box halfly
        Vector3d afterBeingPushed = pushedPos.add(planeNormal.scale(tOfPushedPos));
        return new AxisAlignedBB(afterBeingPushed, staticPos);
    }
    
    public static boolean canCollideWithPortal(Entity entity, Portal portal, float tickDelta) {
        if (portal.canTeleportEntity(entity)) {
            Vector3d cameraPosVec = entity.getEyePosition(tickDelta);
            if (portal.isInFrontOfPortal(cameraPosVec) &&
                portal.isPointInPortalProjection(cameraPosVec)) {
                return true;
            }
        }
        return false;
    }
    
    public static Vector3d handleCollisionHalfwayInPortal(
        Entity entity,
        Vector3d attemptedMove,
        Portal collidingPortal,
        Function<Vector3d, Vector3d> handleCollisionFunc
    ) {
        AxisAlignedBB originalBoundingBox = entity.getBoundingBox();
        
        Vector3d thisSideMove = getThisSideMove(
            entity, attemptedMove, collidingPortal,
            handleCollisionFunc, originalBoundingBox
        );
        
        Vector3d otherSideMove = getOtherSideMove(
            entity, attemptedMove, collidingPortal,
            handleCollisionFunc, originalBoundingBox
        );
        
        //handle stepping onto slab or stair through portal
        if (attemptedMove.y < 0) {
            if (otherSideMove.y > 0) {
                //stepping on the other side
                return new Vector3d(
                    absMin(thisSideMove.x, otherSideMove.x),
                    otherSideMove.y,
                    absMin(thisSideMove.z, otherSideMove.z)
                );
            }
            else if (thisSideMove.y > 0) {
                //stepping on this side
                //re-calc collision with intact collision box
                //the stepping is shorter using the clipped collision box
                Vector3d newThisSideMove = handleCollisionFunc.apply(attemptedMove);
                
                //apply the stepping move for the other side
                Vector3d newOtherSideMove = getOtherSideMove(
                    entity, newThisSideMove, collidingPortal,
                    handleCollisionFunc, originalBoundingBox
                );
                
                return newOtherSideMove;
            }
        }
        
        return new Vector3d(
            absMin(thisSideMove.x, otherSideMove.x),
            absMin(thisSideMove.y, otherSideMove.y),
            absMin(thisSideMove.z, otherSideMove.z)
        );
    }
    
    private static double absMin(double a, double b) {
        return Math.abs(a) < Math.abs(b) ? a : b;
    }
    
    private static Vector3d getOtherSideMove(
        Entity entity,
        Vector3d attemptedMove,
        Portal collidingPortal,
        Function<Vector3d, Vector3d> handleCollisionFunc,
        AxisAlignedBB originalBoundingBox
    ) {
        Vector3d transformedAttemptedMove = collidingPortal.transformLocalVec(attemptedMove);
        
        AxisAlignedBB boxOtherSide = getCollisionBoxOtherSide(
            collidingPortal,
            originalBoundingBox,
            transformedAttemptedMove
        );
        if (boxOtherSide == null) {
            return attemptedMove;
        }

//        if (collidingPortal.hasScaling() || collidingPortal.rotation != null) {
//            boxOtherSide = boxOtherSide.expand(0.05);
//        }
        
        //switch world and check collision
        World oldWorld = entity.world;
        Vector3d oldPos = entity.getPositionVec();
        Vector3d oldLastTickPos = McHelper.lastTickPosOf(entity);
        
        entity.world = collidingPortal.getDestinationWorld();
        entity.setBoundingBox(boxOtherSide);
        
        Vector3d collided = handleCollisionFunc.apply(transformedAttemptedMove);
        Vector3d result = collidingPortal.inverseTransformLocalVec(collided);
        
        entity.world = oldWorld;
        McHelper.setPosAndLastTickPos(entity, oldPos, oldLastTickPos);
        entity.setBoundingBox(originalBoundingBox);
        
        double finalX = correctCoordinate(attemptedMove.x, result.x);
        double finalY = correctCoordinate(attemptedMove.y, result.y);
        double finalZ = correctCoordinate(attemptedMove.z, result.z);
        
        return new Vector3d(finalX, finalY, finalZ);
    }
    
    // floating point deviation may cause collision issues
    private static double correctCoordinate(double attemptedMove, double result) {
        //rotation may cause a free move to reduce a little bit and the game think that it's collided
        if (Math.abs(attemptedMove - result) < 0.001) {
            return attemptedMove;
        }
        
        //0 may become 0.0000001 after rotation. avoid falling through floor
        if (Math.abs(result) < 0.0001) {
            return 0;
        }
        
        //1 may become 0.999999 after rotation. avoid going into wall
        return result * 0.999;
    }
    
    private static Vector3d getThisSideMove(
        Entity entity,
        Vector3d attemptedMove,
        Portal collidingPortal,
        Function<Vector3d, Vector3d> handleCollisionFunc,
        AxisAlignedBB originalBoundingBox
    ) {
        AxisAlignedBB boxThisSide = getCollisionBoxThisSide(
            collidingPortal, originalBoundingBox, attemptedMove
        );
        if (boxThisSide == null) {
            return attemptedMove;
        }
        
        entity.setBoundingBox(boxThisSide);
        Vector3d move1 = handleCollisionFunc.apply(attemptedMove);
        
        entity.setBoundingBox(originalBoundingBox);
        
        return move1;
    }
    
    private static AxisAlignedBB getCollisionBoxThisSide(
        Portal portal,
        AxisAlignedBB originalBox,
        Vector3d attemptedMove
    ) {
        //cut the collision box a little bit more for horizontal portals
        //because the box will be stretched by attemptedMove when calculating collision
        Vector3d cullingPos = portal.getPositionVec().subtract(attemptedMove);
        return clipBox(
            originalBox,
            cullingPos,
            portal.getNormal()
        );
    }
    
    private static AxisAlignedBB getCollisionBoxOtherSide(
        Portal portal,
        AxisAlignedBB originalBox,
        Vector3d attemptedMove
    ) {
        AxisAlignedBB otherSideBox = transformBox(portal, originalBox);
        final AxisAlignedBB box = clipBox(
            otherSideBox,
            portal.destination,
            portal.getContentDirection()
        );
        
        if (box == null) {
            return clipBox(
                otherSideBox,
                portal.destination.subtract(attemptedMove),
                portal.getContentDirection()
            );
        }
        
        return box;
    }
    
    private static AxisAlignedBB transformBox(Portal portal, AxisAlignedBB originalBox) {
        if (portal.rotation == null && portal.scaling == 1) {
            return originalBox.offset(portal.destination.subtract(portal.getPositionVec()));
        }
        else {
            return Helper.transformBox(originalBox, portal::transformPoint);
        }
    }
    
    public static World getWorld(boolean isClient, RegistryKey<World> dimension) {
        if (isClient) {
            return CHelper.getClientWorld(dimension);
        }
        else {
            return McHelper.getServer().getWorld(dimension);
        }
    }
    
    public static boolean isCollidingWithAnyPortal(Entity entity) {
        return ((IEEntity) entity).getCollidingPortal() != null;
    }
    
    public static AxisAlignedBB getActiveCollisionBox(Entity entity) {
        Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
        if (collidingPortal != null) {
            AxisAlignedBB thisSideBox = getCollisionBoxThisSide(
                collidingPortal,
                entity.getBoundingBox(),
                Vector3d.ZERO //is it ok?
            );
            if (thisSideBox != null) {
                return thisSideBox;
            }
            else {
                return new AxisAlignedBB(0, 0, 0, 0, 0, 0);
            }
        }
        else {
            return entity.getBoundingBox();
        }
    }
    
    private static void updateGlobalPortalCollidingPortalForWorld(World world) {
        world.getProfiler().startSection("global_portal_colliding_portal");
        
        List<GlobalTrackedPortal> globalPortals = McHelper.getGlobalPortals(world);
        Iterable<Entity> worldEntityList = McHelper.getWorldEntityList(world);
        
        for (Entity entity : worldEntityList) {
            AxisAlignedBB entityBoundingBoxStretched = getStretchedBoundingBox(entity);
            for (GlobalTrackedPortal globalPortal : globalPortals) {
                AxisAlignedBB globalPortalBoundingBox = globalPortal.getBoundingBox();
                if (entityBoundingBoxStretched.intersects(globalPortalBoundingBox)) {
                    if (canCollideWithPortal(entity, globalPortal, 1)) {
                        ((IEEntity) entity).notifyCollidingWithPortal(globalPortal);
                    }
                }
            }
        }
        
        world.getProfiler().endSection();
    }
    
    public static void init() {
        ModMain.postServerTickSignal.connect(() -> {
            for (ServerWorld world : McHelper.getServer().getWorlds()) {
                updateGlobalPortalCollidingPortalForWorld(world);
            }
        });
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void initClient() {
        ModMain.postClientTickSignal.connect(CollisionHelper::updateClientGlobalPortalCollidingPortal);
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void updateClientGlobalPortalCollidingPortal() {
        for (ClientWorld world : CGlobal.clientWorldLoader.clientWorldMap.values()) {
            updateGlobalPortalCollidingPortalForWorld(world);
        }
    }
    
    public static void notifyCollidingPortals(Portal portal) {
        if (!portal.isInteractable()) {
            return;
        }
        
        AxisAlignedBB portalBoundingBox = portal.getBoundingBox();
        final double compensation = 3;
        int xMin = (int) Math.floor(portalBoundingBox.minX - compensation);
        int yMin = (int) Math.floor(portalBoundingBox.minY - compensation);
        int zMin = (int) Math.floor(portalBoundingBox.minZ - compensation);
        int xMax = (int) Math.ceil(portalBoundingBox.maxX + compensation);
        int yMax = (int) Math.ceil(portalBoundingBox.maxY + compensation);
        int zMax = (int) Math.ceil(portalBoundingBox.maxZ + compensation);
        
        List<Entity> collidingEntities = McHelper.findEntities(
            Entity.class,
            McHelper.getChunkAccessor(portal.world),
            xMin >> 4,
            xMax >> 4,
            Math.max(0, yMin >> 4),
            Math.min(15, yMax >> 4),
            zMin >> 4,
            zMax >> 4,
            entity -> {
                if (entity instanceof Portal) {
                    return false;
                }
                AxisAlignedBB entityBoxStretched = getStretchedBoundingBox(entity);
                if (!entityBoxStretched.intersects(portalBoundingBox)) {
                    return false;
                }
                return canCollideWithPortal(entity, portal, 1);
            }
        );
        
        for (Entity entity : collidingEntities) {
            ((IEEntity) entity).notifyCollidingWithPortal(portal);
        }
    }
    
    private static AxisAlignedBB getStretchedBoundingBox(Entity entity) {
        return entity.getBoundingBox().expand(entity.getMotion());
    }
}
