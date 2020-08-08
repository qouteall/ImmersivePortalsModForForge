package com.qouteall.immersive_portals.mixin.entity_sync;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.chunk_loading.EntitySync;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEEntityTracker;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.TrackedEntity;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Set;

//NOTE must redirect all packets about entities
@Mixin(targets = "net.minecraft.world.server.ChunkManager$EntityTracker")
public abstract class MixinEntityTracker implements IEEntityTracker {
    @Shadow
    @Final
    private TrackedEntity entry;
    @Shadow
    @Final
    private Entity entity;
    @Shadow
    @Final
    private int range;
    @Shadow
    private SectionPos pos;
    @Shadow
    @Final
    private Set<ServerPlayerEntity> trackingPlayers;
    
    @Shadow
    public abstract void removeAllTrackers();
    
    @Shadow
    protected abstract int func_229843_b_();
    
    @Redirect(
        method = "Lnet/minecraft/world/server/ChunkManager$EntityTracker;sendToAllTracking(Lnet/minecraft/network/IPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/play/ServerPlayNetHandler;sendPacket(Lnet/minecraft/network/IPacket;)V"
        )
    )
    private void onSendToOtherNearbyPlayers(
        ServerPlayNetHandler serverPlayNetworkHandler,
        IPacket<?> packet_1
    ) {
        EntitySync.sendRedirectedPacket(serverPlayNetworkHandler, packet_1, entity.world.func_234923_W_());
    }
    
    @Redirect(
        method = "Lnet/minecraft/world/server/ChunkManager$EntityTracker;sendToTrackingAndSelf(Lnet/minecraft/network/IPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/play/ServerPlayNetHandler;sendPacket(Lnet/minecraft/network/IPacket;)V"
        )
    )
    private void onSendToNearbyPlayers(
        ServerPlayNetHandler serverPlayNetworkHandler,
        IPacket<?> packet_1
    ) {
        EntitySync.sendRedirectedPacket(serverPlayNetworkHandler, packet_1, entity.world.func_234923_W_());
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void updateTrackingState(ServerPlayerEntity player) {
        updateEntityTrackingStatus(player);
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void updateTrackingState(List<ServerPlayerEntity> list) {
        for (ServerPlayerEntity player : McHelper.getRawPlayerList()) {
            updateEntityTrackingStatus(player);
        }
    }
    
    @Override
    public Entity getEntity_() {
        return entity;
    }
    
    @Override
    public void updateEntityTrackingStatus(ServerPlayerEntity player) {
        IEThreadedAnvilChunkStorage storage = (IEThreadedAnvilChunkStorage)
            ((ServerWorld) entity.world).getChunkProvider().chunkManager;
        
        if (player == this.entity) {
            return;
        }
        
        int maxWatchDistance = Math.min(
            this.func_229843_b_(),
            (storage.getWatchDistance() - 1) * 16
        );
        boolean isWatchedNow =
            NewChunkTrackingGraph.isPlayerWatchingChunkWithinRaidus(
                player,
                this.entity.world.func_234923_W_(),
                this.entity.chunkCoordX,
                this.entity.chunkCoordZ,
                maxWatchDistance
            ) && this.entity.isSpectatedByPlayer(player);
        if (isWatchedNow) {
            boolean shouldTrack = this.entity.forceSpawn;
            if (!shouldTrack) {
                ChunkPos chunkPos_1 = new ChunkPos(this.entity.chunkCoordX, this.entity.chunkCoordZ);
                ChunkHolder chunkHolder_1 = storage.getChunkHolder_(chunkPos_1.asLong());
                if (chunkHolder_1 != null && chunkHolder_1.getChunkIfComplete() != null) {
                    shouldTrack = true;
                }
            }
            
            if (shouldTrack && this.trackingPlayers.add(player)) {
                EntitySync.withForceRedirect(
                    entity.world.func_234923_W_(),
                    () -> {
                        this.entry.track(player);
                    }
                );
            }
        }
        else if (this.trackingPlayers.remove(player)) {
            EntitySync.withForceRedirect(
                entity.world.func_234923_W_(),
                () -> {
                    this.entry.untrack(player);
                }
            );
        }
        
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        trackingPlayers.remove(oldPlayer);
        entry.untrack(oldPlayer);
    }
    
    @Override
    public void resendSpawnPacketToTrackers() {
        IPacket<?> spawnPacket = entity.createSpawnPacket();
        IPacket redirected = MyNetwork.createRedirectedMessage(entity.world.func_234923_W_(), spawnPacket);
        trackingPlayers.forEach(player -> {
            player.connection.sendPacket(redirected);
        });
    }
    
    @Override
    public void stopTrackingToAllPlayers_() {
        removeAllTrackers();
    }
    
    @Override
    public void tickEntry() {
        entry.tick();
    }
    
    @Override
    public SectionPos getLastCameraPosition() {
        return pos;
    }
    
    @Override
    public void setLastCameraPosition(SectionPos arg) {
        pos = arg;
    }
}
