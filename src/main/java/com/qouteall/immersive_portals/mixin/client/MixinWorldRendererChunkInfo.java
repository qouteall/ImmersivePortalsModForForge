package com.qouteall.immersive_portals.mixin.client;

import com.qouteall.immersive_portals.ducks.IEWorldRendererChunkInfo;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.renderer.WorldRenderer$LocalRenderInformationContainer")
public class MixinWorldRendererChunkInfo implements IEWorldRendererChunkInfo {
    @Shadow
    @Final
    private ChunkRenderDispatcher.ChunkRender renderChunk;
    
    @Override
    public ChunkRenderDispatcher.ChunkRender getBuiltChunk() {
        return renderChunk;
    }
}
