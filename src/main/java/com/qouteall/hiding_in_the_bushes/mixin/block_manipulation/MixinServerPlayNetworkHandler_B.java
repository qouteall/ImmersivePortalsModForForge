package com.qouteall.hiding_in_the_bushes.mixin.block_manipulation;

import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayNetHandler.class)
public class MixinServerPlayNetworkHandler_B {
    @Shadow
    public ServerPlayerEntity player;
    
    @Redirect(
        method = "Lnet/minecraft/network/play/ServerPlayNetHandler;processTryUseItemOnBlock(Lnet/minecraft/network/play/client/CPlayerTryUseItemOnBlockPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ai/attributes/IAttributeInstance;getValue()D"
        )
    )
    private double modifyPlacingBlockRangeSquared(IAttributeInstance iAttributeInstance) {
        double multiplier = HandReachTweak.getActualHandReachMultiplier(player);
        return iAttributeInstance.getValue() * multiplier;
    }
}
