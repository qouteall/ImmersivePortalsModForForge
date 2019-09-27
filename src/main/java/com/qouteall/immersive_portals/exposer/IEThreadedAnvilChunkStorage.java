package com.qouteall.immersive_portals.exposer;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.chunk.ChunkHolder;
import net.minecraft.world.chunk.ServerWorldLightManager;

public interface IEThreadedAnvilChunkStorage {
    int getWatchDistance();
    
    ServerWorld getWorld();
    
    ServerWorldLightManager getLightingProvider();
    
    ChunkHolder getChunkHolder_(long long_1);
    
    void onPlayerRespawn(ServerPlayerEntity oldPlayer);
}
