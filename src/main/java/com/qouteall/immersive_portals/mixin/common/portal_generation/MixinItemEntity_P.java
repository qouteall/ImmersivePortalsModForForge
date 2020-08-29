package com.qouteall.immersive_portals.mixin.common.portal_generation;

import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGenManagement;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity_P {
    @Shadow
    public abstract ItemStack getItem();
    
    @Inject(
        method = "Lnet/minecraft/entity/item/ItemEntity;tick()V",
        at = @At("TAIL")
    )
    private void onItemTickEnded(CallbackInfo ci) {
        ItemEntity this_ = (ItemEntity) (Object) this;
        if (this_.removed) {
            return;
        }
        
        if (this_.world.isRemote()) {
            return;
        }
        
        this_.world.getProfiler().startSection("imm_ptl_item_tick");
        CustomPortalGenManagement.onItemTick(this_);
        this_.world.getProfiler().endSection();
    }
}
