package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.qouteall.imm_ptl_peripheral.altius_world.AltiusInfo;
import com.qouteall.imm_ptl_peripheral.ducks.IELevelProperties;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.datafix.codec.DatapackCodec;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldSettings.class)
public class MixinLevelInfo implements IELevelProperties {
    
    AltiusInfo altiusInfo;
    
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
                MixinLevelInfo this_ = (MixinLevelInfo) (Object) cir.getReturnValue();
                this_.altiusInfo = AltiusInfo.fromTag(((CompoundNBT) obj));
            }
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/world/WorldSettings;func_234950_a_(Lnet/minecraft/world/GameType;)Lnet/minecraft/world/WorldSettings;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onSetGamemode(GameType gameMode, CallbackInfoReturnable<WorldSettings> cir) {
        ((MixinLevelInfo) (Object) cir.getReturnValue()).altiusInfo = altiusInfo;
    }
    
    @Inject(
        method = "Lnet/minecraft/world/WorldSettings;func_234948_a_(Lnet/minecraft/world/Difficulty;)Lnet/minecraft/world/WorldSettings;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onSetDifficulty(Difficulty difficulty, CallbackInfoReturnable<WorldSettings> cir) {
        ((MixinLevelInfo) (Object) cir.getReturnValue()).altiusInfo = altiusInfo;
    }
    
    @Inject(
        method = "Lnet/minecraft/world/WorldSettings;func_234949_a_(Lnet/minecraft/util/datafix/codec/DatapackCodec;)Lnet/minecraft/world/WorldSettings;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onSetDatapack(
        DatapackCodec dataPackSettings,
        CallbackInfoReturnable<WorldSettings> cir
    ) {
        ((MixinLevelInfo) (Object) cir.getReturnValue()).altiusInfo = altiusInfo;
    }
    
    @Inject(
        method = "Lnet/minecraft/world/WorldSettings;func_234959_h_()Lnet/minecraft/world/WorldSettings;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onCopy(CallbackInfoReturnable<WorldSettings> cir) {
        ((MixinLevelInfo) (Object) cir.getReturnValue()).altiusInfo = altiusInfo;
    }
    
    @Override
    public AltiusInfo getAltiusInfo() {
        return altiusInfo;
    }
    
    @Override
    public void setAltiusInfo(AltiusInfo cond) {
        altiusInfo = cond;
    }
}
