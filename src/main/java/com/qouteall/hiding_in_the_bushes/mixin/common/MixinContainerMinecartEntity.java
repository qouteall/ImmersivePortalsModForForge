package com.qouteall.hiding_in_the_bushes.mixin.common;

import net.minecraft.entity.item.minecart.ContainerMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerMinecartEntity.class)
public class MixinContainerMinecartEntity {
    @Shadow
    private boolean dropContentsWhenDead;
    
    //make check minecart do not drop item when crossing portal
    //only happens in forge
    @Inject(method = "remove", at = @At("HEAD"), remap = false)
    private void onRemoveHead(boolean keepData, CallbackInfo ci) {
        if (keepData) {
            dropContentsWhenDead = false;
        }
    }
    
    @Inject(method = "remove", at = @At("RETURN"), remap = false)
    private void onRemoveReturn(boolean keepData, CallbackInfo ci) {
        if (keepData) {
            dropContentsWhenDead = true;
        }
    }
}
