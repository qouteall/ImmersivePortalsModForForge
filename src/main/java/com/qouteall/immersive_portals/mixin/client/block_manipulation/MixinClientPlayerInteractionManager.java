package com.qouteall.immersive_portals.mixin.client.block_manipulation;

import com.qouteall.hiding_in_the_bushes.MyNetworkClient;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import com.qouteall.immersive_portals.ducks.IEClientPlayerInteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerController.class)
public abstract class MixinClientPlayerInteractionManager implements IEClientPlayerInteractionManager {
    @Shadow
    @Final
    private Minecraft mc;
    
    @Shadow
    @Final
    private ClientPlayNetHandler connection;
    
    @Inject(
        method = "Lnet/minecraft/client/multiplayer/PlayerController;sendDiggingPacket(Lnet/minecraft/network/play/client/CPlayerDiggingPacket$Action;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/Direction;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendPlayerAction(
        CPlayerDiggingPacket.Action action,
        BlockPos blockPos,
        Direction direction,
        CallbackInfo ci
    ) {
        if (BlockManipulationClient.isContextSwitched) {
            this.connection.sendPacket(
                MyNetworkClient.createCtsPlayerAction(
                    BlockManipulationClient.remotePointedDim,
                    new CPlayerDiggingPacket(action, blockPos, direction)
                )
            );
            ci.cancel();
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/multiplayer/PlayerController;func_217292_a(Lnet/minecraft/client/entity/player/ClientPlayerEntity;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/util/Hand;Lnet/minecraft/util/math/BlockRayTraceResult;)Lnet/minecraft/util/ActionResultType;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/play/ClientPlayNetHandler;sendPacket(Lnet/minecraft/network/IPacket;)V"
        )
    )
    private void redirectSendPacketOnInteractBlock(
        ClientPlayNetHandler clientPlayNetworkHandler,
        IPacket<?> packet
    ) {
        if (BlockManipulationClient.isContextSwitched) {
            clientPlayNetworkHandler.sendPacket(
                MyNetworkClient.createCtsRightClick(
                    BlockManipulationClient.remotePointedDim,
                    ((CPlayerTryUseItemOnBlockPacket) packet)
                )
            );
        }
        else {
            clientPlayNetworkHandler.sendPacket(packet);
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/multiplayer/PlayerController;extendedReach()Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onHasExtendedReach(CallbackInfoReturnable<Boolean> cir) {
        if (Global.longerReachInCreative) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/multiplayer/PlayerController;getBlockReachDistance()F",
        at = @At("RETURN"),
        cancellable = true,
        require = 0
    )
    private void onGetReachDistance(CallbackInfoReturnable<Float> cir) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        double result = cir.getReturnValue() *
            HandReachTweak.getActualHandReachMultiplier(player);
        cir.setReturnValue((float) result);
        cir.cancel();
    }
    
}
