package com.qouteall.immersive_portals.mixin.altius_world;

import com.mojang.datafixers.DataFixer;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import com.qouteall.immersive_portals.ducks.IELevelProperties;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldInfo.class)
public class MixinLevelProperties implements IELevelProperties {
    AltiusInfo altiusInfo;
    
    @Inject(
        method = "<init>(Lnet/minecraft/world/WorldSettings;Ljava/lang/String;)V",
        at = @At("RETURN")
    )
    private void onConstructedFromLevelInfo(
        WorldSettings levelInfo, String levelName, CallbackInfo ci
    ) {
        altiusInfo = ((IELevelProperties) (Object) levelInfo).getAltiusInfo();
    }
    
    @Inject(
        method = "<init>(Lnet/minecraft/nbt/CompoundNBT;Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/CompoundNBT;)V",
        at = @At("RETURN")
    )
    private void onConstructedFromTag(
        CompoundNBT compoundTag,
        DataFixer dataFixer,
        int i,
        CompoundNBT compoundTag2,
        CallbackInfo ci
    ) {
        if (compoundTag.contains("altius")) {
            INBT tag = compoundTag.get("altius");
            altiusInfo = AltiusInfo.fromTag(((CompoundNBT) tag));
        }
        if (compoundTag.contains("generatorName", 8)) {
            String generatorName = compoundTag.getString("generatorName");
            if (generatorName.equals("imm_ptl_altius")) {
                Helper.log("Upgraded old altius world to new altius world");
                altiusInfo = AltiusInfo.getDummy();
            }
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/world/storage/WorldInfo;updateTagCompound(Lnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/nbt/CompoundNBT;)V",
        at = @At("RETURN")
    )
    private void onUpdateProperties(CompoundNBT levelTag, CompoundNBT playerTag, CallbackInfo ci) {
        if (altiusInfo != null) {
            levelTag.put("altius", altiusInfo.toTag());
        }
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
