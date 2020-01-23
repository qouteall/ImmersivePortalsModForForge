package com.qouteall.immersive_portals.ducks;

import net.minecraft.util.FrameTimer;

public interface IEMinecraftServer {
    boolean portal_getAreAllWorldsLoaded();
    
    public FrameTimer getMetricsDataNonClientOnly();
}
