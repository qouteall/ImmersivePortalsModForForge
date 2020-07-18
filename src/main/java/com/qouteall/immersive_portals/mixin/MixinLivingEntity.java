package com.qouteall.immersive_portals.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    
    @Shadow
    private LivingEntity revengeTarget;
    
    @Shadow
    private LivingEntity lastAttackedEntity;
    
    //maybe avoid memory leak???
    @Inject(method = "Lnet/minecraft/entity/LivingEntity;tick()V", at = @At("RETURN"))
    private void onTickEnded(CallbackInfo ci) {
        Entity this_ = (Entity) (Object) this;
        if (revengeTarget != null) {
            if (revengeTarget.world != this_.world) {
                revengeTarget = null;
            }
        }
        if (lastAttackedEntity != null) {
            if (lastAttackedEntity.world != this_.world) {
                lastAttackedEntity = null;
            }
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/entity/LivingEntity;canEntityBeSeen(Lnet/minecraft/entity/Entity;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCanSee(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity.world != ((Entity) (Object) this).world) {
            cir.setReturnValue(false);
        }
    }
}
