package com.qouteall.immersive_portals;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.alternate_dimension.FormulaGenerator;
import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.chunk_loading.WorldInfoSender;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.my_util.Signal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.teleportation.ServerTeleportationManager;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Dimension;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

public class ModMain {
    public static final Signal postClientTickSignal = new Signal();
    public static final Signal postServerTickSignal = new Signal();
    public static final Signal preRenderSignal = new Signal();
    public static final MyTaskList clientTaskList = new MyTaskList();
    public static final MyTaskList serverTaskList = new MyTaskList();
    public static final MyTaskList preRenderTaskList = new MyTaskList();
    
    public static final RegistryKey<Dimension> alternate1Option = RegistryKey.func_240903_a_(
        Registry.field_239700_af_,
        new ResourceLocation("immersive_portals:alternate1")
    );
    public static final RegistryKey<Dimension> alternate2Option = RegistryKey.func_240903_a_(
        Registry.field_239700_af_,
        new ResourceLocation("immersive_portals:alternate2")
    );
    public static final RegistryKey<Dimension> alternate3Option = RegistryKey.func_240903_a_(
        Registry.field_239700_af_,
        new ResourceLocation("immersive_portals:alternate3")
    );
    public static final RegistryKey<Dimension> alternate4Option = RegistryKey.func_240903_a_(
        Registry.field_239700_af_,
        new ResourceLocation("immersive_portals:alternate4")
    );
    public static final RegistryKey<DimensionType> surfaceType = RegistryKey.func_240903_a_(
        Registry.field_239698_ad_,
        new ResourceLocation("immersive_portals:surface_type")
    );
    
    public static final RegistryKey<World> alternate1 = RegistryKey.func_240903_a_(
        Registry.DIMENSION,
        new ResourceLocation("immersive_portals:alternate1")
    );
    public static final RegistryKey<World> alternate2 = RegistryKey.func_240903_a_(
        Registry.DIMENSION,
        new ResourceLocation("immersive_portals:alternate2")
    );
    public static final RegistryKey<World> alternate3 = RegistryKey.func_240903_a_(
        Registry.DIMENSION,
        new ResourceLocation("immersive_portals:alternate3")
    );
    public static final RegistryKey<World> alternate4 = RegistryKey.func_240903_a_(
        Registry.DIMENSION,
        new ResourceLocation("immersive_portals:alternate4")
    );
    public static final RegistryKey<World> alternate5 = RegistryKey.func_240903_a_(
        Registry.DIMENSION,
        new ResourceLocation("immersive_portals:alternate5")
    );
    
    public static DimensionType surfaceTypeObject;
    
    public static Block portalHelperBlock;
    public static BlockItem portalHelperBlockItem;
    
    public static boolean isAlternateDimension(World world) {
        return world.func_230315_m_() == surfaceTypeObject;
    }
    
    public static void init() {
        Helper.log("Immersive Portals Mod Initializing");
        
        MyNetwork.init();
        
        postClientTickSignal.connect(clientTaskList::processTasks);
        postServerTickSignal.connect(serverTaskList::processTasks);
        preRenderSignal.connect(preRenderTaskList::processTasks);
        
        Global.serverTeleportationManager = new ServerTeleportationManager();
        Global.chunkDataSyncManager = new ChunkDataSyncManager();
        
        NewChunkTrackingGraph.init();
        
        WorldInfoSender.init();
        
        FormulaGenerator.init();
        
        GlobalPortalStorage.init();
    
    }
    
}
