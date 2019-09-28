package com.qouteall.immersive_portals.chunk_loading;

import com.mojang.datafixers.util.Either;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.exposer.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SChunkDataPacket;
import net.minecraft.network.play.server.SUnloadChunkPacket;
import net.minecraft.network.play.server.SUpdateLightPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerChunkProvider;

import java.util.concurrent.CompletableFuture;

//the chunks near player are managed by vanilla
//we only manage the chunks that's seen by portal and not near player
//it is not multi-threaded like vanilla
public class ChunkDataSyncManager {
    
    private static final int unloadWaitingTickTime = 20 * 10;
    
    public ChunkDataSyncManager() {
        SGlobal.chunkTracker.beginWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onBeginWatch
        );
        SGlobal.chunkTracker.endWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onEndWatch
        );
    }
    
    /**
     * {@link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)}
     */
    private void onBeginWatch(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        if (isChunkManagedByVanilla(player, chunkPos)) {
            return;
        }
    
        if (SGlobal.chunkTracker.isChunkDataSent(player, chunkPos)) {
            return;
        }
    
        SGlobal.chunkTracker.onChunkDataSent(player, chunkPos);
        IEThreadedAnvilChunkStorage ieStorage = Helper.getIEStorage(chunkPos.dimension);
    
        if (SGlobal.isChunkLoadingMultiThreaded) {
            sendPacketMultiThreaded(player, chunkPos, ieStorage);
        }
        else {
            sendPacketNormally(player, chunkPos, ieStorage);
        }
    }
    
    private void sendPacketMultiThreaded(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos,
        IEThreadedAnvilChunkStorage ieStorage
    ) {
        ModMain.serverTaskList.addTask(() -> {
            ChunkHolder chunkHolder = ieStorage.getChunkHolder_(chunkPos.getChunkPos().asLong());
            if (chunkHolder == null) {
                //TODO cleanup it
                SGlobal.chunkTracker.setIsLoadedByPortal(
                    chunkPos.dimension,
                    chunkPos.getChunkPos(),
                    true
                );
                return false;
            }
    
            //create future
            CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> future = chunkHolder.func_219276_a(
                ChunkStatus.FULL,
                ((ChunkManager) ieStorage)
            );
            
            future.thenAcceptAsync(either -> {
                ModMain.serverTaskList.addTask(() -> {
                    sendWatchPackets(player, chunkPos, ieStorage);
                    return true;
                });
            });
            
            return true;
        });
    }
    
    private void sendPacketNormally(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos,
        IEThreadedAnvilChunkStorage ieStorage
    ) {
        sendWatchPackets(player, chunkPos, ieStorage);
    }
    
    private void sendWatchPackets(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos,
        IEThreadedAnvilChunkStorage ieStorage
    ) {
        IChunk chunk = Helper.getServer()
            .getWorld(chunkPos.dimension)
            .getChunk(chunkPos.x, chunkPos.z);
        assert chunk != null;
        assert !(chunk instanceof EmptyChunk);
        player.connection.sendPacket(
            MyNetwork.createRedirectedMessage(
                chunkPos.dimension,
                new SChunkDataPacket(
                    ((Chunk) chunk),
                    65535
                )
            )
        );
        
        player.connection.sendPacket(
            MyNetwork.createRedirectedMessage(
                chunkPos.dimension,
                new SUpdateLightPacket(
                    chunkPos.getChunkPos(),
                    ieStorage.getLightingProvider()
                )
            )
        );
    
        //update the entity trackers
        ((ServerChunkProvider) ieStorage).updatePlayerPosition(player);
    }
    
    private void onEndWatch(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        if (isChunkManagedByVanilla(player, chunkPos)) {
            return;
        }
        
        //do not send unload packet instantly
        //watch for a period of time.
        //if player still needs the chunk, stop unloading.
    
        sendUnloadPacket(player, chunkPos);
    }
    
    public void sendUnloadPacket(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        if (isChunkManagedByVanilla(player, chunkPos)) {
            //give up unloading
            return;
        }
    
        if (SGlobal.chunkTracker.isPlayerWatchingChunk(player, chunkPos)) {
            //give up unloading
            return;
        }
        
        player.connection.sendPacket(
            MyNetwork.createRedirectedMessage(
                chunkPos.dimension,
                new SUnloadChunkPacket(
                    chunkPos.x, chunkPos.z
                )
            )
        );
    }
    
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        SGlobal.chunkTracker.onPlayerRespawn(oldPlayer);
        
        Helper.getServer().getWorlds()
            .forEach(world -> {
                ServerChunkProvider chunkManager = (ServerChunkProvider) world.getChunkProvider();
                IEThreadedAnvilChunkStorage storage =
                    (IEThreadedAnvilChunkStorage) chunkManager.chunkManager;
                storage.onPlayerRespawn(oldPlayer);
            });
    }
    
    public static boolean isChunkManagedByVanilla(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos
    ) {
        if (player.dimension != chunkPos.dimension) {
            return false;
        }
        
        int watchDistance = ChunkTracker.getRenderDistanceOnServer();
        
        //NOTE do not use entity.chunkX
        //it's not updated
    
        ChunkPos playerChunkPos = new ChunkPos(player.getPosition());
        
        int chebyshevDistance = Math.max(
            Math.abs(playerChunkPos.x - chunkPos.x),
            Math.abs(playerChunkPos.z - chunkPos.z)
        );
        
        return chebyshevDistance <= watchDistance;
    }
}
