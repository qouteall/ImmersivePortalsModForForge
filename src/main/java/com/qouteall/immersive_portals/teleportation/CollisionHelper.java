package com.qouteall.immersive_portals.teleportation;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class CollisionHelper {
    
    //cut a box with a plane
    //the facing that normal points to will be remained
    //return null for empty box
    private static AxisAlignedBB clipBox(AxisAlignedBB box, Vec3d planePos, Vec3d planeNormal) {
        
        boolean xForward = planeNormal.x > 0;
        boolean yForward = planeNormal.y > 0;
        boolean zForward = planeNormal.z > 0;
        
        Vec3d pushedPos = new Vec3d(
            xForward ? box.minX : box.maxX,
            yForward ? box.minY : box.maxY,
            zForward ? box.minZ : box.maxZ
        );
        Vec3d staticPos = new Vec3d(
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
        Vec3d afterBeingPushed = pushedPos.add(planeNormal.scale(tOfPushedPos));
        return new AxisAlignedBB(afterBeingPushed, staticPos);
    }
    
    private static boolean shouldCollideWithPortal(Entity entity, Portal portal) {
        Vec3d eyePosition = entity.getEyePosition(1);
        return portal.isTeleportable() &&
            portal.isInFrontOfPortal(eyePosition) &&
            portal.isPointInPortalProjection(eyePosition);
    }
    
    public static Vec3d handleCollisionHalfwayInPortal1(
        Entity entity,
        Vec3d attemptedMove,
        Entity collidingPortal,
        Function<Vec3d, Vec3d> handleCollisionFunc
    ) {
        return handleCollisionHalfwayInPortal(
            entity, attemptedMove,
            (Portal) collidingPortal,
            handleCollisionFunc
        );
    }
    
    public static Vec3d handleCollisionHalfwayInPortal(
        Entity entity,
        Vec3d attemptedMove,
        Portal collidingPortal,
        Function<Vec3d, Vec3d> handleCollisionFunc
    ) {
        AxisAlignedBB originalBoundingBox = entity.getBoundingBox();
        
        Vec3d move1 = getThisSideMove(
            entity, attemptedMove, collidingPortal,
            handleCollisionFunc, originalBoundingBox
        );
        
        Vec3d move2 = getOtherSideMove(
            entity, attemptedMove, collidingPortal,
            handleCollisionFunc, originalBoundingBox
        );
        
        return new Vec3d(
            Math.abs(move1.x) < Math.abs(move2.x) ? move1.x : move2.x,
            Math.abs(move1.y) < Math.abs(move2.y) ? move1.y : move2.y,
            Math.abs(move1.z) < Math.abs(move2.z) ? move1.z : move2.z
        );
    }
    
    private static Vec3d getOtherSideMove(
        Entity entity,
        Vec3d attemptedMove,
        Portal collidingPortal,
        Function<Vec3d, Vec3d> handleCollisionFunc,
        AxisAlignedBB originalBoundingBox
    ) {
        AxisAlignedBB boxOtherSide = getCollisionBoxOtherSide(
            collidingPortal, originalBoundingBox, attemptedMove
        );
        if (boxOtherSide == null) {
            return attemptedMove;
        }
    
        //switch world and check collision
        World oldWorld = entity.world;
        Vec3d oldPos = entity.getPositionVec();
        Vec3d oldLastTickPos = Helper.lastTickPosOf(entity);
    
        entity.world = getWorld(entity.world.isRemote, collidingPortal.dimensionTo);
        entity.setBoundingBox(boxOtherSide);
    
        Vec3d move2 = handleCollisionFunc.apply(attemptedMove);
        
        entity.world = oldWorld;
        McHelper.setPosAndLastTickPos(entity, oldPos, oldLastTickPos);
        entity.setBoundingBox(originalBoundingBox);
        
        return move2;
    }
    
    private static Vec3d getThisSideMove(
        Entity entity,
        Vec3d attemptedMove,
        Portal collidingPortal,
        Function<Vec3d, Vec3d> handleCollisionFunc,
        AxisAlignedBB originalBoundingBox
    ) {
        AxisAlignedBB boxThisSide = getCollisionBoxThisSide(
            collidingPortal, originalBoundingBox, attemptedMove
        );
        if (boxThisSide == null) {
            return attemptedMove;
        }
        
        entity.setBoundingBox(boxThisSide);
        Vec3d move1 = handleCollisionFunc.apply(attemptedMove);
    
        entity.setBoundingBox(originalBoundingBox);
    
        return move1;
    }
    
    private static AxisAlignedBB getCollisionBoxThisSide(
        Portal portal,
        AxisAlignedBB originalBox,
        Vec3d attemptedMove
    ) {
        //cut the collision box a little bit more for horizontal portals
        //because the box will be stretched by attemptedMove when calculating collision
//        Vec3d cullingPos = portal.getNormal().y > 0.5 ?
//            portal.getPositionVec().add(portal.getNormal().scale(0.5)) :
//            portal.getPositionVec();
        Vec3d cullingPos = portal.getPositionVec().subtract(attemptedMove);
        return clipBox(
            originalBox,
            cullingPos,
            portal.getNormal()
        );
    }
    
    private static AxisAlignedBB getCollisionBoxOtherSide(
        Portal portal,
        AxisAlignedBB originalBox,
        Vec3d attemptedMove
    ) {
        Vec3d teleportation = portal.destination.subtract(portal.getPositionVec());
        return clipBox(
            originalBox.offset(teleportation),
            portal.destination.subtract(attemptedMove),
            portal.getNormal().scale(-1)
        );
    }
    
    public static World getWorld(boolean isClient, DimensionType dimension) {
        if (isClient) {
            return CHelper.getClientWorld(dimension);
        }
        else {
            return McHelper.getServer().getWorld(dimension);
        }
    }
    
    //world.getEntities is not reliable
    //it has a small chance to ignore collided entities
    //this would cause player to fall through floor when halfway though portal
    //use entity.getCollidingPortal() and do not use this
    public static Portal getCollidingPortalUnreliable(Entity entity) {
        AxisAlignedBB box = entity.getBoundingBox();
        return getCollidingPortalRough(entity, box).filter(
            portal -> shouldCollideWithPortal(
                entity, portal
            )
        ).min(
            Comparator.comparingDouble(p -> p.posY)
        ).orElse(null);
    }
    
    public static Stream<Portal> getCollidingPortalRough(Entity entity, AxisAlignedBB box) {
        World world = entity.world;
        
        List<GlobalTrackedPortal> globalPortals = McHelper.getGlobalPortals(world);
        
        Stream<Portal> normalPortals = world.getEntitiesWithinAABB(
            Portal.class, box, e -> true
        ).stream();
        
        if (globalPortals == null) {
            return normalPortals;
        }
        
        return Streams.concat(
            normalPortals,
            globalPortals.stream()
                .filter(
                    p -> p.getBoundingBox().grow(0.5).intersects(box)
                )
        );
    }
    
    public static boolean isCollidingWithAnyPortal(Entity entity) {
        return ((IEEntity) entity).getCollidingPortal() != null;
    }
    
    public static boolean isNearbyPortal(Entity entity) {
        return !entity.world.getEntitiesWithinAABB(
            Portal.class,
            entity.getBoundingBox().grow(1),
            e -> true
        ).isEmpty();
    }
    
    public static AxisAlignedBB getActiveCollisionBox(Entity entity) {
        Portal collidingPortal = (Portal) ((IEEntity) entity).getCollidingPortal();
        if (collidingPortal != null) {
            AxisAlignedBB thisSideBox = getCollisionBoxThisSide(
                collidingPortal,
                entity.getBoundingBox(),
                Vec3d.ZERO
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
}
