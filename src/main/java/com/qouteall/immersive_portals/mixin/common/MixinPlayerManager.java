package com.qouteall.immersive_portals.mixin.common;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
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
    
    @Shadow
    @Final
    private MinecraftServer server;
    
    @Inject(
        method = "Lnet/minecraft/server/management/PlayerList;initializeConnectionToPlayer(Lnet/minecraft/network/NetworkManager;Lnet/minecraft/entity/player/ServerPlayerEntity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/play/server/SJoinGamePacket;<init>(ILnet/minecraft/world/GameType;Lnet/minecraft/world/GameType;JZLjava/util/Set;Lnet/minecraft/util/registry/DynamicRegistries$Impl;Lnet/minecraft/world/DimensionType;Lnet/minecraft/util/RegistryKey;IIZZZZ)V"
        )
    )
    private void onConnectionEstablished(
        NetworkManager connection,
        ServerPlayerEntity player,
        CallbackInfo ci
    ) {
        player.connection.sendPacket(MyNetwork.createDimSync());
    }
    
    @Inject(method = "Lnet/minecraft/server/management/PlayerList;sendWorldInfo(Lnet/minecraft/entity/player/ServerPlayerEntity;Lnet/minecraft/world/server/ServerWorld;)V", at = @At("RETURN"))
    private void onSendWorldInfo(ServerPlayerEntity player, ServerWorld world, CallbackInfo ci) {
        if (!Global.serverTeleportationManager.isFiringMyChangeDimensionEvent) {
            GlobalPortalStorage.onPlayerLoggedIn(player);
        }
    }
    
    @Inject(method = "Lnet/minecraft/server/management/PlayerList;initializeConnectionToPlayer(Lnet/minecraft/network/NetworkManager;Lnet/minecraft/entity/player/ServerPlayerEntity;)V", at = @At("TAIL"))
    private void onOnPlayerConnect(NetworkManager connection, ServerPlayerEntity player, CallbackInfo ci) {
        NewChunkTrackingGraph.updateForPlayer(player);
    }
    
    //with redirection
    @Inject(
        method = "Lnet/minecraft/server/management/PlayerList;func_232642_a_(Lnet/minecraft/network/IPacket;Lnet/minecraft/util/RegistryKey;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    public void sendToDimension(IPacket<?> packet, RegistryKey<World> dimension, CallbackInfo ci) {
        for (ServerPlayerEntity player : players) {
            if (player.world.func_234923_W_() == dimension) {
                player.connection.sendPacket(
                    MyNetwork.createRedirectedMessage(
                        dimension,
                        packet
                    )
                );
            }
        }
        
        ci.cancel();
    }
    
    /**
     * @author qoutall
     * mostly for sound events
     */
    @Overwrite
    public void sendToAllNearExcept(
        @Nullable PlayerEntity excludingPlayer,
        double x, double y, double z, double distance,
        RegistryKey<World> dimension, IPacket<?> packet
    ) {
        ChunkPos chunkPos = new ChunkPos(new BlockPos(new Vector3d(x, y, z)));
        
        NewChunkTrackingGraph.getPlayersViewingChunk(
            dimension, chunkPos.x, chunkPos.z
        ).filter(playerEntity -> NewChunkTrackingGraph.isPlayerWatchingChunkWithinRaidus(
            playerEntity, dimension, chunkPos.x, chunkPos.z, (int) distance + 16
        )).forEach(playerEntity -> {
            if (playerEntity != excludingPlayer) {
                playerEntity.connection.sendPacket(MyNetwork.createRedirectedMessage(
                    dimension, packet
                ));
            }
        });
    }
}
