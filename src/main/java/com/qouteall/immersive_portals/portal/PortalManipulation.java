package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.RotationHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PortalManipulation {
    public static void removeConnectedPortals(Portal portal, Consumer<Portal> removalInformer) {
        removeOverlappedPortals(
            portal.world,
            portal.getPositionVec(),
            portal.getNormal().scale(-1),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        ServerWorld toWorld = McHelper.getServer().getWorld(portal.dimensionTo);
        removeOverlappedPortals(
            toWorld,
            portal.destination,
            portal.transformLocalVecNonScale(portal.getNormal().scale(-1)),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        removeOverlappedPortals(
            toWorld,
            portal.destination,
            portal.transformLocalVecNonScale(portal.getNormal()),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
    }
    
    public static Portal completeBiWayPortal(Portal portal, EntityType<? extends Portal> entityType) {
        Portal newPortal = createReversePortal(portal, entityType);
    
        McHelper.spawnServerEntityToUnloadedArea(newPortal);
        
        return newPortal;
    }
    
    public static <T extends Portal> T createReversePortal(Portal portal, EntityType<T> entityType) {
        ServerWorld world = McHelper.getServer().getWorld(portal.dimensionTo);
        
        T newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.world.func_234923_W_();
        newPortal.setPosition(portal.destination.x, portal.destination.y, portal.destination.z);
        newPortal.destination = portal.getPositionVec();
        newPortal.specificPlayerId = portal.specificPlayerId;
        
        newPortal.width = portal.width * portal.scaling;
        newPortal.height = portal.height * portal.scaling;
        newPortal.axisW = portal.axisW;
        newPortal.axisH = portal.axisH.scale(-1);
        
        if (portal.specialShape != null) {
            newPortal.specialShape = new GeometryPortalShape();
            initFlippedShape(newPortal, portal.specialShape, portal.scaling);
        }
        
        newPortal.initCullableRange(
            portal.cullableXStart * portal.scaling,
            portal.cullableXEnd * portal.scaling,
            -portal.cullableYStart * portal.scaling,
            -portal.cullableYEnd * portal.scaling
        );
        
        if (portal.rotation != null) {
            rotatePortalBody(newPortal, portal.rotation);
            
            newPortal.rotation = new Quaternion(portal.rotation);
            newPortal.rotation.conjugate();
        }
        
        newPortal.scaling = 1.0 / portal.scaling;
    
        copyAdditionalProperties(newPortal, portal);
        
        return newPortal;
    }
    
    public static void rotatePortalBody(Portal portal, Quaternion rotation) {
        portal.axisW = RotationHelper.getRotated(rotation, portal.axisW);
        portal.axisH = RotationHelper.getRotated(rotation, portal.axisH);
    }
    
    public static Portal completeBiFacedPortal(Portal portal, EntityType<Portal> entityType) {
        Portal newPortal = createFlippedPortal(portal, entityType);
    
        McHelper.spawnServerEntityToUnloadedArea(newPortal);
        
        return newPortal;
    }
    
    public static <T extends Portal> T createFlippedPortal(Portal portal, EntityType<T> entityType) {
        ServerWorld world = (ServerWorld) portal.world;
        T newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.dimensionTo;
        newPortal.setPosition(portal.getPosX(), portal.getPosY(), portal.getPosZ());
        newPortal.destination = portal.destination;
        newPortal.specificPlayerId = portal.specificPlayerId;
        
        newPortal.width = portal.width;
        newPortal.height = portal.height;
        newPortal.axisW = portal.axisW;
        newPortal.axisH = portal.axisH.scale(-1);
        
        if (portal.specialShape != null) {
            newPortal.specialShape = new GeometryPortalShape();
            initFlippedShape(newPortal, portal.specialShape, 1);
        }
        
        newPortal.initCullableRange(
            portal.cullableXStart,
            portal.cullableXEnd,
            -portal.cullableYStart,
            -portal.cullableYEnd
        );
        
        newPortal.rotation = portal.rotation;
        
        newPortal.scaling = portal.scaling;
    
        copyAdditionalProperties(newPortal, portal);
        
        return newPortal;
    }
    
    //the new portal will not be added into world
    public static Portal copyPortal(Portal portal, EntityType<Portal> entityType) {
        ServerWorld world = (ServerWorld) portal.world;
        Portal newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.dimensionTo;
        newPortal.setPosition(portal.getPosX(), portal.getPosY(), portal.getPosZ());
        newPortal.destination = portal.destination;
        newPortal.specificPlayerId = portal.specificPlayerId;
        
        newPortal.width = portal.width;
        newPortal.height = portal.height;
        newPortal.axisW = portal.axisW;
        newPortal.axisH = portal.axisH;
        
        newPortal.specialShape = portal.specialShape;
        
        newPortal.initCullableRange(
            portal.cullableXStart,
            portal.cullableXEnd,
            portal.cullableYStart,
            portal.cullableYEnd
        );
        
        newPortal.rotation = portal.rotation;
        
        newPortal.scaling = portal.scaling;
    
        copyAdditionalProperties(newPortal, portal);
        
        return newPortal;
    }
    
    private static void initFlippedShape(Portal newPortal, GeometryPortalShape specialShape, double scale) {
        newPortal.specialShape.triangles = specialShape.triangles.stream()
            .map(triangle -> new GeometryPortalShape.TriangleInPlane(
                triangle.x1 * scale,
                -triangle.y1 * scale,
                triangle.x2 * scale,
                -triangle.y2 * scale,
                triangle.x3 * scale,
                -triangle.y3 * scale
            )).collect(Collectors.toList());
    }
    
    public static void completeBiWayBiFacedPortal(
        Portal portal, Consumer<Portal> removalInformer,
        Consumer<Portal> addingInformer, EntityType<Portal> entityType
    ) {
        removeOverlappedPortals(
            ((ServerWorld) portal.world),
            portal.getPositionVec(),
            portal.getNormal().scale(-1),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        
        Portal oppositeFacedPortal = completeBiFacedPortal(portal, entityType);
        removeOverlappedPortals(
            McHelper.getServer().getWorld(portal.dimensionTo),
            portal.destination,
            portal.transformLocalVecNonScale(portal.getNormal().scale(-1)),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        
        Portal r1 = completeBiWayPortal(portal, entityType);
        removeOverlappedPortals(
            McHelper.getServer().getWorld(oppositeFacedPortal.dimensionTo),
            oppositeFacedPortal.destination,
            oppositeFacedPortal.transformLocalVecNonScale(oppositeFacedPortal.getNormal().scale(-1)),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        
        Portal r2 = completeBiWayPortal(oppositeFacedPortal, entityType);
        addingInformer.accept(oppositeFacedPortal);
        addingInformer.accept(r1);
        addingInformer.accept(r2);
    }
    
    public static void removeOverlappedPortals(
        World world,
        Vector3d pos,
        Vector3d normal,
        Predicate<Portal> predicate,
        Consumer<Portal> informer
    ) {
        getPortalClutter(world, pos, normal, predicate).forEach(e -> {
            e.remove();
            informer.accept(e);
        });
    }
    
    public static List<Portal> getPortalClutter(
        World world,
        Vector3d pos,
        Vector3d normal,
        Predicate<Portal> predicate
    ) {
        return world.getEntitiesWithinAABB(
            Portal.class,
            new AxisAlignedBB(
                pos.add(0.5, 0.5, 0.5),
                pos.subtract(0.5, 0.5, 0.5)
            ),
            p -> p.getNormal().dotProduct(normal) > 0.5 && predicate.test(p)
        );
    }
    
    public static <T extends Portal> T createOrthodoxPortal(
        EntityType<T> entityType,
        ServerWorld fromWorld, ServerWorld toWorld,
        Direction facing, AxisAlignedBB portalArea,
        Vector3d destination
    ) {
        T portal = entityType.create(fromWorld);
        
        Tuple<Direction, Direction> directions = Helper.getPerpendicularDirections(facing);
        
        Vector3d areaSize = Helper.getBoxSize(portalArea);
        
        AxisAlignedBB boxSurface = Helper.getBoxSurface(portalArea, facing);
        Vector3d center = boxSurface.getCenter();
        portal.setPosition(center.x, center.y, center.z);
        portal.destination = destination;
        
        portal.axisW = Vector3d.func_237491_b_(directions.getA().getDirectionVec());
        portal.axisH = Vector3d.func_237491_b_(directions.getB().getDirectionVec());
        portal.width = Helper.getCoordinate(areaSize, directions.getA().getAxis());
        portal.height = Helper.getCoordinate(areaSize, directions.getB().getAxis());
        
        portal.dimensionTo = toWorld.func_234923_W_();
        
        return portal;
    }
    
    public static void copyAdditionalProperties(Portal to, Portal from) {
        to.extension.motionAffinity = from.extension.motionAffinity;
        to.teleportable = from.teleportable;
        to.teleportChangesScale = from.teleportChangesScale;
        to.specificPlayerId = from.specificPlayerId;
        to.extension.adjustPositionAfterTeleport = from.extension.adjustPositionAfterTeleport;
    }
}
