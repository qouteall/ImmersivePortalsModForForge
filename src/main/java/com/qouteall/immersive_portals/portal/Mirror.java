package com.qouteall.immersive_portals.portal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class Mirror extends Portal {
    public static EntityType<Mirror> entityType;
    
    public Mirror(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    public void tick() {
        super.tick();
        teleportable = false;
        setInteractable(false);
    }
    
    @Override
    public Vector3d transformLocalVecNonScale(Vector3d localVec) {
        return getMirrored(super.transformLocalVecNonScale(localVec));
    }
    
    @Override
    public boolean canTeleportEntity(Entity entity) {
        return false;
    }
    
    public Vector3d getMirrored(Vector3d vec) {
        double len = vec.dotProduct(getNormal());
        return vec.add(getNormal().scale(len * -2));
    }
    
    @Override
    public Vector3d inverseTransformLocalVecNonScale(Vector3d localVec) {
        return super.inverseTransformLocalVecNonScale(getMirrored(localVec));
    }
}
