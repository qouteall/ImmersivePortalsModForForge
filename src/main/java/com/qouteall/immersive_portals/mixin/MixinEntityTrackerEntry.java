package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.network.NetworkMain;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.world.TrackedEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

//NOTE must redirect all packets about entities
@Mixin(value = TrackedEntity.class)
public abstract class MixinEntityTrackerEntry {
    @Shadow
    @Final
    private Entity trackedEntity;
    
    @Shadow
    public abstract void sendSpawnPackets(Consumer<IPacket<?>> consumer_1);
    
    private void sendRedirectedMessage(
        ServerPlayNetHandler serverPlayNetworkHandler,
        IPacket<?> packet_1
    ) {
        NetworkMain.sendRedirected(
            serverPlayNetworkHandler.player, trackedEntity.dimension, packet_1
        );
    }
    
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/play/ServerPlayNetHandler;sendPacket(Lnet/minecraft/network/IPacket;)V"
        )
    )
    private void onSendPositionSyncPacket(
        ServerPlayNetHandler serverPlayNetworkHandler,
        IPacket<?> packet_1
    ) {
        sendRedirectedMessage(serverPlayNetworkHandler, packet_1);
    }
    
    /**
     * @author qouteall
     * @reason method reference can not be redirected
     */
    @Overwrite
    public void track(ServerPlayerEntity serverPlayerEntity_1) {
        ServerPlayNetHandler networkHandler = serverPlayerEntity_1.connection;
        this.sendSpawnPackets(packet -> sendRedirectedMessage(networkHandler, packet));
        this.trackedEntity.addTrackingPlayer(serverPlayerEntity_1);
        serverPlayerEntity_1.addEntity(this.trackedEntity);
    }
    
    @Redirect(
        method = "sendPacket",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/play/ServerPlayNetHandler;sendPacket(Lnet/minecraft/network/IPacket;)V"
        )
    )
    private void onSendToWatcherAndSelf(
        ServerPlayNetHandler serverPlayNetworkHandler,
        IPacket<?> packet_1
    ) {
        sendRedirectedMessage(serverPlayNetworkHandler, packet_1);
    }
}
