package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.ducks.IEBuiltChunk;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkRenderDispatcher.ChunkRender.class)
public abstract class MixinBuiltChunk implements IEBuiltChunk {
    
    @Shadow
    protected abstract void stopCompileTask();
    
    @Override
    public void fullyReset() {
        stopCompileTask();
    }
}
