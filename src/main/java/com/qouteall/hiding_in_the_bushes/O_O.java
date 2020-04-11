package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.common.DimensionManager;

import java.util.Iterator;
import java.util.List;

public class O_O {
    public static boolean isForge() {
        return true;
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerChangeDimensionClient(
        DimensionType from, DimensionType to
    ) {
        updateForgeModelData();
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void segregateClientEntity(
        ClientWorld fromWorld,
        Entity entity
    ) {
        ((IEClientWorld_MA) fromWorld).removeEntityWhilstMaintainingCapability(entity);
        entity.removed = false;
    }
    
    public static void segregateServerEntity(
        ServerWorld fromWorld,
        Entity entity
    ) {
        fromWorld.removeEntity(entity, true);
        entity.revive();
    }
    
    public static void segregateServerPlayer(
        ServerWorld fromWorld,
        ServerPlayerEntity player
    ) {
        fromWorld.removePlayer(player, true);
        player.revive();
    }
    
    public static void onPlayerTravelOnServer(
        ServerPlayerEntity player,
        DimensionType from,
        DimensionType to
    ) {
        Global.serverTeleportationManager.isFiringMyChangeDimensionEvent = true;
        net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerChangedDimensionEvent(
            player, from, to
        );
        Global.serverTeleportationManager.isFiringMyChangeDimensionEvent = false;
    }
    
    public static void loadConfigFabric() {
        //nothing
    }
    
    public static boolean isObsidian(IWorld world, BlockPos obsidianPos) {
        return world.getBlockState(obsidianPos).isPortalFrame(world, obsidianPos);
    }
    
    public static final boolean isReachEntityAttributesPresent = false;
    
    public static void registerDimensionsForge() {
        try {
            DimensionManager.fireRegister();
        }
        catch (Throwable e) {
            Helper.err("Exception When Registering Dimensions " + e);
        }
        
    }
    
    public static boolean detectOptiFine() {
        return MyMixinConnector.getIsOptifinePresent();
    }
    
    public static void postChunkUnloadEventForge(Chunk chunk) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
            new net.minecraftforge.event.world.ChunkEvent.Unload(chunk)
        );
    }
    
    public static void postChunkLoadEventForge(Chunk chunk) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
            new net.minecraftforge.event.world.ChunkEvent.Load(chunk)
        );
    }
    
    private static void updateForgeModelData() {
        
        ClientWorld world = Minecraft.getInstance().world;
        
        MyClientChunkManager chunkProvider = (MyClientChunkManager) world.getChunkProvider();
        
        List<Chunk> chunkList = chunkProvider.getCopiedChunkList();
        Iterator<Chunk> chunkIterator = chunkList.iterator();
        int batchSize = chunkList.size() / 10;
        
        ModMain.clientTaskList.addTask(() -> {
            //if the player teleports, stop task
            if (Minecraft.getInstance().world != world) {
                return true;
            }
            
            for (int i = 0; i < batchSize; i++) {
                if (chunkIterator.hasNext()) {
                    Chunk chunk = chunkIterator.next();
                    chunk.getTileEntityMap().values().forEach(tileEntity -> {
                        ModelDataManager.requestModelDataRefresh(tileEntity);
                    });
                }
                else {
                    return true;
                }
            }
            return false;
        });
    }
    
    public static boolean isNetherHigherModPresent() {
        return false;
    }
}
