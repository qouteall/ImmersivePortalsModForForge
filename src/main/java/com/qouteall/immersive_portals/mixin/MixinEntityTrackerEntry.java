package com.qouteall.immersive_portals.mixin;

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
@Mixin(TrackedEntity.class)
public abstract class MixinEntityTrackerEntry {
    @Shadow
    @Final
    private Entity entity;
    
    @Shadow
    public abstract void sendPackets(Consumer<IPacket<?>> consumer_1);
    
    private void sendRedirectedMessage(
        ServerPlayNetHandler serverPlayNetworkHandler,
        IPacket<?> packet_1
    ) {
        serverPlayNetworkHandler.sendPacket(
            MyNetwork.createRedirectedMessage(
                entity.dimension,
                packet_1
            )
        );
    }
    
    @Redirect(
        method = "Lnet/minecraft/server/network/EntityTrackerEntry;method_18756()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"
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
     * overwrite because method reference can not be redirected
     */
    @Overwrite
    public void startTracking(ServerPlayerEntity serverPlayerEntity_1) {
        ServerPlayNetHandler networkHandler = serverPlayerEntity_1.connection;
        this.sendPackets(packet -> sendRedirectedMessage(networkHandler, packet));
        this.entity.onStartedTrackingBy(serverPlayerEntity_1);
        serverPlayerEntity_1.onStartedTracking(this.entity);
    }
    
    @Redirect(
        method = "Lnet/minecraft/server/network/EntityTrackerEntry;method_18758(Lnet/minecraft/network/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"
        )
    )
    private void onSendToWatcherAndSelf(
        ServerPlayNetHandler serverPlayNetworkHandler,
        IPacket<?> packet_1
    ) {
        sendRedirectedMessage(serverPlayNetworkHandler, packet_1);
    }
}
