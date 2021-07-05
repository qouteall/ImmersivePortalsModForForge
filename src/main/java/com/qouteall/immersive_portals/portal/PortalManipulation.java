package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.api.PortalAPI;
import com.qouteall.immersive_portals.my_util.DQuaternion;
import com.qouteall.immersive_portals.my_util.RotationHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PortalManipulation {
    public static void setPortalTransformation(
        Portal portal,
        RegistryKey<World> destDim,
        Vector3d destPos,
        @Nullable Quaternion rotation,
        double scale
    ) {
        portal.dimensionTo = destDim;
        portal.setDestination(destPos);
        portal.rotation = rotation;
        portal.scaling = scale;
        portal.updateCache();
    }
    
    public static void removeConnectedPortals(Portal portal, Consumer<Portal> removalInformer) {
        removeOverlappedPortals(
            portal.world,
            portal.getOriginPos(),
            portal.getNormal().scale(-1),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        ServerWorld toWorld = McHelper.getServer().getWorld(portal.dimensionTo);
        removeOverlappedPortals(
            toWorld,
            portal.getDestPos(),
            portal.transformLocalVecNonScale(portal.getNormal().scale(-1)),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        removeOverlappedPortals(
            toWorld,
            portal.getDestPos(),
            portal.transformLocalVecNonScale(portal.getNormal()),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
    }
    
    public static Portal completeBiWayPortal(Portal portal, EntityType<? extends Portal> entityType) {
        Portal newPortal = createReversePortal(portal, entityType);
        
        McHelper.spawnServerEntity(newPortal);
        
        return newPortal;
    }
    
    // can also be used in client
    public static <T extends Portal> T createReversePortal(Portal portal, EntityType<T> entityType) {
        World world = portal.getDestinationWorld();
        
        T newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.world.func_234923_W_();
        newPortal.setPosition(portal.getDestPos().x, portal.getDestPos().y, portal.getDestPos().z);
        newPortal.setDestination(portal.getOriginPos());
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
        
        McHelper.spawnServerEntity(newPortal);
        
        return newPortal;
    }
    
    public static <T extends Portal> T createFlippedPortal(Portal portal, EntityType<T> entityType) {
        World world = portal.world;
        T newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.dimensionTo;
        newPortal.setPosition(portal.getPosX(), portal.getPosY(), portal.getPosZ());
        newPortal.setDestination(portal.getDestPos());
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
        World world = portal.world;
        Portal newPortal = entityType.create(world);
        newPortal.dimensionTo = portal.dimensionTo;
        newPortal.setPosition(portal.getPosX(), portal.getPosY(), portal.getPosZ());
        newPortal.setDestination(portal.getDestPos());
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
            portal.world,
            portal.getOriginPos(),
            portal.getNormal().scale(-1),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        
        Portal oppositeFacedPortal = completeBiFacedPortal(portal, entityType);
        removeOverlappedPortals(
            McHelper.getServer().getWorld(portal.dimensionTo),
            portal.getDestPos(),
            portal.transformLocalVecNonScale(portal.getNormal().scale(-1)),
            p -> Objects.equals(p.specificPlayerId, portal.specificPlayerId),
            removalInformer
        );
        
        Portal r1 = completeBiWayPortal(portal, entityType);
        removeOverlappedPortals(
            McHelper.getServer().getWorld(oppositeFacedPortal.dimensionTo),
            oppositeFacedPortal.getDestPos(),
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
                pos.add(0.1, 0.1, 0.1),
                pos.subtract(0.1, 0.1, 0.1)
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
        
        PortalAPI.setPortalOrthodoxShape(portal, facing, portalArea);
        
        portal.setDestination(destination);
        portal.dimensionTo = toWorld.func_234923_W_();
        
        return portal;
    }
    
    public static void copyAdditionalProperties(Portal to, Portal from) {
        to.teleportable = from.teleportable;
        to.teleportChangesScale = from.teleportChangesScale;
        to.specificPlayerId = from.specificPlayerId;
        PortalExtension.get(to).motionAffinity = PortalExtension.get(from).motionAffinity;
        PortalExtension.get(to).adjustPositionAfterTeleport = PortalExtension.get(from).adjustPositionAfterTeleport;
        to.portalTag = from.portalTag;
        to.hasCrossPortalCollision = from.hasCrossPortalCollision;
        to.commandsOnTeleported = from.commandsOnTeleported;
    }
    
    public static void createScaledBoxView(
        ServerWorld areaWorld, AxisAlignedBB area,
        ServerWorld boxWorld, Vector3d boxBottomCenter,
        double scale,
        boolean biWay,
        boolean teleportChangesScale,
        boolean outerFuseView,
        boolean outerRenderingMergable,
        boolean innerRenderingMergable,
        boolean hasCrossPortalCollision
    ) {
        Vector3d viewBoxSize = Helper.getBoxSize(area).scale(1.0 / scale);
        AxisAlignedBB viewBox = Helper.getBoxByBottomPosAndSize(boxBottomCenter, viewBoxSize);
        for (Direction direction : Direction.values()) {
            Portal portal = createOrthodoxPortal(
                Portal.entityType,
                boxWorld, areaWorld,
                direction, Helper.getBoxSurface(viewBox, direction),
                Helper.getBoxSurface(area, direction).getCenter()
            );
            portal.scaling = scale;
            portal.teleportChangesScale = teleportChangesScale;
            portal.fuseView = outerFuseView;
            portal.renderingMergable = outerRenderingMergable;
            portal.hasCrossPortalCollision = hasCrossPortalCollision;
            PortalExtension.get(portal).adjustPositionAfterTeleport = true;
            
            McHelper.spawnServerEntity(portal);
            
            if (biWay) {
                Portal reversePortal = createReversePortal(portal, Portal.entityType);
                
                reversePortal.renderingMergable = innerRenderingMergable;
                
                McHelper.spawnServerEntity(reversePortal);
                
            }
        }
    }
    
    /**
     * Places a portal based on {@code entity}'s looking direction. Does not set the portal destination or add it to the
     * world, you will have to do that yourself.
     *
     * @param width  The width of the portal.
     * @param height The height of the portal.
     * @param entity The entity to place this portal as.
     * @return The placed portal, with no destination set.
     * @author LoganDark
     */
    public static Portal placePortal(double width, double height, Entity entity) {
        Vector3d playerLook = entity.getLookVec();
        
        Tuple<BlockRayTraceResult, List<Portal>> rayTrace =
            Helper.rayTrace(
                entity.world,
                new RayTraceContext(
                    entity.getEyePosition(1.0f),
                    entity.getEyePosition(1.0f).add(playerLook.scale(100.0)),
                    RayTraceContext.BlockMode.OUTLINE,
                    RayTraceContext.FluidMode.NONE,
                    entity
                ),
                true
            );
        
        BlockRayTraceResult hitResult = rayTrace.getA();
        List<Portal> hitPortals = rayTrace.getB();
        
        if (Helper.hitResultIsMissedOrNull(hitResult)) {
            return null;
        }
        
        for (Portal hitPortal : hitPortals) {
            playerLook = hitPortal.transformLocalVecNonScale(playerLook);
        }
        
        Direction lookingDirection = Helper.getFacingExcludingAxis(
            playerLook,
            hitResult.getFace().getAxis()
        );
        
        // this should never happen...
        if (lookingDirection == null) {
            return null;
        }
        
        Vector3d axisH = Vector3d.func_237491_b_(hitResult.getFace().getDirectionVec());
        Vector3d axisW = axisH.crossProduct(Vector3d.func_237491_b_(lookingDirection.getOpposite().getDirectionVec()));
        Vector3d pos = Vector3d.func_237489_a_(hitResult.getPos())
            .add(axisH.scale(0.5 + height / 2));
        
        World world = hitPortals.isEmpty()
            ? entity.world
            : hitPortals.get(hitPortals.size() - 1).getDestinationWorld();
        
        Portal portal = new Portal(Portal.entityType, world);
        
        portal.setRawPosition(pos.x, pos.y, pos.z);
        
        portal.axisW = axisW;
        portal.axisH = axisH;
        
        portal.width = width;
        portal.height = height;
        
        return portal;
    }

    public static DQuaternion getPortalOrientationQuaternion(
            Vector3d axisW, Vector3d axisH
    ) {
        Vector3d normal = axisW.crossProduct(axisH);

        return DQuaternion.matrixToQuaternion(axisW, axisH, normal);
    }

    public static void setPortalOrientationQuaternion(
            Portal portal, DQuaternion quaternion
    ) {
        portal.setOrientation(
                quaternion.rotate(new Vector3d(1, 0, 0)),
                quaternion.rotate(new Vector3d(0, 1, 0))
        );
    }

    public static void adjustRotationToConnect(Portal portalA, Portal portalB) {
        DQuaternion a = PortalAPI.getPortalOrientationQuaternion(portalA);
        DQuaternion b = PortalAPI.getPortalOrientationQuaternion(portalB);

        DQuaternion delta = b.hamiltonProduct(a.getConjugated());

        DQuaternion flip = DQuaternion.rotationByDegrees(
                portalB.axisH, 180
        );
        DQuaternion aRot = flip.hamiltonProduct(delta);

        portalA.setRotationTransformation(aRot.toMcQuaternion());
        portalB.setRotationTransformation(aRot.getConjugated().toMcQuaternion());

    }

}
