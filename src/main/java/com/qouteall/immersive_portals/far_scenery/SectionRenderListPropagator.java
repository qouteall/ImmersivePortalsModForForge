package com.qouteall.immersive_portals.far_scenery;

import com.google.common.collect.Queues;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;

public class SectionRenderListPropagator {
    public static List<ChunkRenderDispatcher.ChunkRender> getRenderSectionList(
        MyBuiltChunkStorage chunks,
        BlockPos cameraPos,
        int renderDistance,
        Predicate<ChunkRenderDispatcher.ChunkRender> isInFrustum,
        int uniqueInt,
        Predicate<ChunkRenderDispatcher.ChunkRender> filter
    ) {
        List<ChunkRenderDispatcher.ChunkRender> result = new ArrayList<>();
        
        ChunkRenderDispatcher.ChunkRender starting = chunks.myGetRenderChunkRaw(
            cameraPos,
            chunks.renderChunks
        );
        
        Predicate<ChunkRenderDispatcher.ChunkRender> visitAndGetIsNewlyVisiting =
            builtChunk -> builtChunk.setFrameIndex(uniqueInt);
        
        visitAndGetIsNewlyVisiting.test(starting);
        
        Queue<ChunkRenderDispatcher.ChunkRender> queue = Queues.newArrayDeque();
        queue.add(starting);
        
        while (!queue.isEmpty()) {
            ChunkRenderDispatcher.ChunkRender curr = queue.poll();
            if (filter.test(curr)) {
                result.add(curr);
            }
            
            for (Direction direction : Direction.values()) {
                ChunkRenderDispatcher.ChunkRender adjacentChunk = getAdjacentChunk(
                    cameraPos, curr, direction, renderDistance, chunks
                );
                
                if (adjacentChunk != null) {
                    if (visitAndGetIsNewlyVisiting.test(adjacentChunk)) {
                        if (isInFrustum.test(curr)) {
                            queue.add(adjacentChunk);
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    private static ChunkRenderDispatcher.ChunkRender getAdjacentChunk(
        BlockPos cameraPos,
        ChunkRenderDispatcher.ChunkRender chunk,
        Direction direction,
        int renderDistance,
        MyBuiltChunkStorage chunks
    ) {
        BlockPos neighborPos = chunk.getBlockPosOffset16(direction);
        if (MathHelper.abs(cameraPos.getX() - neighborPos.getX()) > renderDistance * 16) {
            return null;
        }
        else if (neighborPos.getY() >= 0 && neighborPos.getY() < 256) {
            if (MathHelper.abs(cameraPos.getZ() - neighborPos.getZ()) > renderDistance * 16) {
                return null;
            }
            else {
                return chunks.myGetRenderChunkRaw(neighborPos, chunks.renderChunks);
            }
        }
        else {
            return null;
        }
    }
}
