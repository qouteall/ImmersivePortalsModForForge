package com.qouteall.immersive_portals.optifine_compatibility;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Method;

public class OFBuiltChunkNeighborFix {
    private static Method method_setRenderChunkNeighbour;
    
    public static void init() {
        method_setRenderChunkNeighbour = Helper.noError(() ->
            ChunkRenderDispatcher.ChunkRender.class
                .getDeclaredMethod(
                    "setRenderChunkNeighbour",
                    Direction.class,
                    ChunkRenderDispatcher.ChunkRender.class
                )
        );
    }
    
    public static void updateNeighbor(
        MyBuiltChunkStorage storage,
        ChunkRenderDispatcher.ChunkRender[] chunks
    ) {
        if (!OFInterface.isOptifinePresent) {
            return;
        }
        
        Minecraft.getInstance().getProfiler().startSection("neighbor");
        
        try {
            for (int l = 0; l < Direction.values().length; ++l) {
                Direction facing = Direction.values()[l];
                for (int i = 0, chunksLength = chunks.length; i < chunksLength; i++) {
                    ChunkRenderDispatcher.ChunkRender renderChunk = chunks[i];
                    BlockPos neighborPos = renderChunk.getBlockPosOffset16(facing);
                    ChunkRenderDispatcher.ChunkRender neighbour =
                        storage.myGetRenderChunkRaw(neighborPos, chunks);
                    method_setRenderChunkNeighbour.invoke(
                        renderChunk, facing, neighbour
                    );
                }
            }
        }
        catch (Throwable e) {
            throw new IllegalStateException(e);
        }
        
        Minecraft.getInstance().getProfiler().endSection();
    }
}
