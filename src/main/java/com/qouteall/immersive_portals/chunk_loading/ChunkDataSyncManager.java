package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.network.NetworkMain;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SChunkDataPacket;
import net.minecraft.network.play.server.SUnloadChunkPacket;
import net.minecraft.network.play.server.SUpdateLightPacket;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerChunkProvider;
import org.apache.commons.lang3.Validate;

//the chunks near player are managed by vanilla
//we only manage the chunks that's seen by portal and not near player
//it is not multi-threaded like vanilla
public class ChunkDataSyncManager {
    
    private static final int unloadWaitingTickTime = 20 * 10;
    
    public ChunkDataSyncManager() {
        NewChunkTrackingGraph.beginWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onBeginWatch
        );
        NewChunkTrackingGraph.endWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onEndWatch
        );
    }
    
    private void onBeginWatch(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        McHelper.getServer().getProfiler().startSection("begin_watch");
    
        IEThreadedAnvilChunkStorage ieStorage = McHelper.getIEStorage(chunkPos.dimension);
    
        sendChunkDataPacketNow(player, chunkPos, ieStorage);
    
        McHelper.getServer().getProfiler().endSection();
    }
    
    private void sendChunkDataPacketNow(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos,
        IEThreadedAnvilChunkStorage ieStorage
    ) {
        ChunkHolder chunkHolder = ieStorage.getChunkHolder_(chunkPos.getChunkPos().asLong());
        if (chunkHolder != null) {
            Chunk chunk = chunkHolder.func_219298_c();
            if (chunk != null) {
                doSendWatchPackets(
                    player,
                    chunkPos,
                    ieStorage,
                    chunk
                );
            }
        }
        //if the chunk is not present then the packet will be sent when chunk is ready
        
    }
    
    public void onChunkProvidedDeferred(Chunk chunk) {
        DimensionType dimension = chunk.getWorld().dimension.getType();
        NewChunkTrackingGraph.getPlayersViewingChunk(
            dimension,
            chunk.getPos().x,
            chunk.getPos().z
        ).forEach(player -> {
            doSendWatchPackets(
                player,
                new DimensionalChunkPos(
                    dimension,
                    chunk.getPos()
                ),
                McHelper.getIEStorage(dimension),
                chunk
            );
        });
        //TODO avoid recreating packet when sending to multiple players
    }
    
    private void doSendWatchPackets(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos,
        IEThreadedAnvilChunkStorage ieStorage,
        Chunk chunk
    ) {
        Validate.notNull(chunk);
        
        McHelper.getServer().getProfiler().startSection("send_chunk_data");
        
        //debug
        //Helper.log("Send " + chunkPos);
        
        assert chunk != null;
        assert !(chunk instanceof EmptyChunk);
        NetworkMain.sendRedirected(
            player,
            chunkPos.dimension,
            new SChunkDataPacket(
                ((Chunk) chunk),
                65535
            )
        );
        
        NetworkMain.sendRedirected(
            player,
            chunkPos.dimension,
            new SUpdateLightPacket(
                chunkPos.getChunkPos(),
                ieStorage.getLightingProvider()
            )
        );
        
        //update the entity trackers
        ((ChunkManager) ieStorage).updatePlayerPosition(player);
        
        McHelper.getServer().getProfiler().endSection();
    }
    
    private void onEndWatch(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
    
        sendUnloadPacket(player, chunkPos);

//        ModMain.serverTaskList.addTask(()->{
//            if (NewChunkTrackingGraph.isPlayerWatchingChunk(
//                player,
//                chunkPos.dimension,
//                chunkPos.x,
//                chunkPos.z
//            )) {
//                return true;
//            }
//
//            sendUnloadPacket(player, chunkPos);
//            return true;
//        });
    
    
    }
    
    public void sendUnloadPacket(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        //debug
        //Helper.log("Unload " + chunkPos);
    
        NetworkMain.sendRedirected(
            player,
            chunkPos.dimension,
            new SUnloadChunkPacket(
                chunkPos.x, chunkPos.z
            )
        );
    }
    
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        NewChunkTrackingGraph.forceRemovePlayer(oldPlayer);
    
        McHelper.getServer().getWorlds()
            .forEach(world -> {
                ServerChunkProvider chunkManager = (ServerChunkProvider) world.getChunkProvider();
                IEThreadedAnvilChunkStorage storage =
                    (IEThreadedAnvilChunkStorage) chunkManager.chunkManager;
                storage.onPlayerRespawn(oldPlayer);
            });
    }
    
}
