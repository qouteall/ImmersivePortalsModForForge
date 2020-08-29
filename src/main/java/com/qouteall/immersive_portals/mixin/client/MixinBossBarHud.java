package com.qouteall.immersive_portals.mixin.client;

import net.minecraft.client.gui.overlay.BossOverlayGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BossOverlayGui.class)
public class MixinBossBarHud {
    @Inject(method = "Lnet/minecraft/client/gui/overlay/BossOverlayGui;shouldCreateFog()Z", at = @At("HEAD"), cancellable = true)
    private void onShouldThickenFog(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
        cir.cancel();
    }
}
