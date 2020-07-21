package com.qouteall.immersive_portals.alternate_dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityClassification;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.OverworldBiomeProvider;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.DimensionSettings;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.NoiseChunkGenerator;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import java.util.List;

public class NormalSkylandGenerator extends ChunkGenerator {
    
    public static final Codec<NormalSkylandGenerator> codec = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("seed").stable().forGetter(g -> g.worldSeed)
        ).apply(instance, instance.stable(NormalSkylandGenerator::new))
    );
    
    private long worldSeed;
    private final NoiseChunkGenerator proxy;
    
    public NormalSkylandGenerator(
        long seed
    ) {
        super(
            new OverworldBiomeProvider(seed, false, false),
            new DimensionStructuresSettings(true)
        );
        
        worldSeed = seed;
        
        proxy = new NoiseChunkGenerator(
            this.getBiomeProvider(),
            seed,
            new DimensionSettings.Preset("floating_islands", (preset) -> {
                return DimensionSettings.Preset.func_236134_a_(
                    new DimensionStructuresSettings(false),
                    Blocks.STONE.getDefaultState(),
                    Blocks.WATER.getDefaultState(),
                    preset,
                    false,
                    false
                );
            }).func_236137_b_()
        );
    }
    
    @Override
    protected Codec<? extends ChunkGenerator> func_230347_a_() {
        return codec;
    }
    
    @Override
    public ChunkGenerator func_230349_a_(long seed) {
        return new NormalSkylandGenerator(seed);
    }
    
    @Override
    public void generateSurface(
        WorldGenRegion region, IChunk chunk
    ) {
        proxy.generateSurface(region, chunk);
    }
    
    @Override
    public void func_230352_b_(
        IWorld world, StructureManager accessor, IChunk chunk
    ) {
        proxy.func_230352_b_(world, accessor, chunk);
    }
    
    //make end city and woodland mansion be able to generate
    @Override
    public int func_222529_a(int x, int z, Heightmap.Type heightmapType) {
        return proxy.func_222529_a(x, z, heightmapType);
    }
    
    @Override
    public IBlockReader func_230348_a_(int x, int z) {
        return proxy.func_230348_a_(x, z);
    }
    
    @Override
    public int func_230355_e_() {
        return proxy.func_230355_e_();
    }
    
    @Override
    public int func_230356_f_() {
        return proxy.func_230356_f_();
    }
    
    @Override
    public List<Biome.SpawnListEntry> func_230353_a_(Biome biome, StructureManager accessor, EntityClassification group, BlockPos pos) {
        return proxy.func_230353_a_(biome, accessor, group, pos);
    }
    
    @Override
    public void func_230354_a_(WorldGenRegion region) {
        proxy.func_230354_a_(region);
    }
    
}
