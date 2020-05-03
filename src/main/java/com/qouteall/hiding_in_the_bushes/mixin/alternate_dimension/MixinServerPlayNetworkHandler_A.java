package com.qouteall.hiding_in_the_bushes.mixin.alternate_dimension;

import com.qouteall.immersive_portals.ducks.IEPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CClientStatusPacket;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetHandler.class)
public class MixinServerPlayNetworkHandler_A {
//    @Shadow
//    public ServerPlayerEntity player;
//
//    @Redirect(
//        method = "processClientStatus",
//        at = @At(
//            value = "FIELD",
//            target = "Lnet/minecraft/world/dimension/DimensionType;OVERWORLD:Lnet/minecraft/world/dimension/DimensionType;"
//        )
//    )
//    private DimensionType redirectRespawnDimension() {
//        DimensionType spawnDimension = ((IEPlayerEntity) player).portal_getSpawnDimension();
//        if (spawnDimension != null) {
//            return spawnDimension;
//        }
//        else {
//            return DimensionType.OVERWORLD;
//        }
//    }
}
