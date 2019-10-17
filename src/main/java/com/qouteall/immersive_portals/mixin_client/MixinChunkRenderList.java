package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.exposer.IEChunkRenderList;
import net.minecraft.client.renderer.AbstractChunkRenderContainer;
import net.minecraft.client.renderer.chunk.ChunkRender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(AbstractChunkRenderContainer.class)
public class MixinChunkRenderList implements IEChunkRenderList {
    @Shadow
    private double viewEntityX;
    @Shadow
    private double viewEntityY;
    @Shadow
    private double viewEntityZ;
    
    @Mutable
    @Shadow
    @Final
    protected List<ChunkRender> renderChunks;
    
    @Override
    public void setCameraPos(double x, double y, double z) {
        viewEntityX = x;
        viewEntityY = y;
        viewEntityZ = z;
    }
    
    @Override
    public List<ChunkRender> getChunkRenderers() {
        return renderChunks;
    }
    
    @Override
    public void setChunkRenderers(List<ChunkRender> arg) {
        renderChunks = arg;
    }
}
