package com.qouteall.immersive_portals.mixin.portal_generation;

import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGenManagement;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
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
        
        final int interval = 19;
        
        // check every 19 ticks
        if (this_.getEntityId() % interval == this_.world.getGameTime() % interval) {
            this_.world.getProfiler().startSection("imm_ptl_item_tick");
            CustomPortalGenManagement.onItemTick(this_);
            this_.world.getProfiler().endSection();
        }
    }
}
