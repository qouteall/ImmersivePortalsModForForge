package com.qouteall.immersive_portals.mixin.common.portal_generation;

import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGenManagement;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class MixinItemStack {
    @Inject(
        method = "Lnet/minecraft/item/ItemStack;onItemUse(Lnet/minecraft/item/ItemUseContext;)Lnet/minecraft/util/ActionResultType;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onUseOnBlockEnded(ItemUseContext context, CallbackInfoReturnable<ActionResultType> cir) {
        CustomPortalGenManagement.onItemUse(context, cir.getReturnValue());
    }
}
