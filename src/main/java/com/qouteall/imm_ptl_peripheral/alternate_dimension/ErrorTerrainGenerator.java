package com.qouteall.imm_ptl_peripheral.alternate_dimension;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Blockreader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.PerlinNoiseGenerator;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.EndCityStructure;
import net.minecraft.world.gen.feature.structure.MineshaftStructure;
import net.minecraft.world.gen.feature.structure.OceanMonumentStructure;
import net.minecraft.world.gen.feature.structure.StrongholdStructure;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.WoodlandMansionStructure;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.spawner.WorldEntitySpawner;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ErrorTerrainGenerator extends ChunkGenerator {
    public static final Codec<ErrorTerrainGenerator> codec = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("seed").stable().forGetter(g -> g.worldSeed),
            ChaosBiomeSource.codec.fieldOf("biomeSource").stable().forGetter(o -> ((ChaosBiomeSource) o.getBiomeProvider()))
        ).apply(instance, instance.stable(ErrorTerrainGenerator::new))
    );
    
    private final BlockState air = Blocks.AIR.getDefaultState();
    private final BlockState defaultBlock = Blocks.STONE.getDefaultState();
    private final BlockState defaultFluid = Blocks.WATER.getDefaultState();
    
    public static final int regionChunkNum = 4;
    public static final int averageY = 64;
    public static final int maxY = 128;
    
    private static final Blockreader verticalBlockSample = new Blockreader(
        Stream.concat(
            Stream.generate(Blocks.STONE::getDefaultState).limit(64),
            Stream.generate(Blocks.AIR::getDefaultState).limit(128 + 64)
        ).toArray(BlockState[]::new)
    );
    
    private long worldSeed;
    
    private final LoadingCache<ChunkPos, RegionErrorTerrainGenerator> cache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build(
            new CacheLoader<ChunkPos, RegionErrorTerrainGenerator>() {
                public RegionErrorTerrainGenerator load(ChunkPos key) {
                    return new RegionErrorTerrainGenerator(key.x, key.z, worldSeed);
                }
            });
    
    private final PerlinNoiseGenerator surfaceDepthNoise;
    
    public ErrorTerrainGenerator(long seed, BiomeProvider biomeSource) {
        super(biomeSource, new DimensionStructuresSettings(true));
        worldSeed = seed;
        
        surfaceDepthNoise = new PerlinNoiseGenerator(
            new SharedSeedRandom(seed), IntStream.rangeClosed(-3, 0));
        
    }
    
    private static double getProbability(Structure<?> structureFeature) {
        if (structureFeature instanceof StrongholdStructure) {
            return 0.0007;
        }
        if (structureFeature instanceof MineshaftStructure) {
            return 0.015;
        }
        if (structureFeature instanceof OceanMonumentStructure) {
            return 0.03;
        }
        if (structureFeature instanceof WoodlandMansionStructure) {
            return 0.08;
        }
        if (structureFeature instanceof EndCityStructure) {
            return 0.2;
        }
        return 0.15;
    }
    
    @Override
    public void func_230354_a_(WorldGenRegion region) {
        int i = region.getMainChunkX();
        int j = region.getMainChunkZ();
        Biome biome = region.getBiome((new ChunkPos(i, j)).asBlockPos());
        SharedSeedRandom chunkRandom = new SharedSeedRandom();
        chunkRandom.setDecorationSeed(region.getSeed(), i << 4, j << 4);
        WorldEntitySpawner.performWorldGenSpawning(region, biome, i, j, chunkRandom);
    }
    
    @Override
    public void func_230352_b_(
        IWorld world, StructureManager accessor, IChunk chunk
    ) {
        ChunkPrimer protoChunk = (ChunkPrimer) chunk;
        ChunkPos pos = chunk.getPos();
        Heightmap oceanFloorHeightMap = protoChunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap surfaceHeightMap = protoChunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        
        int regionX = Math.floorDiv(pos.x, regionChunkNum);
        int regionZ = Math.floorDiv(pos.z, regionChunkNum);
        RegionErrorTerrainGenerator generator = Helper.noError(() ->
            cache.get(new ChunkPos(regionX, regionZ))
        );
        
        for (int sectionY = 0; sectionY < 16; sectionY++) {
            ChunkSection section = protoChunk.getSection(sectionY);
            section.lock();
            
            for (int localX = 0; localX < 16; localX++) {
                for (int localY = 0; localY < 16; localY++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int worldX = pos.x * 16 + localX;
                        int worldY = sectionY * 16 + localY;
                        int worldZ = pos.z * 16 + localZ;
                        
                        BlockState currBlockState = generator.getBlockComposition(
                            worldX, worldY, worldZ
                        );
                        
                        if (currBlockState != air) {
                            section.setBlockState(localX, localY, localZ, currBlockState, false);
                            oceanFloorHeightMap.update(localX, worldY, localZ, currBlockState);
                            surfaceHeightMap.update(localX, worldY, localZ, currBlockState);
                        }
                    }
                }
            }
            
            section.unlock();
        }
        
    }
    
    @Override
    public int func_222529_a(int x, int z, Heightmap.Type heightmapType) {
        return 64;
    }
    
    @Override
    public int getWorldHeight() {
        return 128;
    }
    
    //if it's not 0, the sea biome will cause huge lag spike because of light updates
    //don't know why
    @Override
    public int func_230356_f_() {
        return 0;
    }
    
    //may be incorrect
    @Override
    public IBlockReader func_230348_a_(int x, int z) {
        return verticalBlockSample;
    }
    
    @Override
    protected Codec<? extends ChunkGenerator> func_230347_a_() {
        return codec;
    }
    
    @Override
    public ChunkGenerator func_230349_a_(long seed) {
        return new ErrorTerrainGenerator(seed, getBiomeProvider().func_230320_a_(seed));
    }
    
    @Override
    public void generateSurface(WorldGenRegion region, IChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.x;
        int j = chunkPos.z;
        SharedSeedRandom chunkRandom = new SharedSeedRandom();
        chunkRandom.setBaseChunkSeed(i, j);
        ChunkPos chunkPos2 = chunk.getPos();
        int k = chunkPos2.getXStart();
        int l = chunkPos2.getZStart();
        double d = 0.0625D;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        
        for (int m = 0; m < 16; ++m) {
            for (int n = 0; n < 16; ++n) {
                int o = k + m;
                int p = l + n;
                int q = chunk.getTopBlockY(Heightmap.Type.WORLD_SURFACE_WG, m, n) + 1;
                double e = this.surfaceDepthNoise.noiseAt(
                    (double) o * 0.0625D,
                    (double) p * 0.0625D,
                    0.0625D,
                    (double) m * 0.0625D
                ) * 15.0D;
                region.getBiome(mutable.setPos(k + m, q, l + n)).buildSurface(
                    chunkRandom,
                    chunk,
                    o,
                    p,
                    q,
                    e,
                    this.defaultBlock,
                    this.defaultFluid,
                    this.func_230356_f_(),
                    region.getSeed()
                );
            }
        }
        
        
        avoidSandLag(region);
    }
    
    //TODO carve more
    
    private static void avoidSandLag(WorldGenRegion region) {
        IChunk centerChunk = region.getChunk(region.getMainChunkX(), region.getMainChunkZ());
        BlockPos.Mutable temp = new BlockPos.Mutable();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                boolean isLastAir = true;
                for (int y = 0; y < 100; y++) {
                    temp.setPos(x, y, z);
                    BlockState blockState = centerChunk.getBlockState(temp);
                    Block block = blockState.getBlock();
                    if (block == Blocks.SAND || block == Blocks.GRAVEL) {
                        if (isLastAir) {
                            centerChunk.setBlockState(
                                temp,
                                Blocks.SANDSTONE.getDefaultState(),
                                true
                            );
                        }
                    }
                    isLastAir = blockState.isAir();
                }
            }
        }
    }
}
