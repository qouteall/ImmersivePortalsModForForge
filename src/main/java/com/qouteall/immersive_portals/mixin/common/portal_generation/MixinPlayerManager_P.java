package com.qouteall.immersive_portals.mixin.common.portal_generation;

import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGenManagement;
import net.minecraft.server.management.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class MixinPlayerManager_P {
    @Inject(
        method = "Lnet/minecraft/server/management/PlayerList;reloadResources()V",
        at = @At("RETURN")
    )
    private void onOnDatapackReloaded(CallbackInfo ci) {
        CustomPortalGenManagement.onDatapackReload();
    }
}
