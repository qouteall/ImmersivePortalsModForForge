package com.qouteall.immersive_portals.alternate_dimension;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.ImprovedNoiseGenerator;
import net.minecraft.world.gen.feature.structure.Structure;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ChaosBiomeSource extends BiomeProvider {
    private ImprovedNoiseGenerator sampler;
    private Biome[] biomeArray;
    
    protected final Map<Structure<?>, Boolean> structureFeatures = Maps.newHashMap();
    protected final Set<BlockState> topMaterials = Sets.newHashSet();
    
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
        Biome[] abiome = new Biome[width * length];
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < length; dz++) {
                Biome newBiome = getBiome(x + dx, z + dz);
                Validate.notNull(newBiome);
                abiome[dx * width + dz] = newBiome;
            }
        }
        return abiome;
    }
    
    @Override
    public Set<Biome> getBiomesInSquare(int centerX, int centerZ, int sideLength) {
        return Arrays.stream(biomeArray).collect(Collectors.toSet());
    }
    
    @Nullable
    @Override
    public BlockPos findBiomePosition(
        int x, int z, int range, List<Biome> biomes, Random random
    ) {
        int i = x - range >> 2;
        int j = z - range >> 2;
        int k = x + range >> 2;
        int l = z + range >> 2;
        int m = k - i + 1;
        int n = l - j + 1;
        int o = 0 >> 2;
        BlockPos blockPos = null;
        int p = 0;
    
        for (int q = 0; q < n; ++q) {
            for (int r = 0; r < m; ++r) {
                int s = i + r;
                int t = j + q;
                if (biomes.contains(this.getBiome(s, t))) {
                    if (blockPos == null || random.nextInt(p + 1) == 0) {
                        blockPos = new BlockPos(s << 2, 0, t << 2);
                    }
                
                    ++p;
                }
            }
        }
    
        return blockPos;
    }
    
    @Override
    public boolean hasStructure(Structure<?> structureIn) {
        return (Boolean) this.structureFeatures.computeIfAbsent(structureIn, (structureFeature) -> {
            return Arrays.stream(this.biomeArray).anyMatch((biome) -> {
                return biome.hasStructure(structureFeature);
            });
        });
    }
    
    @Override
    public Set<BlockState> getSurfaceBlocks() {
        if (this.topMaterials.isEmpty()) {
            Iterator var1 = Arrays.stream(biomeArray).iterator();
        
            while (var1.hasNext()) {
                Biome biome = (Biome) var1.next();
                this.topMaterials.add(biome.getSurfaceBuilderConfig().getTop());
            }
        }
    
        return this.topMaterials;
    }
}
