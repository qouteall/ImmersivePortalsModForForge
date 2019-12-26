package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.ChunkTrackingGraph;
import com.qouteall.immersive_portals.chunk_loading.WorldInfoSender;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.my_util.Signal;
import com.qouteall.immersive_portals.network.NetworkMain;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;

public class ModMain {
    //after world ticking
    public static final Signal postClientTickSignal = new Signal();
    public static final Signal postServerTickSignal = new Signal();
    public static final Signal preRenderSignal = new Signal();
    public static final MyTaskList clientTaskList = new MyTaskList();
    public static final MyTaskList serverTaskList = new MyTaskList();
    public static final MyTaskList preRenderTaskList = new MyTaskList();
    
    
    public static void onInitialize() {
    
        NetworkMain.init();
        
        postClientTickSignal.connect(clientTaskList::processTasks);
        postServerTickSignal.connect(serverTaskList::processTasks);
        preRenderSignal.connect(preRenderTaskList::processTasks);
    
        SGlobal.serverTeleportationManager = new ServerTeleportationManager();
        SGlobal.chunkTrackingGraph = new ChunkTrackingGraph();
        SGlobal.chunkDataSyncManager = new ChunkDataSyncManager();
    
        WorldInfoSender.init();
        
    }
    
    private static boolean getIsMixinInClassPath() {
        try {
            Class.forName("org.spongepowered.asm.launch.Phases");
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public static void checkMixinState() {
        if (!SGlobal.isServerMixinApplied) {
            String message =
                "Mixin is NOT loaded. Install Mixin according to mod description." +
                    " https://www.curseforge.com/minecraft/mc-mods/immersive-portals-for-forge";
            
            if (getIsMixinInClassPath()) {
                message = "Mixin is in classpath but mixin is not applied";
            }
            Helper.err(message);
            throw new IllegalStateException(message);
        }
    }
}
