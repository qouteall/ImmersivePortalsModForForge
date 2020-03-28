package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntBox;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class BorderPortal extends GlobalTrackedPortal {
    public static EntityType<BorderPortal> entityType;
    
    public BorderPortal(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    public static void setBorderPortal(
        ServerWorld world,
        int x1, int y1,
        int x2, int y2
    ) {
        GlobalPortalStorage storage = GlobalPortalStorage.get(world);
        
        storage.data.removeIf(
            portal -> portal instanceof BorderPortal
        );
        
        IntBox box = new IntBox(
            new BlockPos(x1, 0, y1),
            new BlockPos(x2, 256, y2)
        ).getSorted();
        AxisAlignedBB area = box.toRealNumberBox();
        
        storage.data.add(createWrappingPortal(world, area, Direction.NORTH));
        storage.data.add(createWrappingPortal(world, area, Direction.SOUTH));
        storage.data.add(createWrappingPortal(world, area, Direction.WEST));
        storage.data.add(createWrappingPortal(world, area, Direction.EAST));
        
        storage.onDataChanged();
    }
    
    public static void removeBorderPortal(
        ServerWorld world
    ) {
        GlobalPortalStorage storage = GlobalPortalStorage.get(world);
        
        storage.data.removeIf(
            portal -> portal instanceof BorderPortal
        );
        
        storage.onDataChanged();
    }
    
    private static BorderPortal createWrappingPortal(
        ServerWorld serverWorld,
        AxisAlignedBB area,
        Direction direction
    ) {
        BorderPortal portal = new BorderPortal(entityType, serverWorld);
        
        Vec3d areaSize = Helper.getBoxSize(area);
        
        Tuple<Direction, Direction> axises = Helper.getPerpendicularDirections(
            direction
        );
        AxisAlignedBB boxSurface = Helper.getBoxSurface(area, direction);
        Vec3d center = boxSurface.getCenter();
        AxisAlignedBB oppositeSurface = Helper.getBoxSurface(area, direction.getOpposite());
        Vec3d destination = oppositeSurface.getCenter();
        portal.setPosition(center.x, center.y, center.z);
        portal.destination = destination;
        
        portal.axisW = new Vec3d(axises.getA().getDirectionVec());
        portal.axisH = new Vec3d(axises.getB().getDirectionVec());
        portal.width = Helper.getCoordinate(areaSize, axises.getA().getAxis());
        portal.height = Helper.getCoordinate(areaSize, axises.getB().getAxis());
        
        portal.dimensionTo = serverWorld.dimension.getType();
        
        return portal;
    }
}
