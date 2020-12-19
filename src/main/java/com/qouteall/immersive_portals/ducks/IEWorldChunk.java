package com.qouteall.immersive_portals.ducks;

import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;


public interface IEWorldChunk {
    ClassInheritanceMultiMap<Entity>[] portal_getEntitySections();
}
