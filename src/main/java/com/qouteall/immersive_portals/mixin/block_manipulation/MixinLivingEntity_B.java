package com.qouteall.immersive_portals.mixin.block_manipulation;

import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity_B {
    @Shadow public abstract AbstractAttributeMap getAttributes();
    
    @Inject(
        method = "Lnet/minecraft/entity/LivingEntity;registerAttributes()V",
        at = @At("TAIL")
    )
    private void onInitAttributes(CallbackInfo ci) {
        getAttributes().registerAttribute(HandReachTweak.handReachMultiplierAttribute);
    }
}
