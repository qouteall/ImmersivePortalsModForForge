package com.qouteall.imm_ptl_peripheral.mixin.common.alternate_dimension;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalLong;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.IBiomeMagnifier;

@Mixin(DimensionType.class)
public class MixinDimensionType {
    
    @Invoker("<init>")
    static DimensionType constructor(
        OptionalLong fixedTime, boolean hasSkylight, boolean hasCeiling, boolean ultrawarm,
        boolean natural, double coordinateScale, boolean hasEnderDragonFight, boolean piglinSafe,
        boolean bedWorks, boolean respawnAnchorWorks, boolean hasRaids, int logicalHeight,
        IBiomeMagnifier biomeAccessType,
        ResourceLocation infiniburn, ResourceLocation skyProperties, float ambientLight
    ) {
        return null;
    }
    
    @Inject(
        method = "Lnet/minecraft/world/DimensionType;func_236027_a_(Lnet/minecraft/util/registry/DynamicRegistries$Impl;)Lnet/minecraft/util/registry/DynamicRegistries$Impl;",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onAddRegistryDefaults(
        DynamicRegistries.Impl registryManager,
        CallbackInfoReturnable<DynamicRegistries.Impl> cir
    ) {
//        MutableRegistry<DimensionType> mutableRegistry = registryManager.get(Registry.DIMENSION_TYPE_KEY);
//        mutableRegistry.add(
//            AlternateDimensions.surfaceType,
//            AlternateDimensions.surfaceTypeObject,
//            Lifecycle.stable()
//        );
    }
    
    static {
//        AlternateDimensions.surfaceTypeObject = constructor(
//            OptionalLong.empty(), true, false,
//            false, true, 1.0D, false,
//            false, true, false, true,
//            256, HorizontalVoronoiBiomeAccessType.INSTANCE,
//            BlockTags.INFINIBURN_OVERWORLD.getId(),
//            DimensionType.OVERWORLD_ID, 0.0F
//        );
    }
}
