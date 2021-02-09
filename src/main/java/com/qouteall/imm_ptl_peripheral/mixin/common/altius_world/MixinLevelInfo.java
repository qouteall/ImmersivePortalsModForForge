package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusGameRule;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.datafix.codec.DatapackCodec;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldSettings.class)
public class MixinLevelInfo {
    
    @Inject(
        method = "Lnet/minecraft/world/WorldSettings;func_234951_a_(Lcom/mojang/serialization/Dynamic;Lnet/minecraft/util/datafix/codec/DatapackCodec;)Lnet/minecraft/world/WorldSettings;",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onReadLevelInfoFromDynamic(
        Dynamic<?> dynamic,
        DatapackCodec dataPackSettings,
        CallbackInfoReturnable<WorldSettings> cir
    ) {
        DataResult<?> altiusElement = dynamic.getElement("altius");
        Object obj = altiusElement.get().left().orElse(null);
        if (obj != null) {
            if (obj instanceof CompoundNBT) {
                AltiusGameRule.upgradeOldDimensionStack();
            }
        }
    }
}
