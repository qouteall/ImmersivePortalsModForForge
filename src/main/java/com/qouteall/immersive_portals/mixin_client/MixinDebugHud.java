package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.render.RenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.client.gui.overlay.DebugOverlayGui;

@Mixin(DebugOverlayGui.class)
public class MixinDebugHud {
    @Inject(method = "getRightText", at = @At("RETURN"), cancellable = true)
    private void onGetRightText(CallbackInfoReturnable<List<String>> cir) {
        cir.getReturnValue().add("Rendered Portal Num: " + RenderHelper.lastPortalRenderInfos.size());
    }
}
