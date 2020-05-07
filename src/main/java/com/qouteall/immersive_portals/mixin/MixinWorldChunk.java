package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.ducks.IEWorldChunk;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Chunk.class)
public abstract class MixinWorldChunk implements IEWorldChunk {
    @Final
    @Shadow
    private ClassInheritanceMultiMap<Entity>[] entityLists;

    @Override
    public ClassInheritanceMultiMap<Entity>[] getEntitySections() {
        return entityLists;
    }
}
