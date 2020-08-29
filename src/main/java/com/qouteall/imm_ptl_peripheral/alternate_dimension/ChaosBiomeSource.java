package com.qouteall.imm_ptl_peripheral.alternate_dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.FastRandom;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryLookupCodec;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.FuzzedBiomeMagnifier;
import net.minecraft.world.biome.provider.BiomeProvider;
import java.util.stream.Collectors;

public class ChaosBiomeSource extends BiomeProvider {
    public static Codec<ChaosBiomeSource> codec = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("seed").forGetter(o -> o.worldSeed),
        RegistryLookupCodec.func_244331_a(Registry.field_239720_u_).forGetter(o -> o.biomeRegistry)
    ).apply(instance, instance.stable(ChaosBiomeSource::new)));
    
    private long worldSeed;
    private Registry<Biome> biomeRegistry;
    
    public ChaosBiomeSource(long seed, Registry<Biome> biomeRegistry) {
        super(biomeRegistry.stream().collect(Collectors.toList()));
        
        worldSeed = seed;
        this.biomeRegistry = biomeRegistry;
    }
    
    private Biome getRandomBiome(int x, int z) {
        int biomeNum = biomes.size();
        
        int index = (Math.abs((int) FastRandom.mix(x, z))) % biomeNum;
        return biomes.get(index);
    }
    
    @Override
    public Biome getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        return FuzzedBiomeMagnifier.INSTANCE.getBiome(
            worldSeed, biomeX / 2, 0, biomeZ / 2,
            (x, y, z) -> getRandomBiome(x, z)
        );
    }
    
    @Override
    protected Codec<? extends BiomeProvider> func_230319_a_() {
        return codec;
    }
    
    @Override
    public BiomeProvider func_230320_a_(long seed) {
        return new ChaosBiomeSource(seed, biomeRegistry);
    }
}
