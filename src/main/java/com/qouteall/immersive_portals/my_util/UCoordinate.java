package com.qouteall.immersive_portals.my_util;

import net.minecraft.entity.Entity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public class UCoordinate {
    public RegistryKey<World> dimension;
    public Vector3d pos;
    
    public UCoordinate(RegistryKey<World> dimension, Vector3d pos) {
        Validate.notNull(dimension);
        Validate.notNull(pos);
        this.dimension = dimension;
        this.pos = pos;
    }
    
    public UCoordinate(Entity entity) {
        this(entity.world.func_234923_W_(), entity.getPositionVec());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UCoordinate that = (UCoordinate) o;
        return dimension.equals(that.dimension) &&
            pos.equals(that.pos);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(dimension, pos);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s %s %s", dimension.func_240901_a_(), pos.x, pos.y, pos.z);
    }
}
