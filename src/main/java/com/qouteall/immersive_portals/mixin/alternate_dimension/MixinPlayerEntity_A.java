package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity_A implements IEPlayerEntity {
    private DimensionType portal_spawnDimension;
    
    @Inject(method = "Lnet/minecraft/entity/player/PlayerEntity;readAdditional(Lnet/minecraft/nbt/CompoundNBT;)V", at = @At("RETURN"))
    private void onReadCustomDataFromTag(CompoundNBT tag, CallbackInfo ci) {
        if (tag.contains("portal_spawnDimension")) {
            int dimIntId = tag.getInt("portal_spawnDimension");
            DimensionType dimensionType = DimensionType.getById(dimIntId);
            if (dimensionType == null) {
                Helper.err("Invalid spawn dimension " + dimIntId);
            }
            else {
                portal_spawnDimension = dimensionType;
            }
        }
    }
    
    @Inject(method = "Lnet/minecraft/entity/player/PlayerEntity;writeAdditional(Lnet/minecraft/nbt/CompoundNBT;)V", at = @At("RETURN"))
    private void onWriteCustomDataToTag(CompoundNBT tag, CallbackInfo ci) {
        if (portal_spawnDimension != null) {
            tag.putInt("portal_spawnDimension", portal_spawnDimension.getId());
        }
    }
    
    @Inject(method = "Lnet/minecraft/entity/player/PlayerEntity;func_226560_a_(Lnet/minecraft/util/math/BlockPos;ZZ)V", at = @At("RETURN"))
    private void onSetPlayerSpawn(BlockPos blockPos, boolean bl, boolean bl2, CallbackInfo ci) {
        portal_spawnDimension = ((Entity) (Object) this).dimension;
    }
    
    @Override
    public DimensionType portal_getSpawnDimension() {
        return portal_spawnDimension;
    }
}
