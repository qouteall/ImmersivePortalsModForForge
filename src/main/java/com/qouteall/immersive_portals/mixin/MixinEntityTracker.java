package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import com.qouteall.immersive_portals.exposer.IEEntityTracker;
import com.qouteall.immersive_portals.exposer.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TrackedEntity;
import net.minecraft.world.chunk.ChunkHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Set;

//NOTE must redirect all packets about entities
@Mixin(targets = "net.minecraft.server.world.ThreadedAnvilChunkStorage$EntityTracker")
public class MixinEntityTracker implements IEEntityTracker {
    @Shadow
    @Final
    private TrackedEntity entry;
    @Shadow
    @Final
    private Entity entity;
    @Shadow
    @Final
    private int maxDistance;
    @Shadow
    private SectionPos lastCameraPosition;
    @Shadow
    @Final
    private Set<ServerPlayerEntity> playersTracking;
    
    @Redirect(
        method = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage$EntityTracker;sendToOtherNearbyPlayers(Lnet/minecraft/network/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"
        )
    )
    private void onSendToOtherNearbyPlayers(
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
        method = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage$EntityTracker;sendToNearbyPlayers(Lnet/minecraft/network/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"
        )
    )
    private void onSendToNearbyPlayers(
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
    
    //copied
    private static int getChebyshevDistance(
        ChunkPos chunkPos_1,
        ServerPlayerEntity serverPlayerEntity_1,
        boolean boolean_1
    ) {
        int int_3;
        int int_4;
        if (boolean_1) {
            SectionPos chunkSectionPos_1 = serverPlayerEntity_1.getCameraPosition();
            int_3 = chunkSectionPos_1.getChunkX();
            int_4 = chunkSectionPos_1.getChunkZ();
        }
        else {
            int_3 = MathHelper.floor(serverPlayerEntity_1.posX / 16.0D);
            int_4 = MathHelper.floor(serverPlayerEntity_1.posZ / 16.0D);
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
     */
    @Overwrite
    public void updateCameraPosition(ServerPlayerEntity player) {
        updateCameraPosition_(player);
    }
    
    /**
     * @author qouteall
     * performance may be slowed down
     */
    @Overwrite
    public void updateCameraPosition(List<ServerPlayerEntity> list_1) {
        //ignore the argument
    
        Helper.getCopiedPlayerList().forEach(this::updateCameraPosition);
        
    }
    
    @Override
    public Entity getEntity_() {
        return entity;
    }
    
    @Override
    public void updateCameraPosition_(ServerPlayerEntity player) {
        IEThreadedAnvilChunkStorage storage = Helper.getIEStorage(entity.dimension);
        
        if (player != this.entity) {
            Vec3d relativePos = (new Vec3d(
                player.posX,
                player.posY,
                player.posZ
            )).subtract(this.entry.method_18759());
            int maxWatchDistance = Math.min(
                this.maxDistance,
                (storage.getWatchDistance() - 1) * 16
            );
            boolean isWatchedNow =
                player.dimension == entity.dimension &&
                    relativePos.x >= (double) (-maxWatchDistance) &&
                    relativePos.x <= (double) maxWatchDistance &&
                    relativePos.z >= (double) (-maxWatchDistance) &&
                    relativePos.z <= (double) maxWatchDistance &&
                    this.entity.canBeSpectated(player);
            isWatchedNow = isWatchedNow ||
                SGlobal.chunkTracker.isPlayerWatchingChunk(
                    player,
                    new DimensionalChunkPos(
                        entity.dimension,
                        new ChunkPos(entity.getBlockPos())
                    )
                );
            if (isWatchedNow) {
                boolean shouldTrack = this.entity.forceSpawn;
                if (!shouldTrack) {
                    ChunkPos chunkPos_1 = new ChunkPos(this.entity.chunkCoordX, this.entity.chunkCoordZ);
                    ChunkHolder chunkHolder_1 = storage.getChunkHolder_(chunkPos_1.toLong());
                    if (chunkHolder_1 != null && chunkHolder_1.getWorldChunk() != null) {
                        shouldTrack = true;
                    }
//                    else {
                        //retry it next tick
//                        ModMain.serverTaskList.addTask(() -> {
//                            Helper.log("Retry tracking player " + player.getName());
//                            updateCameraPosition_(player);
//                            return true;
//                        });
//                    }
                }
    
                if (shouldTrack && this.playersTracking.add(player)) {
                    this.entry.startTracking(player);
                }
            }
            else if (this.playersTracking.remove(player)) {
                this.entry.stopTracking(player);
            }
            
        }
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        playersTracking.remove(oldPlayer);
    }
}
