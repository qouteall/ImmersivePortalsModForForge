package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.exposer.IEWorldRendererChunkInfo;
import net.minecraft.client.renderer.chunk.ChunkRender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.renderer.WorldRenderer$LocalRenderInformationContainer", remap = false)
public class MixinWorldRendererChunkInfo implements IEWorldRendererChunkInfo {
    
    @Shadow
    @Final
    private ChunkRender renderChunk;
    
    @Override
    public ChunkRender getChunkRenderer() {
        return renderChunk;
    }
}
