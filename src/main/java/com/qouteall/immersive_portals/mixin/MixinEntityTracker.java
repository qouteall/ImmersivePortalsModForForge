package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEEntityTracker;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.network.NetworkMain;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TrackedEntity;
import net.minecraft.world.server.ChunkHolder;
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
public class MixinEntityTracker implements IEEntityTracker {
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
        NetworkMain.sendRedirected(
            serverPlayNetworkHandler.player, entity.dimension, packet_1
        );
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
        NetworkMain.sendRedirected(
            serverPlayNetworkHandler.player, entity.dimension, packet_1
        );
    }
    
    //copied
    private static int getChebyshevDistance(
        ChunkPos chunkPos_1,
        ServerPlayerEntity serverPlayerEntity_1,
        boolean boolean_1
    ) {
        int int_3;
        int int_4;
        if (boolean_1) {
            SectionPos chunkSectionPos_1 = serverPlayerEntity_1.getManagedSectionPos();
            int_3 = chunkSectionPos_1.getSectionX();
            int_4 = chunkSectionPos_1.getSectionZ();
        }
        else {
            int_3 = MathHelper.floor(serverPlayerEntity_1.getPosX() / 16.0D);
            int_4 = MathHelper.floor(serverPlayerEntity_1.getPosZ() / 16.0D);
        }
        
        return getChebyshevDistance(chunkPos_1, int_3, int_4);
    }
    
    //copied
    private static int getChebyshevDistance(ChunkPos chunkPos_1, int int_1, int int_2) {
        int int_3 = chunkPos_1.x - int_1;
        int int_4 = chunkPos_1.z - int_2;
        return Math.max(Math.abs(int_3), Math.abs(int_4));
    }
    
    /**
     * @author qouteall
     * @reason
     */
    @Overwrite
    public void updateTrackingState(ServerPlayerEntity player) {
        updateCameraPosition_(player);
    }
    
    /**
     * @author qouteall
     * @reason
     */
    @Overwrite
    public void updateTrackingState(List<ServerPlayerEntity> list_1) {
        //ignore the argument
    
        McHelper.getRawPlayerList().forEach(this::updateTrackingState);
        
    }
    
    @Override
    public Entity getEntity_() {
        return entity;
    }
    
    @Override
    public void updateCameraPosition_(ServerPlayerEntity player) {
        IEThreadedAnvilChunkStorage storage = McHelper.getIEStorage(entity.dimension);
        
        if (player != this.entity) {
            Vec3d relativePos = (player.getPositionVec()).subtract(this.entry.func_219456_b());
            int maxWatchDistance = Math.min(
                this.range,
                (storage.getWatchDistance() - 1) * 16
            );
            boolean isWatchedNow =
                NewChunkTrackingGraph.isPlayerWatchingChunkWithinRaidus(
                    player,
                    entity.dimension,
                    entity.chunkCoordX,
                    entity.chunkCoordZ,
                    maxWatchDistance
                ) &&
                    this.entity.isSpectatedByPlayer(player);
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
                    this.entry.track(player);
                }
            }
            else if (this.trackingPlayers.remove(player)) {
                this.entry.untrack(player);
            }
            
        }
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        trackingPlayers.remove(oldPlayer);
    }
}
