package com.qouteall.immersive_portals.exposer;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;

public interface IEThreadedAnvilChunkStorage {
    int getWatchDistance();
    
    ServerWorld getWorld();
    
    ServerWorldLightManager getLightingProvider();
    
    ChunkHolder getChunkHolder_(long long_1);
    
    void onPlayerRespawn(ServerPlayerEntity oldPlayer);
}
