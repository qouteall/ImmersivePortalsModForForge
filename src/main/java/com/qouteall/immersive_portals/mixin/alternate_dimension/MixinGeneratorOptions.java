package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.alternate_dimension.ErrorTerrainGenerator;
import com.qouteall.immersive_portals.alternate_dimension.NormalSkylandGenerator;
import com.qouteall.immersive_portals.alternate_dimension.VoidChunkGenerator;
import com.qouteall.immersive_portals.ducks.IESimpleRegistry;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.Dimension;
import net.minecraft.world.DimensionType;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

// Uses hacky ways to add dimension
// Should be replaced by better ways later
@Mixin(DimensionGeneratorSettings.class)
public class MixinGeneratorOptions {
    @Inject(
        method = "<init>(JZZLnet/minecraft/util/registry/SimpleRegistry;Ljava/util/Optional;)V",
        at = @At("RETURN")
    )
    private void onInitEnded(
        long seed,
        boolean generateStructures,
        boolean bonusChest,
        SimpleRegistry<Dimension> simpleRegistry,
        Optional<String> legacyCustomOptions,
        CallbackInfo ci
    ) {
        SimpleRegistry<Dimension> registry = simpleRegistry;
        
        if (Global.enableAlternateDimensions) {
            portal_addCustomDimension(
                seed,
                registry,
                ModMain.alternate1Option,
                () -> ModMain.surfaceTypeObject,
                NormalSkylandGenerator::new
            );
            
            portal_addCustomDimension(
                seed,
                registry,
                ModMain.alternate2Option,
                () -> ModMain.surfaceTypeObject,
                NormalSkylandGenerator::new
            );
            
            portal_addCustomDimension(
                seed,
                registry,
                ModMain.alternate3Option,
                () -> ModMain.surfaceTypeObject,
                ErrorTerrainGenerator::new
            );
            
            portal_addCustomDimension(
                seed,
                registry,
                ModMain.alternate4Option,
                () -> ModMain.surfaceTypeObject,
                ErrorTerrainGenerator::new
            );
    
            portal_addCustomDimension(
                seed,
                registry,
                ModMain.alternate5Option,
                () -> ModMain.surfaceTypeObject,
                (s) -> new VoidChunkGenerator()
            );
        }
        
        portal_recoverVanillaDimensions(seed, simpleRegistry);
    }
    
    // DFU may error with alternate dimensions and drop the nether and the end
    // Should be removed in later versions
    private void portal_recoverVanillaDimensions(
        long seed,
        SimpleRegistry<Dimension> simpleRegistry
    ) {
        if (!simpleRegistry.containsKey(Dimension.field_236054_c_.func_240901_a_())) {
            SimpleRegistry<Dimension> dimensionOptions = DimensionType.func_236022_a_(seed);
            
            simpleRegistry.register(
                Dimension.field_236054_c_,
                dimensionOptions.func_230516_a_(Dimension.field_236054_c_)
            );
            simpleRegistry.func_239662_d_(Dimension.field_236054_c_);
            
            Helper.err("Missing Nether. Recovered");
        }
        
        if (!simpleRegistry.containsKey(Dimension.field_236055_d_.func_240901_a_())) {
            SimpleRegistry<Dimension> dimensionOptions = DimensionType.func_236022_a_(seed);
            
            simpleRegistry.register(
                Dimension.field_236055_d_,
                dimensionOptions.func_230516_a_(Dimension.field_236055_d_)
            );
            simpleRegistry.func_239662_d_(Dimension.field_236055_d_);
            
            Helper.err("Missing The End. Recovered");
        }
        
    }
    
    void portal_addCustomDimension(
        long argSeed,
        SimpleRegistry<Dimension> registry,
        RegistryKey<Dimension> key,
        Supplier<DimensionType> dimensionTypeSupplier,
        Function<Long, ChunkGenerator> chunkGeneratorCreator
    ) {
        if (!registry.containsKey(key.func_240901_a_())) {
            registry.register(
                key,
                new Dimension(
                    dimensionTypeSupplier,
                    chunkGeneratorCreator.apply(argSeed)
                )
            );
        }
        //stop the dimension from being saved to level.dat
        //avoids messing with dfu
        ((IESimpleRegistry) registry).markUnloaded(key);
    }
}
