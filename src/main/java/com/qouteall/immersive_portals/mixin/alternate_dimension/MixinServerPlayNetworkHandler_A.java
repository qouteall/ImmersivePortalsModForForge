package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.ducks.IEPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayNetHandler.class)
public class MixinServerPlayNetworkHandler_A {
    @Shadow
    public ServerPlayerEntity player;
    
    @Redirect(
        method = "Lnet/minecraft/network/play/ServerPlayNetHandler;processClientStatus(Lnet/minecraft/network/play/client/CClientStatusPacket;)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/dimension/DimensionType;OVERWORLD:Lnet/minecraft/world/dimension/DimensionType;"
        )
    )
    private DimensionType redirectRespawnDimension() {
        DimensionType spawnDimension = ((IEPlayerEntity) player).portal_getSpawnDimension();
        if (spawnDimension != null) {
            return spawnDimension;
        }
        else {
            return DimensionType.OVERWORLD;
        }
    }
}
