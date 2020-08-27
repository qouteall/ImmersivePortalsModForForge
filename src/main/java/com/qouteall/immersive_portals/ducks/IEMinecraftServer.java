package com.qouteall.immersive_portals.ducks;

import net.minecraft.util.FrameTimer;

public interface IEMinecraftServer {
    public FrameTimer getMetricsDataNonClientOnly();
    
    boolean portal_getAreAllWorldsLoaded();
}
