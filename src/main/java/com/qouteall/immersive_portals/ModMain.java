package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.ChunkTrackingGraph;
import com.qouteall.immersive_portals.chunk_loading.WorldInfoSender;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.my_util.Signal;
import com.qouteall.immersive_portals.network.NetworkMain;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;

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
    
        //to avoid loading some classes too early
        //so strange
        clientTaskList.addTask(() -> {
            if (LateLoadedHelper.dimensionTypeToString == null) {
                LateLoadedHelper.dimensionTypeToString =
                    (obj) -> Registry.DIMENSION_TYPE.getKey((DimensionType) obj).toString();
            }
            return true;
        });
        serverTaskList.addTask(() -> {
            if (LateLoadedHelper.dimensionTypeToString == null) {
                LateLoadedHelper.dimensionTypeToString =
                    (obj) -> Registry.DIMENSION_TYPE.getKey((DimensionType) obj).toString();
            }
            return true;
        });
        
        
    }
    
}
