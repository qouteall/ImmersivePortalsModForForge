package com.qouteall.immersive_portals.ducks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import java.io.File;

public interface IEThreadedAnvilChunkStorage {
    int getWatchDistance();
    
    ServerWorld getWorld();
    
    ServerWorldLightManager getLightingProvider();
    
    ChunkHolder getChunkHolder_(long long_1);
    
    void onPlayerRespawn(ServerPlayerEntity oldPlayer);
    
    void updateEntityTrackersAfterSendingChunkPacket(
        Chunk chunk,
        ServerPlayerEntity playerEntity
    );
    
    void resendSpawnPacketToTrackers(Entity entity);
    
    File portal_getSaveDir();
    
    boolean portal_isChunkGenerated(ChunkPos chunkPos);
}
