package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.world.IWorld;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.*;

public class VoidChunkGenerator extends EndChunkGenerator {
    
    public VoidChunkGenerator(
        IWorld p_i48956_1_,
        BiomeProvider p_i48956_2_,
        EndGenerationSettings p_i48956_3_
    ) {
        super(p_i48956_1_, p_i48956_2_, p_i48956_3_);
    }
    
    @Override
    public void makeBase(IWorld p_222537_1_, IChunk p_222537_2_) {
        //nothing
    }
}
