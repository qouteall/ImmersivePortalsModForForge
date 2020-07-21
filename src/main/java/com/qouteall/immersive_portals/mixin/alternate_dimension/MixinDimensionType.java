package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.ModMain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalLong;
import net.minecraft.server.IDynamicRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;

@Mixin(DimensionType.class)
public class MixinDimensionType {
    
    @Invoker("<init>")
    static DimensionType constructor(
        OptionalLong fixedTime,
        boolean hasSkylight,
        boolean hasCeiling,
        boolean ultrawarm,
        boolean natural,
        boolean shrunk,
        boolean piglinSafe,
        boolean bedWorks,
        boolean respawnAnchorWorks,
        boolean hasRaids,
        int logicalHeight,
        ResourceLocation infiniburn,
        float ambientLight
    ) {
        return null;
    }
    
//    @Inject(
//        method = "method_28517",
//        at = @At("RETURN"),
//        cancellable = true
//    )
//    private static void onInitDimensionOptions(
//        long seed,
//        CallbackInfoReturnable<SimpleRegistry<DimensionOptions>> cir
//    ) {

//    }
    
    @Inject(
        method = "Lnet/minecraft/world/DimensionType;func_236027_a_(Lnet/minecraft/server/IDynamicRegistries$Impl;)Lnet/minecraft/server/IDynamicRegistries$Impl;",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onAddRegistryDefaults(
        IDynamicRegistries.Impl registryTracker,
        CallbackInfoReturnable<IDynamicRegistries.Impl> cir
    ) {
        registryTracker.func_239774_a_(
            ModMain.surfaceType,
            ModMain.surfaceTypeObject
        );
    }
    
    static {
        ModMain.surfaceTypeObject = constructor(
            OptionalLong.empty(),
            true,
            false,
            false,
            true,
            false,
            false,
            true,
            true,
            true,
            256,
            BlockTags.INFINIBURN_OVERWORLD.func_230234_a_(),
            0
        );
    }
}
