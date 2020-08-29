package com.qouteall.immersive_portals.mixin.common.entity_sync;

import com.qouteall.immersive_portals.chunk_loading.EntitySync;
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
@Mixin(TrackedEntity.class)
public abstract class MixinEntityTrackerEntry {
    @Shadow
    @Final
    private Entity trackedEntity;
    
    @Shadow
    public abstract void sendSpawnPackets(Consumer<IPacket<?>> consumer_1);
    
    @Redirect(
        method = "Lnet/minecraft/world/TrackedEntity;tick()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/play/ServerPlayNetHandler;sendPacket(Lnet/minecraft/network/IPacket;)V"
        )
    )
    private void onSendPositionSyncPacket(
        ServerPlayNetHandler serverPlayNetworkHandler,
        IPacket<?> packet_1
    ) {
        EntitySync.sendRedirectedPacket(serverPlayNetworkHandler, packet_1, trackedEntity.world.func_234923_W_());
    }
    
    @Inject(
        method = "Lnet/minecraft/world/TrackedEntity;track(Lnet/minecraft/entity/player/ServerPlayerEntity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/TrackedEntity;sendSpawnPackets(Ljava/util/function/Consumer;)V"
        )
    )
    private void injectSendpacketsOnStartTracking(ServerPlayerEntity player, CallbackInfo ci) {
        this.sendSpawnPackets(packet -> EntitySync.sendRedirectedPacket(player.connection, packet, trackedEntity.world.func_234923_W_()));
    }
    
    @Redirect(
        method = "Lnet/minecraft/world/TrackedEntity;track(Lnet/minecraft/entity/player/ServerPlayerEntity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/TrackedEntity;sendSpawnPackets(Ljava/util/function/Consumer;)V"
        )
    )
    private void redirectSendPacketsOnStartTracking(
        TrackedEntity entityTrackerEntry,
        Consumer<IPacket<?>> sender
    ) {
        //nothing
    }
    
    @Redirect(
        method = "Lnet/minecraft/world/TrackedEntity;sendPacket(Lnet/minecraft/network/IPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/play/ServerPlayNetHandler;sendPacket(Lnet/minecraft/network/IPacket;)V"
        )
    )
    private void onSendToWatcherAndSelf(
        ServerPlayNetHandler serverPlayNetworkHandler,
        IPacket<?> packet_1
    ) {
        EntitySync.sendRedirectedPacket(serverPlayNetworkHandler, packet_1, trackedEntity.world.func_234923_W_());
    }
}
