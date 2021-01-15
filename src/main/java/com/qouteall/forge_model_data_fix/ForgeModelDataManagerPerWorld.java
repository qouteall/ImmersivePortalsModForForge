package com.qouteall.forge_model_data_fix;

import com.google.common.base.Preconditions;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copies {@link net.minecraftforge.client.model.ModelDataManager}
 */
public class ForgeModelDataManagerPerWorld {
    private WeakReference<World> currentWorld = new WeakReference<>(null);
    
    private final Map<ChunkPos, Set<BlockPos>> needModelDataRefresh = new ConcurrentHashMap<>();
    
    private final Map<ChunkPos, Map<BlockPos, IModelData>> modelDataCache = new ConcurrentHashMap<>();
    
    public ForgeModelDataManagerPerWorld() {
    
    }
    
    private void cleanCaches(World world) {
    
    }
    
    public void requestModelDataRefresh(TileEntity te) {
        Preconditions.checkNotNull(te, "Tile entity must not be null");
        World world = te.getWorld();
        
        cleanCaches(world);
        needModelDataRefresh.computeIfAbsent(new ChunkPos(te.getPos()), $ -> Collections.synchronizedSet(new HashSet<>()))
            .add(te.getPos());
    }
    
    private void refreshModelData(World world, ChunkPos chunk) {
        cleanCaches(world);
        Set<BlockPos> needUpdate = needModelDataRefresh.remove(chunk);
        
        if (needUpdate != null) {
            Map<BlockPos, IModelData> data = modelDataCache.computeIfAbsent(chunk, $ -> new ConcurrentHashMap<>());
            for (BlockPos pos : needUpdate) {
                TileEntity toUpdate = world.getTileEntity(pos);
                if (toUpdate != null && !toUpdate.isRemoved()) {
                    data.put(pos, toUpdate.getModelData());
                }
                else {
                    data.remove(pos);
                }
            }
        }
    }
    
    public void onChunkUnload(Chunk chunk) {
        needModelDataRefresh.remove(chunk);
        modelDataCache.remove(chunk);
    }
    
    @Nullable
    public IModelData getModelData(World world, BlockPos pos) {
        return getModelData(world, new ChunkPos(pos)).get(pos);
    }
    
    public Map<BlockPos, IModelData> getModelData(World world, ChunkPos pos) {
        Preconditions.checkArgument(world.isRemote, "Cannot request model data for server world");
        refreshModelData(world, pos);
        return modelDataCache.getOrDefault(pos, Collections.emptyMap());
    }
}
