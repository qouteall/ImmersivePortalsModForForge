package com.qouteall.immersive_portals.portal;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Mirror extends Portal {
    public static EntityType<Mirror> entityType;
    
    
    public Mirror(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    public Vec3d getContentDirection() {
        return getNormal();
    }
    
    @Override
    public boolean isTeleportable() {
        return false;
    }
    
    @Override
    public void move(MoverType typeIn, Vec3d pos) {
        //mirror cannot be moved
    }
}
