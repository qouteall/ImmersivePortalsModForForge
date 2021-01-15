package com.qouteall.hiding_in_the_bushes.mixin.client;

import com.qouteall.hiding_in_the_bushes.ModMainForge;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$ChunkRender$ChunkRenderTask")
public class MixinChunkRenderTask {
    
}
