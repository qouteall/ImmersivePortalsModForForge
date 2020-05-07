package com.qouteall.hiding_in_the_bushes.fix_model_data;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ILightReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.IModelData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelDataHacker {
    private static int loggedNum = 0;
    
    public static void log(Runnable runnable) {
        if (loggedNum < 200) {
            loggedNum++;
            runnable.run();
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void updateForgeModelData() {

//        ClientWorld world = Minecraft.getInstance().world;
//
//        MyClientChunkManager chunkProvider = (MyClientChunkManager) world.getChunkProvider();
//
//        List<Chunk> chunkList = chunkProvider.getCopiedChunkList();
//        Iterator<Chunk> chunkIterator = chunkList.iterator();
//        int batchSize = chunkList.size() / 10;
//
//        ModMain.clientTaskList.addTask(() -> {
//            //if the player teleports, stop task
//            if (Minecraft.getInstance().world != world) {
//                return true;
//            }
//
//            for (int i = 0; i < batchSize; i++) {
//                if (chunkIterator.hasNext()) {
//                    Chunk chunk = chunkIterator.next();
//                    chunk.getTileEntityMap().values().forEach(tileEntity -> {
//                        ModelDataManager.requestModelDataRefresh(tileEntity);
//                    });
//                }
//                else {
//                    return true;
//                }
//            }
//            return false;
//        });
    }
    
    @OnlyIn(Dist.CLIENT)
    public static IModelData fetchMissingModelData(
        ILightReader world,
        BlockPos pos
    ) {
        log(() -> {
            if (world instanceof IWorld) {
                DimensionType type = ((IWorld) world).getDimension().getType();
                Helper.err("Fetching Missing Model Data " + type + pos);
            }
            else {
                Helper.err("Fetching Missing Model Data " + world + pos);
            }
        });
        
        TileEntity tileEntity = world.getTileEntity(pos);
        
        if (tileEntity == null) {
            log(() -> {
                Helper.err("Cannot find block entity for model data");
            });
            return net.minecraftforge.client.model.data.EmptyModelData.INSTANCE;
        }
        
        IModelData modelData = tileEntity.getModelData();
        if (modelData == null) {
            log(() -> {
                Helper.err("Block entity has null model data? " + tileEntity);
            });
            return net.minecraftforge.client.model.data.EmptyModelData.INSTANCE;
        }
        
        return modelData;
    }
    
    public static Map<BlockPos, IModelData> getChunkModelData(World world, ChunkPos chunkPos) {
        Map<BlockPos, IModelData> data = new HashMap<>();
        Chunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
        chunk.getTileEntityMap().values().forEach(tileEntity -> {
            IModelData modelData = tileEntity.getModelData();
            if (modelData == null) {
                Helper.err("Null Model Data " +
                    world.dimension.getType() + tileEntity.getPos() + tileEntity);
            }
            else {
                data.put(tileEntity.getPos(), modelData);
            }
        });
        return data;
    }
}
