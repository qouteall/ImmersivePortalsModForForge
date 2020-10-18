package com.qouteall.imm_ptl_peripheral.alternate_dimension;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.block.Blocks;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.Dimension;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.OverworldBiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.DimensionSettings;
import net.minecraft.world.gen.FlatChunkGenerator;
import net.minecraft.world.gen.FlatGenerationSettings;
import net.minecraft.world.gen.FlatLayerInfo;
import net.minecraft.world.gen.NoiseChunkGenerator;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

public class AlternateDimensions {
    public static ChunkGenerator createSkylandGenerator(long seed, DynamicRegistries rm) {
        
        MutableRegistry<Biome> biomeRegistry = rm.func_243612_b(Registry.field_239720_u_);
        OverworldBiomeProvider biomeSource = new OverworldBiomeProvider(
            seed, false, false, biomeRegistry
        );
        
        MutableRegistry<DimensionSettings> settingsRegistry = rm.func_243612_b(Registry.field_243549_ar);
        
        HashMap<Structure<?>, StructureSeparationSettings> structureMap = new HashMap<>();
        structureMap.putAll(DimensionStructuresSettings.field_236191_b_);
        structureMap.remove(Structure.field_236367_c_);
        structureMap.remove(Structure.field_236375_k_);
        
        DimensionStructuresSettings structuresConfig = new DimensionStructuresSettings(
            Optional.empty(), structureMap
        );
        DimensionSettings skylandSetting = DimensionSettings.func_242742_a(
            structuresConfig, Blocks.STONE.getDefaultState(),
            Blocks.WATER.getDefaultState(), new ResourceLocation("imm_ptl:skyland_gen_id"),
            false, false
        );
        
        return new NoiseChunkGenerator(
            biomeSource, seed, () -> skylandSetting
        );
    }
    
    public static ChunkGenerator createErrorTerrainGenerator(long seed, DynamicRegistries rm) {
        MutableRegistry<Biome> biomeRegistry = rm.func_243612_b(Registry.field_239720_u_);
        
        ChaosBiomeSource chaosBiomeSource = new ChaosBiomeSource(seed, biomeRegistry);
        return new ErrorTerrainGenerator(seed, chaosBiomeSource);
    }
    
    public static ChunkGenerator createVoidGenerator(DynamicRegistries rm) {
        MutableRegistry<Biome> biomeRegistry = rm.func_243612_b(Registry.field_239720_u_);
        
        DimensionStructuresSettings structuresConfig = new DimensionStructuresSettings(
            Optional.of(DimensionStructuresSettings.field_236192_c_),
            Maps.newHashMap(ImmutableMap.of())
        );
        FlatGenerationSettings flatChunkGeneratorConfig =
            new FlatGenerationSettings(structuresConfig, biomeRegistry);
        flatChunkGeneratorConfig.getFlatLayers().add(new FlatLayerInfo(1, Blocks.AIR));
        flatChunkGeneratorConfig.updateLayers();
        
        return new FlatChunkGenerator(flatChunkGeneratorConfig);
    }
    
    public static void addDimension(
        long argSeed,
        SimpleRegistry<Dimension> registry,
        RegistryKey<Dimension> key,
        Supplier<DimensionType> dimensionTypeSupplier,
        ChunkGenerator chunkGenerator
    ) {
        if (!registry.keySet().contains(key.func_240901_a_())) {
            registry.register(
                key,
                new Dimension(
                    dimensionTypeSupplier,
                    chunkGenerator
                ),
                Lifecycle.experimental()
            );
        }
    }
    
    public static void addAlternateDimensions(
        SimpleRegistry<Dimension> registry, DynamicRegistries rm,
        long seed
    ) {
        addDimension(
            seed,
            registry,
            ModMain.alternate1Option,
            () -> ModMain.surfaceTypeObject,
            createSkylandGenerator(seed, rm)
        );
        
        addDimension(
            seed,
            registry,
            ModMain.alternate2Option,
            () -> ModMain.surfaceTypeObject,
            createSkylandGenerator(seed, rm)
        );
        
        addDimension(
            seed,
            registry,
            ModMain.alternate3Option,
            () -> ModMain.surfaceTypeObject,
            createErrorTerrainGenerator(seed, rm)
        );
        
        addDimension(
            seed,
            registry,
            ModMain.alternate4Option,
            () -> ModMain.surfaceTypeObject,
            createErrorTerrainGenerator(seed, rm)
        );
        
        addDimension(
            seed,
            registry,
            ModMain.alternate5Option,
            () -> ModMain.surfaceTypeObject,
            createVoidGenerator(rm)
        );
    }
    
    // don't store dimension info into level.dat
    // avoid weird dfu error
    public static SimpleRegistry<Dimension> getAlternateDimensionsRemoved(
        SimpleRegistry<Dimension> registry
    ) {
        return McHelper.filterAndCopyRegistry(
            registry,
            (key, obj) -> !(key == ModMain.alternate1Option ||
                key == ModMain.alternate2Option ||
                key == ModMain.alternate3Option ||
                key == ModMain.alternate4Option ||
                key == ModMain.alternate5Option)
        );
    }
    
    // When DFU does not recognize a mod dimension (in level.dat) it will throw an error
    // then the nether and the end will be swallowed
    // it's not IP's issue. but I add the fix code because many people encounter the issue
    public static void addMissingVanillaDimensions(
        SimpleRegistry<Dimension> registry, DynamicRegistries rm,
        long seed
    ) {
        if (!registry.keySet().contains(Dimension.field_236054_c_.func_240901_a_())) {
            Helper.err("Missing the nether. This may be caused by DFU. Trying to fix");
            
            addDimension(
                seed,
                registry,
                Dimension.field_236054_c_,
                () -> DimensionType.field_236005_i_,
                DimensionType.func_242720_b(
                    rm.func_243612_b(Registry.field_239720_u_),
                    rm.func_243612_b(Registry.field_243549_ar),
                    seed
                )
            );
        }
        
        if (!registry.keySet().contains(Dimension.field_236055_d_.func_240901_a_())) {
            Helper.err("Missing the end. This may be caused by DFU. Trying to fix");
            addDimension(
                seed,
                registry,
                Dimension.field_236055_d_,
                () -> DimensionType.field_236006_j_,
                DimensionType.func_242717_a(
                    rm.func_243612_b(Registry.field_239720_u_),
                    rm.func_243612_b(Registry.field_243549_ar),
                    seed
                )
            );
        }
    }
    
}
