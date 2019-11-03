package com.qouteall.immersive_portals.ducks;

import net.minecraft.world.chunk.AbstractChunkProvider;

public interface IEWorld {
    void setChunkManager(AbstractChunkProvider manager);
}
