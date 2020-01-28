package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.ImprovedNoiseGenerator;
import net.minecraft.world.gen.feature.structure.Structure;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ChaosBiomeSource extends BiomeProvider {
    private ImprovedNoiseGenerator sampler;
    private Biome[] biomeArray;
    
    public ChaosBiomeSource(long seed) {
        
        sampler = new ImprovedNoiseGenerator(new Random(seed));
        
        Biome[] excluded = {
            Biomes.THE_END,
            Biomes.END_BARRENS,
            Biomes.END_HIGHLANDS,
            Biomes.END_MIDLANDS,
            Biomes.SMALL_END_ISLANDS
        };
        Set<Biome> excludedSet = Arrays.stream(excluded).collect(Collectors.toSet());
        
        biomeArray = Registry.BIOME.stream()
            .filter(biome -> !excludedSet.contains(biome))
            .toArray(Biome[]::new);
    }
    
    
    @Override
    public Biome getBiome(int biomeX, int biomeY) {
        double sampled = sampler.func_215456_a(biomeX, 0, 0, 0, 0);
        double whatever = sampled * 23333;
        double decimal = whatever - Math.floor(whatever);
        
        int biomeNum = biomeArray.length;
        int index = (int) Math.floor(decimal * biomeNum);
        if (index >= biomeNum) {
            index = biomeNum;
        }
        return biomeArray[index];
    }
    
    @Override
    public Biome[] getBiomes(int x, int z, int width, int length, boolean cacheFlag) {
        return new Biome[]{getBiome(x, z)};
    }
    
    @Override
    public Set<Biome> getBiomesInSquare(int centerX, int centerZ, int sideLength) {
        HashSet<Biome> objects = new HashSet<>();
        objects.add(getBiome(centerX, centerZ));
        return objects;
    }
    
    @Nullable
    @Override
    public BlockPos findBiomePosition(
        int x, int z, int range, List<Biome> biomes, Random random
    ) {
        return null;
    }
    
    @Override
    public boolean hasStructure(Structure<?> structureIn) {
        return false;
    }
    
    @Override
    public Set<BlockState> getSurfaceBlocks() {
        return null;
    }
}
