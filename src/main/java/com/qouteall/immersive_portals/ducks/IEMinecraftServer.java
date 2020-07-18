package com.qouteall.immersive_portals.ducks;

import net.minecraft.server.IDynamicRegistries;
import net.minecraft.util.FrameTimer;

public interface IEMinecraftServer {
    public FrameTimer getMetricsDataNonClientOnly();
    
    boolean portal_getAreAllWorldsLoaded();
    
    IDynamicRegistries.Impl portal_getDimensionTracker();
}
