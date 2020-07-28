package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEEntityTracker;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import java.util.ArrayList;
import java.util.List;

public class EntitySync {
    private static final LimitedLogger limitedLogger = new LimitedLogger(100);
    
    public static void init() {
        ModMain.postServerTickSignal.connect(EntitySync::tick);
    }
    
    /**
     * Replace {@link ThreadedAnvilChunkStorage#tickPlayerMovement()}
     */
    private static void tick() {
        MinecraftServer server = McHelper.getServer();
        
        server.getProfiler().startSection("ip_entity_tracking");
        
        List<ServerPlayerEntity> playerList = McHelper.getRawPlayerList();
        
        List<ServerPlayerEntity> dirtyPlayers = new ArrayList<>();
        
        for (ServerPlayerEntity player : playerList) {
            ChunkManager storage =
                ((ServerWorld) player.world).getChunkProvider().chunkManager;
            ChunkManager.EntityTracker playerItselfTracker =
                storage.entities.get(player.getEntityId());
            if (playerItselfTracker != null) {
                if (isDirty(playerItselfTracker)) {
                    dirtyPlayers.add(player);
                }
            }
            else {
                limitedLogger.err(
                    "Entity tracker abnormal " + player + player.world.func_234923_W_().func_240901_a_()
                );
            }
        }
        
        server.getWorlds().forEach(world -> {
            ChunkManager storage = world.getChunkProvider().chunkManager;
            
            for (ChunkManager.EntityTracker tracker : storage.entities.values()) {
                ((IEEntityTracker) tracker).tickEntry();
                
                List<ServerPlayerEntity> updatedPlayerList = isDirty(tracker) ?
                    playerList : dirtyPlayers;
                
                for (ServerPlayerEntity player : updatedPlayerList) {
                    ((IEEntityTracker) tracker).updateEntityTrackingStatus(player);
                }
                
                markUnDirty(tracker);
            }
        });
        
        server.getProfiler().endSection();
    }
    
    private static boolean isDirty(ChunkManager.EntityTracker tracker) {
        SectionPos newPos = SectionPos.from(tracker.entity);
        return !tracker.pos.equals(newPos);
    }
    
    private static void markUnDirty(ChunkManager.EntityTracker tracker) {
        tracker.pos = SectionPos.from(tracker.entity);
    }
}
