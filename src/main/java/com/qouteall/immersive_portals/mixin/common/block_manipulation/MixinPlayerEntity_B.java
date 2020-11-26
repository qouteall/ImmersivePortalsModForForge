package com.qouteall.immersive_portals.mixin.common.block_manipulation;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity_B {
    @Inject(method = "Lnet/minecraft/entity/player/PlayerEntity;func_234570_el_()Lnet/minecraft/entity/ai/attributes/AttributeModifierMap$MutableAttribute;", at = @At("RETURN"), cancellable = true)
    private static void onCreatePlayerAttributes(CallbackInfoReturnable<AttributeModifierMap.MutableAttribute> cir) {
        // We only need to use this attribute on Fabric, not Forge.  If we inject on forge it causes
        // compatibility problems with other mods.
        if (!O_O.isForge()) {
            cir.setReturnValue(cir.getReturnValue().func_233815_a_(HandReachTweak.handReachMultiplierAttribute, 1.0));
        }
    }
}
