package com.qouteall.immersive_portals.mixin.entity_sync;

import com.qouteall.hiding_in_the_bushes.network.NetworkMain;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.world.TrackedEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    
    @Inject(
        method = "track",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/TrackedEntity;sendSpawnPackets(Ljava/util/function/Consumer;)V"
        )
    )
    private void injectSendpacketsOnStartTracking(ServerPlayerEntity player, CallbackInfo ci) {
        this.sendSpawnPackets(packet -> sendRedirectedMessage(player.connection, packet));
    }
    
    @Redirect(
        method = "track",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/TrackedEntity;sendSpawnPackets(Ljava/util/function/Consumer;)V"
        )
    )
    private void redirectSendPacketsOnStartTracking(
        TrackedEntity trackedEntity,
        Consumer<IPacket<?>> p_219452_1_
    ) {
        //nothing
    }

//    /**
//     * @author qouteall
//     * @reason method reference can not be redirected
//     */
//    @Overwrite
//    public void track(ServerPlayerEntity serverPlayerEntity_1) {
//        ServerPlayNetHandler networkHandler = serverPlayerEntity_1.connection;
//        this.sendSpawnPackets(packet -> sendRedirectedMessage(networkHandler, packet));
//        this.trackedEntity.addTrackingPlayer(serverPlayerEntity_1);
//        serverPlayerEntity_1.addEntity(this.trackedEntity);
//
//        net.minecraftforge.event.ForgeEventFactory.onStartEntityTracking(
//            this.trackedEntity, serverPlayerEntity_1
//        );
//    }
    
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
