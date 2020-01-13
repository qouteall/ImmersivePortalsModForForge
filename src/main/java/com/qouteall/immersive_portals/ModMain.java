package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.ChunkTrackingGraph;
import com.qouteall.immersive_portals.chunk_loading.WorldInfoSender;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.my_util.Signal;
import com.qouteall.immersive_portals.network.NetworkMain;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.NewNetherPortalEntity;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;
import net.minecraft.world.dimension.DimensionType;

public class ModMain {
    //after world ticking
    public static final Signal postClientTickSignal = new Signal();
    public static final Signal postServerTickSignal = new Signal();
    public static final Signal preRenderSignal = new Signal();
    public static final MyTaskList clientTaskList = new MyTaskList();
    public static final MyTaskList serverTaskList = new MyTaskList();
    public static final MyTaskList preRenderTaskList = new MyTaskList();
    
    public static DimensionType alternate;
    
    
    public static void onInitialize() {
        
        NetherPortalEntity.init();
        
        NewNetherPortalEntity.init();
        
        NetworkMain.init();
        
        postClientTickSignal.connect(clientTaskList::processTasks);
        postServerTickSignal.connect(serverTaskList::processTasks);
        preRenderSignal.connect(preRenderTaskList::processTasks);
        
        SGlobal.serverTeleportationManager = new ServerTeleportationManager();
        SGlobal.chunkTrackingGraph = new ChunkTrackingGraph();
        SGlobal.chunkDataSyncManager = new ChunkDataSyncManager();
    
        WorldInfoSender.init();
        
    }
    
    public static void checkMixinState() {
        if (!SGlobal.isServerMixinApplied) {
            String message =
                "Mixin is NOT loaded. Install MixinBootstrap." +
                    " https://www.curseforge.com/minecraft/mc-mods/immersive-portals-for-forge";
    
            try {
                Class.forName("org.spongepowered.asm.launch.Phases");
                Helper.err("What? Mixin is in classpath???");
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
                Helper.err(e);
                Helper.err(e.getCause());
                Helper.err("Mixin is not in classpath");
            }
    
            Helper.err(message);
            throw new IllegalStateException(message);
        }
    }
    
    public static boolean isMixinInClasspath() {
        try {
            Class.forName("org.spongepowered.asm.launch.Phases");
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }
}
