package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.immersive_portals.McHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

public class EndFloorPortal extends GlobalTrackedPortal {
    public static EntityType<EndFloorPortal> entityType;
    
    public EndFloorPortal(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    public static void enableFloor() {
        ServerWorld endWorld = McHelper.getServer().getWorld(DimensionType.THE_END);
        GlobalPortalStorage storage = GlobalPortalStorage.get(endWorld);
        
        boolean isEndFloorPresent = storage.data.stream()
            .anyMatch(
                portal -> portal instanceof EndFloorPortal
            );
        
        if (!isEndFloorPresent) {
            EndFloorPortal endFloorPortal = new EndFloorPortal(entityType, endWorld);
            
            endFloorPortal.setPosition(0, 0, 0);
            endFloorPortal.destination = new Vec3d(0, 256, 0);
            endFloorPortal.dimensionTo = DimensionType.OVERWORLD;
            endFloorPortal.axisW = new Vec3d(0, 0, 1);
            endFloorPortal.axisH = new Vec3d(1, 0, 0);
            endFloorPortal.width = 23333;
            endFloorPortal.height = 23333;
            
            endFloorPortal.loadFewerChunks = false;
            
            storage.data.add(endFloorPortal);
            
            storage.onDataChanged();
        }
    }
    
    public static void removeFloor() {
        ServerWorld endWorld = McHelper.getServer().getWorld(DimensionType.THE_END);
        GlobalPortalStorage storage = GlobalPortalStorage.get(endWorld);
        
        storage.data.removeIf(
            portal -> portal instanceof EndFloorPortal
        );
        
        storage.onDataChanged();
    }
}
