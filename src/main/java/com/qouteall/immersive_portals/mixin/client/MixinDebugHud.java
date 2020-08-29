package com.qouteall.immersive_portals.mixin.client;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.overlay.DebugOverlayGui;

@Mixin(DebugOverlayGui.class)
public class MixinDebugHud {
    @Inject(method = "Lnet/minecraft/client/gui/overlay/DebugOverlayGui;getDebugInfoRight()Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void onGetRightText(CallbackInfoReturnable<List<String>> cir) {
        List<String> returnValue = cir.getReturnValue();
        returnValue.add("Rendered Portal Num: " + RenderStates.lastPortalRenderInfos.size());
        
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null) {
            Portal collidingPortal = ((IEEntity) player).getCollidingPortal();
            if (collidingPortal != null) {
                String text = "Colliding " + collidingPortal.toString();
                returnValue.addAll(Helper.splitStringByLen(text, 50));
            }
        }
        
        if (RenderStates.debugText != null && !RenderStates.debugText.isEmpty()) {
            returnValue.add("Debug: " + RenderStates.debugText);
        }
    }
}
