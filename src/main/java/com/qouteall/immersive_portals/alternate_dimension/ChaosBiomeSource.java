package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.util.FastRandom;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.FuzzedBiomeMagnifier;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.SimplexNoiseGenerator;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class ChaosBiomeSource extends BiomeProvider {
    private SimplexNoiseGenerator sampler1;
    private SimplexNoiseGenerator sampler2;
    private Biome[] biomeArray;
    private long worldSeed;
    
    public ChaosBiomeSource(long seed) {
        super(Registry.BIOME.stream().collect(Collectors.toSet()));
        
        worldSeed = seed;
        
        sampler1 = new SimplexNoiseGenerator(new Random(seed));
        sampler2 = new SimplexNoiseGenerator(new Random(seed + 1));
        
        Biome[] excluded = {
//            Biomes.THE_END,
//            Biomes.END_BARRENS,
//            Biomes.END_HIGHLANDS,
//            Biomes.END_MIDLANDS,
//            Biomes.SMALL_END_ISLANDS
        };
        Set<Biome> excludedSet = Arrays.stream(excluded).collect(Collectors.toSet());
        
        biomeArray = Registry.BIOME.stream()
            .filter(biome -> !excludedSet.contains(biome))
            .toArray(Biome[]::new);
    }
    
    private Biome getRandomBiome(int x, int z) {
        int biomeNum = biomeArray.length;
        
        int index = (Math.abs((int) FastRandom.mix(x, z))) % biomeNum;
        return biomeArray[(int) index];
    }
    
    @Override
    public Biome getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        return FuzzedBiomeMagnifier.INSTANCE.getBiome(
            worldSeed, biomeX / 2, 0, biomeZ / 2,
            (x, y, z) -> getRandomBiome(x, z)
        );
    }
}
