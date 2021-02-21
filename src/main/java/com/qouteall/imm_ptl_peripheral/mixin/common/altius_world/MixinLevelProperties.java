package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusGameRule;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraft.world.storage.ServerWorldInfo;
import net.minecraft.world.storage.VersionData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorldInfo.class)
public class MixinLevelProperties {
    
   
    
    @Shadow
    @Final
    private DimensionGeneratorSettings field_237343_c_;
    
    @Inject(
        method = "Lnet/minecraft/world/storage/ServerWorldInfo;func_237369_a_(Lcom/mojang/serialization/Dynamic;Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/world/WorldSettings;Lnet/minecraft/world/storage/VersionData;Lnet/minecraft/world/gen/settings/DimensionGeneratorSettings;Lcom/mojang/serialization/Lifecycle;)Lnet/minecraft/world/storage/ServerWorldInfo;",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onReadDataFromTag(
        Dynamic<INBT> dynamic,
        DataFixer dataFixer,
        int i,
        CompoundNBT playerTag,
        WorldSettings levelInfo,
        VersionData saveVersionInfo,
        DimensionGeneratorSettings generatorOptions,
        Lifecycle lifecycle,
        CallbackInfoReturnable<ServerWorldInfo> cir
    ) {
        ServerWorldInfo levelProperties = cir.getReturnValue();
        
        MixinLevelProperties this_ = (MixinLevelProperties) (Object) levelProperties;
        
        INBT altiusTag = dynamic.getElement("altius", null);
        if (altiusTag != null) {
            AltiusGameRule.upgradeOldDimensionStack();
        }
    }
    
   
}
