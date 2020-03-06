package com.qouteall.immersive_portals.alternate_dimension;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.EndChunkGenerator;
import net.minecraft.world.gen.EndGenerationSettings;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.structure.EndCityStructure;
import net.minecraft.world.gen.feature.structure.MineshaftStructure;
import net.minecraft.world.gen.feature.structure.OceanMonumentStructure;
import net.minecraft.world.gen.feature.structure.StrongholdStructure;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.structure.WoodlandMansionStructure;
import net.minecraft.world.gen.feature.template.TemplateManager;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

public class ErrorTerrainGenerator extends EndChunkGenerator {
    private final BlockState AIR;
    
    public static final int regionChunkNum = 4;
    public static final int averageY = 64;
    public static final int maxY = 128;
    
    LoadingCache<ChunkPos, RegionErrorTerrainGenerator> cache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build(
            new CacheLoader<ChunkPos, RegionErrorTerrainGenerator>() {
                public RegionErrorTerrainGenerator load(ChunkPos key) {
                    return new RegionErrorTerrainGenerator(key.x, key.z, world.getSeed());
                }
            });
    
    public ErrorTerrainGenerator(
        IWorld iWorld,
        BiomeProvider biomeSource,
        EndGenerationSettings floatingIslandsChunkGeneratorConfig
    ) {
        super(iWorld, biomeSource, floatingIslandsChunkGeneratorConfig);
        AIR = Blocks.AIR.getDefaultState();
    }
    
    @Override
    public void makeBase(IWorld world, IChunk chunk) {
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
    
                        if (currBlockState != AIR) {
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
    
    //carve more
    @Override
    public void func_225550_a_(
        BiomeManager biomeAccess,
        IChunk chunk,
        GenerationStage.Carving carver
    ) {
        SharedSeedRandom chunkRandom = new SharedSeedRandom();
        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        Biome biome = this.getBiome(biomeAccess, chunkPos.asBlockPos());
        BitSet bitSet = chunk.getCarvingMask(carver);
        
        for (int cx = chunkX - 8; cx <= chunkX + 8; ++cx) {
            for (int cz = chunkZ - 8; cz <= chunkZ + 8; ++cz) {
                List<ConfiguredCarver<?>> list = biome.getCarvers(carver);
                ListIterator listIterator = list.listIterator();
                
                while (listIterator.hasNext()) {
                    int n = listIterator.nextIndex();
                    ConfiguredCarver<?> configuredCarver = (ConfiguredCarver) listIterator.next();
                    chunkRandom.setLargeFeatureSeed(this.seed + (long) n, cx, cz);
                    boolean shouldCarve = configuredCarver.shouldCarve(chunkRandom, cx, cz);
                    if (shouldCarve) {
                        //carve more
                        for (int i = 0; i < 4; i++) {
                            configuredCarver.func_227207_a_(chunk, (blockPos) -> {
                                return this.getBiome(biomeAccess, blockPos);
                            }, chunkRandom, this.getSeaLevel(), cx, cz, chunkX, chunkZ, bitSet);
                        }
                    }
                }
            }
        }
        
    }
    
    //generate more ore
    @Override
    public void decorate(WorldGenRegion region) {
        try {
            super.decorate(region);
    
        }
        catch (Throwable throwable) {
            Helper.err("Force ignore exception while generating feature " + throwable);
        }
    
        int centerChunkX = region.getMainChunkX();
        int centerChunkZ = region.getMainChunkZ();
        int x = centerChunkX * 16;
        int z = centerChunkZ * 16;
        BlockPos blockPos = new BlockPos(x, 0, z);
    
        for (int pass = 0; pass < 2; pass++) {
            Biome biome = this.getBiome(region.getBiomeManager(), blockPos.add(8, 8, 8));
            SharedSeedRandom chunkRandom = new SharedSeedRandom();
            long currSeed = chunkRandom.setDecorationSeed(region.getSeed() + pass, x, z);
        
            generateFeatureForStep(
                region, centerChunkX, centerChunkZ,
                blockPos, biome, chunkRandom, currSeed,
                GenerationStage.Decoration.UNDERGROUND_ORES
            );
        }
    
        SimpleSpawnerFeature.instance.place(
            region,
            this,
            randomSeed,
            blockPos,
            null
        );
    }
    
    private void generateFeatureForStep(
        WorldGenRegion region,
        Object centerChunkX,
        Object centerChunkZ,
        BlockPos blockPos,
        Biome biome,
        SharedSeedRandom chunkRandom,
        long currSeed,
        GenerationStage.Decoration feature
    ) {
        try {
            biome.decorate(
                feature, this, region, currSeed, chunkRandom, blockPos
            );
        }
        catch (Exception var17) {
            CrashReport crashReport = CrashReport.makeCrashReport(var17, "Biome decoration");
            crashReport.makeCategory("Generation").addDetail("CenterX", centerChunkX).addDetail(
                "CenterZ",
                centerChunkZ
            ).addDetail("Step", (Object) feature).addDetail("Seed", (Object) currSeed).addDetail(
                "Biome",
                (Object) Registry.BIOME.getKey(biome)
            );
            throw new ReportedException(crashReport);
        }
    }
    
    public void generateStructures(
        BiomeManager biomeAccess,
        IChunk chunk,
        ChunkGenerator<?> chunkGenerator,
        TemplateManager structureManager
    ) {
        randomSeed.setBaseChunkSeed(chunk.getPos().x, chunk.getPos().z);
    
        Iterator var5 = Feature.STRUCTURES.values().iterator();
    
        while (var5.hasNext()) {
            Structure<?> structureFeature = (Structure) var5.next();
            if (chunkGenerator.getBiomeProvider().hasStructure(structureFeature)) {
                StructureStart structureStart = chunk.getStructureStart(structureFeature.getStructureName());
                int i = structureStart != null ? structureStart.func_227457_j_() : 0;
                SharedSeedRandom chunkRandom = new SharedSeedRandom();
                ChunkPos chunkPos = chunk.getPos();
                StructureStart structureStart2 = StructureStart.DUMMY;
                Biome biome = biomeAccess.getBiome(new BlockPos(
                    chunkPos.getXStart() + 9,
                    0,
                    chunkPos.getZStart() + 9
                ));
                boolean shouldStart = hasStructure(biome, structureFeature);
                if (randomSeed.nextDouble() > getProbability(structureFeature)) {
                    shouldStart = false;
                }
                if (shouldStart) {
                    StructureStart structureStart3 = structureFeature.getStartFactory().create(
                        structureFeature,
                        chunkPos.x,
                        chunkPos.z,
                        MutableBoundingBox.getNewBoundingBox(),
                        i,
                        chunkGenerator.getSeed()
                    );
                    structureStart3.init(
                        this,
                        structureManager,
                        chunkPos.x,
                        chunkPos.z,
                        biome
                    );
                    structureStart2 = structureStart3.isValid() ? structureStart3 : StructureStart.DUMMY;
                }
                
                chunk.putStructureStart(structureFeature.getStructureName(), structureStart2);
            }
        }
        
    }
    
    private static double getProbability(Structure<?> structureFeature) {
        if (structureFeature instanceof StrongholdStructure) {
            return 0.015;
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
            return 0.1;
        }
        return 0.15;
    }
    
    @Override
    public void spawnMobs(WorldGenRegion region) {
        super.spawnMobs(region);
        
    }
    
    @Override
    public void func_225551_a_(WorldGenRegion chunkRegion, IChunk chunk) {
        super.func_225551_a_(chunkRegion, chunk);
        avoidSandLag(chunkRegion);
    }
    
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
    
    //make end city and woodland mansion be able to generate
    @Override
    public int func_222529_a(int x, int z, Heightmap.Type heightmapType) {
        return 64;
    }
    
}
