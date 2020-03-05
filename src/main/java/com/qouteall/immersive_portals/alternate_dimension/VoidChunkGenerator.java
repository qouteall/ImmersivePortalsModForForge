package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.world.IWorld;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.EndChunkGenerator;
import net.minecraft.world.gen.EndGenerationSettings;

public class VoidChunkGenerator extends EndChunkGenerator {
    public VoidChunkGenerator(
        IWorld iWorld,
        BiomeProvider biomeSource,
        EndGenerationSettings floatingIslandsChunkGeneratorConfig
    ) {
        super(iWorld, biomeSource, floatingIslandsChunkGeneratorConfig);
    }
    
    @Override
    public void makeBase(IWorld world, IChunk chunk) {
        //nothing
    }
}
