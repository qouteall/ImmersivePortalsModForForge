package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.ColumnFuzzedBiomeMagnifier;
import net.minecraft.world.biome.IBiomeMagnifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalLong;

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
        MutableRegistry<DimensionType> mutableRegistry = registryManager.func_243612_b(Registry.field_239698_ad_);
        mutableRegistry.register(
            ModMain.surfaceType,
            ModMain.surfaceTypeObject,
            Lifecycle.stable()
        );
    }

    static {
        ModMain.surfaceTypeObject = constructor(
            OptionalLong.empty(), true, false,
            false, true, 1.0D, false,
            false, true, false, true,
            256, ColumnFuzzedBiomeMagnifier.INSTANCE,
            BlockTags.field_241277_aC_.func_230234_a_(),
            DimensionType.field_242710_a, 0.0F
        );
    }
}
