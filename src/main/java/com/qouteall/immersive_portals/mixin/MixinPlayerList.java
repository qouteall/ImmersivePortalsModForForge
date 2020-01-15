package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.network.StcDimensionInfo;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.management.PlayerList;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerList.class)
public class MixinPlayerList {
    @Inject(
        method = "recreatePlayerEntity",
        at = @At("HEAD")
    )
    private void onPlayerRespawn(
        ServerPlayerEntity oldPlayer,
        DimensionType dimensionType_1,
        boolean boolean_1,
        CallbackInfoReturnable<ServerPlayerEntity> cir
    ) {
        SGlobal.chunkDataSyncManager.onPlayerRespawn(oldPlayer);
    }
    
    @Inject(
        method = "recreatePlayerEntity",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onPlayerReapawnFinished(
        ServerPlayerEntity serverPlayerEntity_1,
        DimensionType dimensionType_1,
        boolean boolean_1,
        CallbackInfoReturnable<ServerPlayerEntity> cir
    ) {
        GlobalPortalStorage.onPlayerLoggedIn(cir.getReturnValue());
    }
    
    @Inject(
        method = "initializeConnectionToPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/fml/network/NetworkHooks;sendDimensionDataPacket(Lnet/minecraft/network/NetworkManager;Lnet/minecraft/entity/player/ServerPlayerEntity;)V"
        )
    )
    private void onJustBeforeInitializingConnection(
        NetworkManager netManager,
        ServerPlayerEntity playerIn,
        CallbackInfo ci
    ) {
        StcDimensionInfo.sendDimensionInfo(playerIn);
    }
    
    @Inject(method = "initializeConnectionToPlayer", at = @At("TAIL"))
    private void onOnPlayerConnect(
        NetworkManager netManager,
        ServerPlayerEntity playerIn,
        CallbackInfo ci
    ) {
        GlobalPortalStorage.onPlayerLoggedIn(playerIn);
    }
    
}
