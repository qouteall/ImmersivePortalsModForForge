package com.qouteall.immersive_portals.api;

import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.SignalBiArged;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.Dimension;
import net.minecraft.world.DimensionType;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class IPDimensionAPI {
    public static final SignalBiArged<DimensionGeneratorSettings, DynamicRegistries> onServerWorldInit = new SignalBiArged<>();
    
    private static final Set<ResourceLocation> nonPersistentDimensions = new HashSet<>();
    
    public static void init() {
        onServerWorldInit.connect(IPDimensionAPI::addMissingVanillaDimensions);
    }
    
    public static void addDimension(
        long argSeed,
        SimpleRegistry<Dimension> dimensionOptionsRegistry,
        ResourceLocation dimensionId,
        Supplier<DimensionType> dimensionTypeSupplier,
        ChunkGenerator chunkGenerator
    ) {
        if (!dimensionOptionsRegistry.keySet().contains(dimensionId)) {
            dimensionOptionsRegistry.register(
                RegistryKey.func_240903_a_(Registry.field_239700_af_, dimensionId),
                new Dimension(
                    dimensionTypeSupplier,
                    chunkGenerator
                ),
                Lifecycle.experimental()
            );
        }
    }
    
    public static void markDimensionNonPersistent(ResourceLocation dimensionId) {
        nonPersistentDimensions.add(dimensionId);
    }
    
    // This is not API
    // When DFU does not recognize a mod dimension (in level.dat) it will throw an error
    // then the nether and the end will be swallowed (https://github.com/TelepathicGrunt/Bumblezone-Fabric/issues/20)
    // to fix that, don't store the custom dimensions into level.dat
    public static SimpleRegistry<Dimension> getAdditionalDimensionsRemoved(
        SimpleRegistry<Dimension> registry
    ) {
        if (nonPersistentDimensions.isEmpty()) {
            return registry;
        }
        
        return McHelper.filterAndCopyRegistry(
            registry,
            (key, obj) -> {
                ResourceLocation identifier = key.func_240901_a_();
                return !nonPersistentDimensions.contains(identifier);
            }
        );
    }
    
    // fix nether and end swallowed by DFU error
    private static void addMissingVanillaDimensions(DimensionGeneratorSettings generatorOptions, DynamicRegistries registryManager) {
        SimpleRegistry<Dimension> registry = generatorOptions.func_236224_e_();
        long seed = generatorOptions.func_236221_b_();
        if (!registry.keySet().contains(Dimension.field_236054_c_.func_240901_a_())) {
            Helper.err("Missing the nether. This may be caused by DFU. Trying to fix");
            
            IPDimensionAPI.addDimension(
                seed,
                registry,
                Dimension.field_236054_c_.func_240901_a_(),
                () -> DimensionType.field_236005_i_,
                DimensionType.func_242720_b(
                    registryManager.func_243612_b(Registry.field_239720_u_),
                    registryManager.func_243612_b(Registry.field_243549_ar),
                    seed
                )
            );
        }
        
        if (!registry.keySet().contains(Dimension.field_236055_d_.func_240901_a_())) {
            Helper.err("Missing the end. This may be caused by DFU. Trying to fix");
            IPDimensionAPI.addDimension(
                seed,
                registry,
                Dimension.field_236055_d_.func_240901_a_(),
                () -> DimensionType.field_236006_j_,
                DimensionType.func_242717_a(
                    registryManager.func_243612_b(Registry.field_239720_u_),
                    registryManager.func_243612_b(Registry.field_243549_ar),
                    seed
                )
            );
        }
    }
}
