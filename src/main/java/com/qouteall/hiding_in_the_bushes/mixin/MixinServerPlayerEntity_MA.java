package com.qouteall.hiding_in_the_bushes.mixin;

import com.qouteall.immersive_portals.Global;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity_MA {
    @Inject(method = "changeDimension", at = @At("HEAD"), remap = false)
    private void onChangeDimensionByVanilla(
        DimensionType p_changeDimension_1_,
        ITeleporter p_changeDimension_2_,
        CallbackInfoReturnable<Entity> cir
    ) {
        Global.chunkDataSyncManager.onPlayerRespawn((ServerPlayerEntity) (Object) this);
    }
}
