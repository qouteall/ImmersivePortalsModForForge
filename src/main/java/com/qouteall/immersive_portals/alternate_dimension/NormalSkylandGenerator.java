package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.EndChunkGenerator;
import net.minecraft.world.gen.EndGenerationSettings;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.structure.Structure;

public class NormalSkylandGenerator extends EndChunkGenerator {
    public NormalSkylandGenerator(
        IWorld iWorld,
        BiomeProvider biomeSource,
        EndGenerationSettings floatingIslandsChunkGeneratorConfig
    ) {
        super(iWorld, biomeSource, floatingIslandsChunkGeneratorConfig);
    }
    
    @Override
    public boolean hasStructure(
        Biome biome, Structure<? extends IFeatureConfig> structureFeature
    ) {
        if (structureFeature == Structure.MINESHAFT) {
            //no mineshaft
            return false;
        }
        return super.hasStructure(biome, structureFeature);
    }
    
    //make end city and woodland mansion be able to generate
    @Override
    public int func_222529_a(int x, int z, Heightmap.Type heightmapType) {
        return 64;
    }
}
