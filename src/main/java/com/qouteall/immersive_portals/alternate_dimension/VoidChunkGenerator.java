package com.qouteall.immersive_portals.alternate_dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Blockreader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.provider.SingleBiomeProvider;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.server.ServerWorld;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

public class VoidChunkGenerator extends ChunkGenerator {
    public static Codec<VoidChunkGenerator> codec;
    
    private Blockreader verticalBlockSample = new Blockreader(
        Stream.generate(Blocks.AIR::getDefaultState)
            .limit(256)
            .toArray(BlockState[]::new)
    );
    
    public VoidChunkGenerator() {
        super(
            new SingleBiomeProvider(Biomes.PLAINS),
            new DimensionStructuresSettings(Optional.empty(), new HashMap<>())
        );
    }
    
    @Override
    protected Codec<? extends ChunkGenerator> func_230347_a_() {
        return codec;
    }
    
    @Override
    public ChunkGenerator func_230349_a_(long seed) {
        return this;
    }
    
    @Override
    public void generateSurface(
        WorldGenRegion region, IChunk chunk
    ) {
        //nothing
    }
    
    @Override
    public void func_230352_b_(
        IWorld world, StructureManager accessor, IChunk chunk
    ) {
        //nothing
    }
    
    @Override
    public void func_230350_a_(
        long seed, BiomeManager access, IChunk chunk, GenerationStage.Carving carver
    ) {
        //nothing
    }
    
    @Nullable
    @Override
    public BlockPos func_235956_a_(
        ServerWorld world,
        Structure<?> feature,
        BlockPos center,
        int radius,
        boolean skipExistingChunks
    ) {
        return null;
    }
    
    @Override
    public void func_230351_a_(WorldGenRegion region, StructureManager accessor) {
        //nothing
    }
    
    @Override
    public void func_230354_a_(WorldGenRegion region) {
        //nothing
    }
    
    @Override
    public void func_235954_a_(
        StructureManager structureAccessor, IChunk chunk, TemplateManager structureManager, long l
    ) {
        //nothing
    }
    
    @Override
    public void func_235953_a_(
        IWorld world, StructureManager accessor, IChunk chunk
    ) {
        //nothing
    }
    
    @Override
    public int func_222529_a(int x, int z, Heightmap.Type heightmapType) {
        return 0;
    }
    
    @Override
    public IBlockReader func_230348_a_(int x, int z) {
        return verticalBlockSample;
    }
    
    static {
        codec = MapCodec.of(
            Encoder.empty(),
            Decoder.unit(VoidChunkGenerator::new)
        ).stable().codec();
    }
}
