package com.qouteall.immersive_portals;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class McHelper {
    public static CompoundNBT writeEntityWithId(Entity entity) {
        return entity.serializeNBT();
    }
    
    @Nullable
    public static Entity readEntity(CompoundNBT tag, World world) {
        return EntityType.byKey(tag.getString("id")).map(
            entityType -> {
                Entity e = entityType.create(world);
                e.read(tag);
                return e;
            }
        ).orElse(null);
    }
}
