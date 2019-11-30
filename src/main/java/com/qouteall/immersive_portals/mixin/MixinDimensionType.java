package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.LateLoadedHelper;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionType.class)
public abstract class MixinDimensionType {
    
    @Inject(
        method = "toString",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onToString(CallbackInfoReturnable<String> cir) {
        if (LateLoadedHelper.dimensionTypeToString != null) {
            //avoid loading some classes too early
            cir.setReturnValue(LateLoadedHelper.dimensionTypeToString.apply(this));
            cir.cancel();
        }
    }

    
}
