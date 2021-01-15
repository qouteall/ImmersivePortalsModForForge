package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SChunkDataPacket;
import net.minecraft.network.play.server.SUnloadChunkPacket;
import net.minecraft.network.play.server.SUpdateLightPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.lighting.BlockLightEngine;
import net.minecraft.world.lighting.IWorldLightListener;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorldLightManager;
import java.util.function.Supplier;

//the chunks near player are managed by vanilla
//we only manage the chunks that's seen by portal and not near player
//it is not multi-threaded like vanilla
public class ChunkDataSyncManager {
    
    private static final int unloadWaitingTickTime = 20 * 10;
    
    private static final boolean debugLightStatus = true;
    
    public ChunkDataSyncManager() {
        NewChunkTrackingGraph.beginWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onBeginWatch
        );
        NewChunkTrackingGraph.endWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onEndWatch
        );
    }
    
    /**
     * {@link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)}
     */
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
            Chunk chunk = chunkHolder.getChunkIfComplete();
            if (chunk != null) {
                McHelper.getServer().getProfiler().startSection("ptl_create_chunk_packet");
                
                debugCheckLight(chunk);
                
                player.connection.sendPacket(
                    MyNetwork.createRedirectedMessage(
                        chunkPos.dimension,
                        new SChunkDataPacket(((Chunk) chunk), 65535)
                    )
                );
                
                player.connection.sendPacket(
                    MyNetwork.createRedirectedMessage(
                        chunkPos.dimension,
                        new SUpdateLightPacket(
                            chunkPos.getChunkPos(),
                            ieStorage.getLightingProvider(),
                            true
                        )
                    )
                );
                
                ieStorage.updateEntityTrackersAfterSendingChunkPacket(chunk, player);
                
                McHelper.getServer().getProfiler().endSection();
                
                return;
            }
        }
        //if the chunk is not present then the packet will be sent when chunk is ready
        
        //test
//        Helper.log("chunk not ready" + chunkPos);
    }
    
    private void debugCheckLight(Chunk chunk) {
        if (!debugLightStatus) {
            return;
        }
        
        if (!chunk.hasLight()) {
            Helper.err("Sending light update when the light is not on " + chunk.getPos());
            new Throwable().printStackTrace();
        }
        
        IWorldLightListener chunkLightingView =
            ((ServerWorldLightManager) chunk.getWorld().getLightManager()).getLightEngine(LightType.BLOCK);
        
        NibbleArray lightSection = ((BlockLightEngine) chunkLightingView).getData(
            SectionPos.from(chunk.getPos(), 0)
        );
        
        if (lightSection == null) {
            Helper.err("Missing light " + chunk.getPos());
            new Throwable().printStackTrace();
        }
    }
    
    /**
     * {@link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)}r
     */
    public void onChunkProvidedDeferred(Chunk chunk) {
        RegistryKey<World> dimension = chunk.getWorld().func_234923_W_();
        IEThreadedAnvilChunkStorage ieStorage = McHelper.getIEStorage(dimension);
        
        debugCheckLight(chunk);
        
        //test
//        Helper.log("deferred chunk " + chunk.getPos() + chunk.getWorld());
        
        McHelper.getServer().getProfiler().startSection("ptl_create_chunk_packet");
        
        Supplier<IPacket> chunkDataPacketRedirected = Helper.cached(
            () -> MyNetwork.createRedirectedMessage(
                dimension,
                new SChunkDataPacket(((Chunk) chunk), 65535)
            )
        );
        
        Supplier<IPacket> lightPacketRedirected = Helper.cached(
            () -> MyNetwork.createRedirectedMessage(
                dimension,
                new SUpdateLightPacket(chunk.getPos(), ieStorage.getLightingProvider(), true)
            )
        );
        
        NewChunkTrackingGraph.getPlayersViewingChunk(
            dimension, chunk.getPos().x, chunk.getPos().z
        ).forEach(player -> {
            player.connection.sendPacket(chunkDataPacketRedirected.get());
            
            player.connection.sendPacket(lightPacketRedirected.get());
            
            ieStorage.updateEntityTrackersAfterSendingChunkPacket(chunk, player);
        });
        
        McHelper.getServer().getProfiler().endSection();
    }
    
    private void onEndWatch(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        
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
        McHelper.getServer().getWorlds()
            .forEach(world -> {
                ServerChunkProvider chunkManager = (ServerChunkProvider) world.getChunkProvider();
                IEThreadedAnvilChunkStorage storage =
                    (IEThreadedAnvilChunkStorage) chunkManager.chunkManager;
                storage.onPlayerRespawn(oldPlayer);
            });
        
        NewChunkTrackingGraph.forceRemovePlayer(oldPlayer);
    }
    
}
