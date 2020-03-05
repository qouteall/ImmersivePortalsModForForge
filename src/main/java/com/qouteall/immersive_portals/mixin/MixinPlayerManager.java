package com.qouteall.immersive_portals.mixin;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.server.management.PlayerList;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerList.class)
public class MixinPlayerManager {
    @Shadow
    @Final
    private List<ServerPlayerEntity> players;
    
    @Inject(method = "Lnet/minecraft/server/management/PlayerList;sendWorldInfo(Lnet/minecraft/entity/player/ServerPlayerEntity;Lnet/minecraft/world/server/ServerWorld;)V", at = @At("RETURN"))
    private void onSendWorldInfo(ServerPlayerEntity player, ServerWorld world, CallbackInfo ci) {
        if (!Global.serverTeleportationManager.isFiringMyChangeDimensionEvent) {
            GlobalPortalStorage.onPlayerLoggedIn(player);
        }
    }
    
    //sometimes the server side player dimension is not same as client
    //so redirect it
    @Inject(
        method = "Lnet/minecraft/server/management/PlayerList;sendPacketToAllPlayersInDimension(Lnet/minecraft/network/IPacket;Lnet/minecraft/world/dimension/DimensionType;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    public void sendToDimension(IPacket<?> packet, DimensionType dimension, CallbackInfo ci) {
        players.stream()
            .filter(player -> player.dimension == dimension)
            .forEach(player -> player.connection.sendPacket(
                MyNetwork.createRedirectedMessage(
                    dimension,
                    packet
                )
            ));
        ci.cancel();
    }
}
