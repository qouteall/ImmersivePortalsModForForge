package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.overlay.DebugOverlayGui;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugOverlayGui.class)
public class MixinDebugHud {
    @Inject(method = "Lnet/minecraft/client/gui/overlay/DebugOverlayGui;getDebugInfoRight()Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void onGetRightText(CallbackInfoReturnable<List<String>> cir) {
        List<String> returnValue = cir.getReturnValue();
        returnValue.add("Rendered Portal Num: " + MyRenderHelper.lastPortalRenderInfos.size());
        ClientWorld world = Minecraft.getInstance().world;
        if (world != null) {
            returnValue.add("In: " + world.dimension.getType());
        }
    }
}
