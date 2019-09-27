package com.qouteall.immersive_portals.exposer;

import net.minecraft.world.chunk.AbstractChunkProvider;

public interface IEWorld {
    void setChunkManager(AbstractChunkProvider manager);
}
