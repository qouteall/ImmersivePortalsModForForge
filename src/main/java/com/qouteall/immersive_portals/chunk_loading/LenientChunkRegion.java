package com.qouteall.immersive_portals.chunk_loading;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.server.ServerWorld;
import java.util.ArrayList;
import java.util.List;

// Lenient means getBlockState does not crash if out of bound
public class LenientChunkRegion extends WorldGenRegion {
    
    public LenientChunkRegion(ServerWorld world, List<IChunk> chunks) {
        super(world, chunks);
    }
    
    static LenientChunkRegion createLenientChunkRegion(
        DimensionalChunkPos center, int radius, ServerWorld world
    ) {
        List<IChunk> chunks = new ArrayList<>();
    
        for (int z = center.z - radius; z <= center.z + radius; z++) {
            for (int x = center.x - radius; x <= center.x + radius; x++) {
                chunks.add(world.getChunk(x, z));
            }
        }
    
        return new LenientChunkRegion(
            world, chunks
        );
    }
    
    @Override
    public BlockState getBlockState(BlockPos pos) {
        final IChunk chunk = this.getChunk(
            pos.getX() >> 4, pos.getZ() >> 4,
            ChunkStatus.FULL, false
        );
        if (chunk == null) {
            return Blocks.AIR.getDefaultState();
        }
        return chunk.getBlockState(pos);
    }
}
