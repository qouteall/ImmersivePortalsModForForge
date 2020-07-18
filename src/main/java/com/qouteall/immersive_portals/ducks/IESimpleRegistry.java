package com.qouteall.immersive_portals.ducks;

import net.minecraft.util.RegistryKey;

public interface IESimpleRegistry {
    void markUnloaded(RegistryKey<?> key);
}
