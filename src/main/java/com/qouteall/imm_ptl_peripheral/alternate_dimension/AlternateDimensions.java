package com.qouteall.imm_ptl_peripheral.alternate_dimension;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.api.IPDimensionAPI;
import com.qouteall.immersive_portals.ducks.IEWorld;
import net.minecraft.block.Blocks;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.Dimension;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.OverworldBiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.DimensionSettings;
import net.minecraft.world.gen.FlatChunkGenerator;
import net.minecraft.world.gen.FlatGenerationSettings;
import net.minecraft.world.gen.FlatLayerInfo;
import net.minecraft.world.gen.NoiseChunkGenerator;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraft.world.server.ServerWorld;
import java.util.HashMap;
import java.util.Optional;

public class AlternateDimensions {
    public static void init() {
        IPDimensionAPI.onServerWorldInit.connect(AlternateDimensions::initializeAlternateDimensions);
        
        ModMain.postServerTickSignal.connect(AlternateDimensions::tick);
    }
    
    private static void initializeAlternateDimensions(
        DimensionGeneratorSettings generatorOptions, DynamicRegistries registryManager
    ) {
        SimpleRegistry<Dimension> registry = generatorOptions.func_236224_e_();
        long seed = generatorOptions.func_236221_b_();
        if (!Global.enableAlternateDimensions) {
            return;
        }
        
        DimensionType surfaceTypeObject = registryManager.func_243612_b(Registry.field_239698_ad_).getOrDefault(new ResourceLocation("immersive_portals:surface_type"));
        
        if (surfaceTypeObject == null) {
            Helper.err("Missing dimension type immersive_portals:surface_type");
            return;
        }
        
        //different seed
        IPDimensionAPI.addDimension(
            seed,
            registry,
            alternate1Option.func_240901_a_(),
            () -> surfaceTypeObject,
            createSkylandGenerator(seed + 1, registryManager)
        );
        IPDimensionAPI.markDimensionNonPersistent(alternate1Option.func_240901_a_());
        
        IPDimensionAPI.addDimension(
            seed,
            registry,
            alternate2Option.func_240901_a_(),
            () -> surfaceTypeObject,
            createSkylandGenerator(seed, registryManager)
        );
        IPDimensionAPI.markDimensionNonPersistent(alternate2Option.func_240901_a_());
        
        //different seed
        IPDimensionAPI.addDimension(
            seed,
            registry,
            alternate3Option.func_240901_a_(),
            () -> surfaceTypeObject,
            createErrorTerrainGenerator(seed + 1, registryManager)
        );
        IPDimensionAPI.markDimensionNonPersistent(alternate3Option.func_240901_a_());
        
        IPDimensionAPI.addDimension(
            seed,
            registry,
            alternate4Option.func_240901_a_(),
            () -> surfaceTypeObject,
            createErrorTerrainGenerator(seed, registryManager)
        );
        IPDimensionAPI.markDimensionNonPersistent(alternate4Option.func_240901_a_());
        
        IPDimensionAPI.addDimension(
            seed,
            registry,
            alternate5Option.func_240901_a_(),
            () -> surfaceTypeObject,
            createVoidGenerator(registryManager)
        );
        IPDimensionAPI.markDimensionNonPersistent(alternate5Option.func_240901_a_());
    }
    
    
    public static final RegistryKey<Dimension> alternate1Option = RegistryKey.func_240903_a_(
        Registry.field_239700_af_,
        new ResourceLocation("immersive_portals:alternate1")
    );
    public static final RegistryKey<Dimension> alternate2Option = RegistryKey.func_240903_a_(
        Registry.field_239700_af_,
        new ResourceLocation("immersive_portals:alternate2")
    );
    public static final RegistryKey<Dimension> alternate3Option = RegistryKey.func_240903_a_(
        Registry.field_239700_af_,
        new ResourceLocation("immersive_portals:alternate3")
    );
    public static final RegistryKey<Dimension> alternate4Option = RegistryKey.func_240903_a_(
        Registry.field_239700_af_,
        new ResourceLocation("immersive_portals:alternate4")
    );
    public static final RegistryKey<Dimension> alternate5Option = RegistryKey.func_240903_a_(
        Registry.field_239700_af_,
        new ResourceLocation("immersive_portals:alternate5")
    );
    public static final RegistryKey<DimensionType> surfaceType = RegistryKey.func_240903_a_(
        Registry.field_239698_ad_,
        new ResourceLocation("immersive_portals:surface_type")
    );
    public static final RegistryKey<World> alternate1 = RegistryKey.func_240903_a_(
        Registry.field_239699_ae_,
        new ResourceLocation("immersive_portals:alternate1")
    );
    public static final RegistryKey<World> alternate2 = RegistryKey.func_240903_a_(
        Registry.field_239699_ae_,
        new ResourceLocation("immersive_portals:alternate2")
    );
    public static final RegistryKey<World> alternate3 = RegistryKey.func_240903_a_(
        Registry.field_239699_ae_,
        new ResourceLocation("immersive_portals:alternate3")
    );
    public static final RegistryKey<World> alternate4 = RegistryKey.func_240903_a_(
        Registry.field_239699_ae_,
        new ResourceLocation("immersive_portals:alternate4")
    );
    public static final RegistryKey<World> alternate5 = RegistryKey.func_240903_a_(
        Registry.field_239699_ae_,
        new ResourceLocation("immersive_portals:alternate5")
    );
//    public static DimensionType surfaceTypeObject;
    
    public static boolean isAlternateDimension(World world) {
        final RegistryKey<World> key = world.func_234923_W_();
        return key == alternate1 ||
            key == alternate2 ||
            key == alternate3 ||
            key == alternate4 ||
            key == alternate5;
    }
    
    private static void syncWithOverworldTimeWeather(ServerWorld world, ServerWorld overworld) {
        ((IEWorld) world).portal_setWeather(
            overworld.getRainStrength(1), overworld.getRainStrength(1),
            overworld.getThunderStrength(1), overworld.getThunderStrength(1)
        );
    }
    
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
    
    
    private static void tick() {
        if (!Global.enableAlternateDimensions) {
            return;
        }
        
        ServerWorld overworld = McHelper.getServerWorld(World.field_234918_g_);
        
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate1), overworld);
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate2), overworld);
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate3), overworld);
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate4), overworld);
        syncWithOverworldTimeWeather(McHelper.getServerWorld(alternate5), overworld);
    }
    
    
}
