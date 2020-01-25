package com.qouteall.immersive_portals.ducks;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

public interface IEWorldRendererChunkInfo {
    ChunkRenderDispatcher.ChunkRender getBuiltChunk();
}
