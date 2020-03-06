package com.qouteall.hiding_in_the_bushes.mixin.block_manipulation;

import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.management.PlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerInteractionManager.class)
public class MixinServerPlayerInteractionManager {
    @Shadow
    public ServerPlayerEntity player;
    
    @Redirect(
        method = "Lnet/minecraft/server/management/PlayerInteractionManager;func_225416_a(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/network/play/client/CPlayerDiggingPacket$Action;Lnet/minecraft/util/Direction;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ai/attributes/IAttributeInstance;getValue()D"
        )
    )
    private double modifyBreakBlockRangeSquare(IAttributeInstance iAttributeInstance) {
        double multiplier = HandReachTweak.getActualHandReachMultiplier(player);
        return iAttributeInstance.getValue() * multiplier;
    }
}
