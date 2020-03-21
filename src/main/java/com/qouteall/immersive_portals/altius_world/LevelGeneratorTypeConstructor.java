package com.qouteall.immersive_portals.altius_world;

import net.minecraft.world.WorldType;

public interface LevelGeneratorTypeConstructor {
    WorldType construct(int id, String name, String storedName, int version);
}
