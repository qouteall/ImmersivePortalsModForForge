package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEEntityTracker;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import com.qouteall.immersive_portals.network.CommonNetwork;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
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
     * Replace ThreadedAnvilChunkStorage#tickPlayerMovement()
     */
    private static void tick() {
        MinecraftServer server = McHelper.getServer();
        
        server.getProfiler().startSection("ip_entity_tracking");
        
        List<ServerPlayerEntity> playerList = McHelper.getRawPlayerList();
        
        List<ServerPlayerEntity> dirtyPlayers = new ArrayList<>();
        
        for (ServerPlayerEntity player : playerList) {
            ChunkManager storage =
                ((ServerWorld) player.world).getChunkProvider().chunkManager;
            Int2ObjectMap<ChunkManager.EntityTracker> entityTrackerMap =
                ((IEThreadedAnvilChunkStorage) storage).getEntityTrackerMap();
            
            ChunkManager.EntityTracker playerItselfTracker =
                entityTrackerMap.get(player.getEntityId());
            if (playerItselfTracker != null) {
                if (isDirty(playerItselfTracker)) {
                    dirtyPlayers.add(player);
                }
            }
            else {
//                limitedLogger.err(
//                    "Entity tracker abnormal " + player + player.world.getRegistryKey().getValue()
//                );
            }
        }
        
        server.getWorlds().forEach(world -> {
            ChunkManager storage = world.getChunkProvider().chunkManager;
            Int2ObjectMap<ChunkManager.EntityTracker> entityTrackerMap =
                ((IEThreadedAnvilChunkStorage) storage).getEntityTrackerMap();
            
            CommonNetwork.withForceRedirect(world.func_234923_W_(), () -> {
                for (ChunkManager.EntityTracker tracker : entityTrackerMap.values()) {
                    ((IEEntityTracker) tracker).tickEntry();
                    
                    boolean dirty = isDirty(tracker);
                    List<ServerPlayerEntity> updatedPlayerList = dirty ? playerList : dirtyPlayers;
                    
                    for (ServerPlayerEntity player : updatedPlayerList) {
                        ((IEEntityTracker) tracker).updateEntityTrackingStatus(player);
                    }
                    
                    if (dirty) {
                        markUnDirty(tracker);
                    }
                }
            });
        });
        
        server.getProfiler().endSection();
    }
    
    private static boolean isDirty(ChunkManager.EntityTracker tracker) {
        SectionPos newPos = SectionPos.from(((IEEntityTracker) tracker).getEntity_());
        return !((IEEntityTracker) tracker).getLastCameraPosition().equals(newPos);
    }
    
    private static void markUnDirty(ChunkManager.EntityTracker tracker) {
        SectionPos currPos = SectionPos.from(((IEEntityTracker) tracker).getEntity_());
        ((IEEntityTracker) tracker).setLastCameraPosition(currPos);
    }
    
}
