package com.qouteall.immersive_portals.mixin.client.block_manipulation;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient_B {
    @Shadow
    protected abstract void middleClickMouse();
    
    @Shadow
    public ClientWorld world;
    
    @Shadow
    public RayTraceResult objectMouseOver;
    
    @Shadow
    protected int leftClickCounter;
    
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;sendClickBlockToController(Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/entity/player/ClientPlayerEntity;isHandActive()Z"
        ),
        cancellable = true
    )
    private void onHandleBlockBreaking(boolean isKeyPressed, CallbackInfo ci) {
        if (BlockManipulationClient.isPointingToPortal()) {
            BlockManipulationClient.myHandleBlockBreaking(isKeyPressed);
            ci.cancel();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;clickMouse()V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDoAttack(CallbackInfo ci) {
        if (leftClickCounter <= 0) {
            if (BlockManipulationClient.isPointingToPortal()) {
                BlockManipulationClient.myAttackBlock();
                ci.cancel();
            }
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;rightClickMouse()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/entity/player/ClientPlayerEntity;getHeldItem(Lnet/minecraft/util/Hand;)Lnet/minecraft/item/ItemStack;"
        ),
        cancellable = true
    )
    private void onDoItemUse(CallbackInfo ci) {
        if (BlockManipulationClient.isPointingToPortal()) {
            // supporting offhand is unnecessary
            BlockManipulationClient.myItemUse(Hand.MAIN_HAND);
            ci.cancel();
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/Minecraft;processKeyBinds()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;middleClickMouse()V"
        )
    )
    private void redirectDoItemPick(Minecraft minecraftClient) {
        if (BlockManipulationClient.isPointingToPortal()) {
            ClientWorld remoteWorld = CGlobal.clientWorldLoader.getWorld(
                BlockManipulationClient.remotePointedDim
            );
            ClientWorld oldWorld = this.world;
            RayTraceResult oldTarget = this.objectMouseOver;
            
            world = remoteWorld;
            objectMouseOver = BlockManipulationClient.remoteHitResult;
            
            middleClickMouse();
            
            world = oldWorld;
            objectMouseOver = oldTarget;
        }
        else {
            middleClickMouse();
        }
    }
}
