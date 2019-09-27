package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.exposer.IEChunkRenderList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import net.minecraft.client.renderer.AbstractChunkRenderContainer;
import net.minecraft.client.renderer.chunk.ChunkRender;

@Mixin(AbstractChunkRenderContainer.class)
public class MixinChunkRenderList implements IEChunkRenderList {
    @Shadow
    private double cameraX;
    @Shadow
    private double cameraY;
    @Shadow
    private double cameraZ;
    
    @Mutable
    @Shadow
    @Final
    protected List<ChunkRender> chunkRenderers;
    
    @Override
    public void setCameraPos(double x, double y, double z) {
        cameraX = x;
        cameraY = y;
        cameraZ = z;
    }
    
    @Override
    public List<ChunkRender> getChunkRenderers() {
        return chunkRenderers;
    }
    
    @Override
    public void setChunkRenderers(List<ChunkRender> arg) {
        chunkRenderers = arg;
    }
}
