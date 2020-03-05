package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.teleportation.CollisionHelper;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    //use portal culled collision box
    @Redirect(
        method = "Lnet/minecraft/client/entity/player/ClientPlayerEntity;shouldBlockPushPlayer(Lnet/minecraft/util/math/BlockPos;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/entity/player/ClientPlayerEntity;getBoundingBox()Lnet/minecraft/util/math/AxisAlignedBB;"
        )
    )
    private AxisAlignedBB redirectGetBoundingBox(ClientPlayerEntity clientPlayerEntity) {
        return CollisionHelper.getActiveCollisionBox(clientPlayerEntity);
    }
}
