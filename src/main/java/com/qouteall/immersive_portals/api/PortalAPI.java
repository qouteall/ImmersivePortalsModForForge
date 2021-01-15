package com.qouteall.immersive_portals.api;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.DQuaternion;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalManipulation;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import javax.annotation.Nullable;

public class PortalAPI {
    
    public static void setPortalPositionShape(
        Portal portal,
        Vector3d position,
        DQuaternion orientation,
        double width, double height
    ) {
        portal.setOriginPos(position);
        portal.setSquareShape(
            orientation.rotate(new Vector3d(1, 0, 0)),
            orientation.rotate(new Vector3d(0, 1, 0)),
            width, height
        );
    }
    
    public static void setPortalOrthodoxShape(Portal portal, Direction facing, AxisAlignedBB portalArea) {
        Tuple<Direction, Direction> directions = Helper.getPerpendicularDirections(facing);
        
        Vector3d areaSize = Helper.getBoxSize(portalArea);
        
        AxisAlignedBB boxSurface = Helper.getBoxSurface(portalArea, facing);
        Vector3d center = boxSurface.getCenter();
        portal.setPosition(center.x, center.y, center.z);
        
        portal.axisW = Vector3d.func_237491_b_(directions.getA().getDirectionVec());
        portal.axisH = Vector3d.func_237491_b_(directions.getB().getDirectionVec());
        portal.width = Helper.getCoordinate(areaSize, directions.getA().getAxis());
        portal.height = Helper.getCoordinate(areaSize, directions.getB().getAxis());
    }
    
    public static void setPortalTransformation(
        Portal portal,
        RegistryKey<World> destinationDimension,
        Vector3d destinationPosition,
        @Nullable DQuaternion rotation,
        double scale
    ) {
        portal.setDestinationDimension(destinationDimension);
        portal.setDestination(destinationPosition);
        portal.setRotationTransformation(rotation.toMcQuaternion());
        portal.setScaleTransformation(scale);
    }
    
    public static void spawnServerEntity(Entity entity) {
        McHelper.spawnServerEntity(entity);
    }
    
    public static <T extends Portal> T createReversePortal(T portal) {
        return (T) PortalManipulation.createReversePortal(
            portal, (EntityType<? extends Portal>) portal.getType()
        );
    }
    
    public static <T extends Portal> T createFlippedPortal(T portal) {
        return (T) PortalManipulation.createFlippedPortal(
            portal, (EntityType<? extends Portal>) portal.getType()
        );
    }
    
    public static <T extends Portal> T copyPortal(Portal portal, EntityType<T> entityType) {
        return (T) PortalManipulation.copyPortal(portal, (EntityType<Portal>) entityType);
    }
    
    public static void addGlobalPortal(
        ServerWorld world, Portal portal
    ) {
        GlobalPortalStorage.get(world).addPortal(portal);
    }
    
    public static void removeGlobalPortal(
        ServerWorld world, Portal portal
    ) {
        GlobalPortalStorage.get(world).removePortal(portal);
    }
}
