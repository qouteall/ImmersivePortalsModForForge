package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.overlay.DebugOverlayGui;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = DebugOverlayGui.class)
public class MixinDebugHud {
    @Inject(method = "getDebugInfoRight", at = @At("RETURN"), cancellable = true)
    private void onGetRightText(CallbackInfoReturnable<List<String>> cir) {
        cir.getReturnValue().add(getAdditionalDebugInfo());
    }
    
    private String getAdditionalDebugInfo() {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        Entity collidingPortal =
            player != null ? ((IEEntity) player).getCollidingPortal() : null;
        return String.format(
            "Rendered Portal Num: %s",
            MyRenderHelper.lastPortalRenderInfos.size()
        );
    }
}
