package com.qouteall.hiding_in_the_bushes.mixin;

import com.qouteall.hiding_in_the_bushes.DimensionSyncManager;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.management.PlayerList;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerList.class)
public class MixinPlayerManager_MA {
    @Shadow
    @Final
    private List<ServerPlayerEntity> players;
    
    @Inject(
        method = "Lnet/minecraft/server/management/PlayerList;recreatePlayerEntity(Lnet/minecraft/entity/player/ServerPlayerEntity;Lnet/minecraft/world/dimension/DimensionType;Z)Lnet/minecraft/entity/player/ServerPlayerEntity;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/management/PlayerList;removePlayer(Lnet/minecraft/entity/player/ServerPlayerEntity;)Z",
            remap = false
        )
    )
    private void onPlayerRespawn(
        ServerPlayerEntity oldPlayer,
        DimensionType dimensionType_1,
        boolean boolean_1,
        CallbackInfoReturnable<ServerPlayerEntity> cir
    ) {
        if (oldPlayer.dimension == dimensionType_1) {
            Helper.log("Avoided refreshing chunk visibility for player");
            return;
        }
        Global.chunkDataSyncManager.onPlayerRespawn(oldPlayer);
    }
    
    @Inject(
        method = "Lnet/minecraft/server/management/PlayerList;initializeConnectionToPlayer(Lnet/minecraft/network/NetworkManager;Lnet/minecraft/entity/player/ServerPlayerEntity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/fml/network/NetworkHooks;sendDimensionDataPacket(Lnet/minecraft/network/NetworkManager;Lnet/minecraft/entity/player/ServerPlayerEntity;)V",
            remap = false
        )
    )
    private void onJustBeforeInitializingConnection(
        NetworkManager netManager,
        ServerPlayerEntity playerIn,
        CallbackInfo ci
    ) {
        DimensionSyncManager.sendDimensionInfo(playerIn);
    }
    
}
