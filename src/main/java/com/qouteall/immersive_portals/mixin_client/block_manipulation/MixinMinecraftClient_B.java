package com.qouteall.immersive_portals.mixin_client.block_manipulation;

import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraftClient_B {
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;sendClickBlockToController(Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/entity/player/ClientPlayerEntity;isHandActive()Z"
        ),
        cancellable = true
    )
    private void onHandleBlockBreaking(boolean isKeyPressed, CallbackInfo ci) {
        if (BlockManipulationClient.isPointingToRemoteBlock()) {
            BlockManipulationClient.myHandleBlockBreaking(isKeyPressed);
            ci.cancel();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;clickMouse()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/RayTraceResult;getType()Lnet/minecraft/util/math/RayTraceResult$Type;"
        ),
        cancellable = true
    )
    private void onDoAttack(CallbackInfo ci) {
        if (BlockManipulationClient.isPointingToRemoteBlock()) {
            BlockManipulationClient.myAttackBlock();
            ci.cancel();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;rightClickMouse()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/RayTraceResult;getType()Lnet/minecraft/util/math/RayTraceResult$Type;"
        ),
        cancellable = true
    )
    private void onDoItemUse(CallbackInfo ci) {
        if (BlockManipulationClient.isPointingToRemoteBlock()) {
            //TODO support offhand
            BlockManipulationClient.myItemUse(Hand.MAIN_HAND);
            ci.cancel();
        }
    }
}
