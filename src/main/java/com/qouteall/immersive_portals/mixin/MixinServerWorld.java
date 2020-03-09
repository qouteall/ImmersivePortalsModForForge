package com.qouteall.immersive_portals.mixin;

import com.google.common.collect.Lists;
import com.qouteall.immersive_portals.ducks.IEServerWorld;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ForcedChunksSaveData;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.function.Predicate;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld implements IEServerWorld {
    
    @Shadow
    public abstract DimensionSavedDataManager getSavedData();
    
    @Shadow
    public abstract ServerChunkProvider getChunkProvider();
    
    @Override
    public void setChunkForcedWithoutImmediateLoading(int x, int z, boolean forced) {
        ForcedChunksSaveData forcedChunkState = (ForcedChunksSaveData) this.getSavedData().getOrCreate(
            ForcedChunksSaveData::new,
            "chunks"
        );
        ChunkPos chunkPos = new ChunkPos(x, z);
        long l = chunkPos.asLong();
        boolean bl2;
        if (forced) {
            bl2 = forcedChunkState.getChunks().add(l);
        }
        else {
            bl2 = forcedChunkState.getChunks().remove(l);
        }
        
        forcedChunkState.setDirty(bl2);
        if (bl2) {
            this.getChunkProvider().forceChunk(chunkPos, forced);
        }
        
        
    }
    
    @Override
    public <T extends Entity> List<T> getEntitiesWithoutImmediateChunkLoading(
        Class<? extends T> entityClass, AxisAlignedBB box, Predicate<? super T> predicate
    ) {
        int i = MathHelper.floor((box.minX - 2.0D) / 16.0D);
        int j = MathHelper.ceil((box.maxX + 2.0D) / 16.0D);
        int k = MathHelper.floor((box.minZ - 2.0D) / 16.0D);
        int l = MathHelper.ceil((box.maxZ + 2.0D) / 16.0D);
        List<T> list = Lists.newArrayList();
        AbstractChunkProvider chunkManager = this.getChunkProvider();
        
        for (int m = i; m < j; ++m) {
            for (int n = k; n < l; ++n) {
                Chunk worldChunk = (Chunk) portal_getChunkIfLoaded(m, n);
                if (worldChunk != null) {
                    worldChunk.getEntitiesOfTypeWithinAABB((Class) entityClass, box, list, predicate);
                }
            }
        }
        
        return list;
    }
    
    private IChunk portal_getChunkIfLoaded(
        int x, int z
    ) {
        ChunkManager storage = getChunkProvider().chunkManager;
        IEThreadedAnvilChunkStorage ieStorage = (IEThreadedAnvilChunkStorage) storage;
        ChunkHolder chunkHolder = ieStorage.getChunkHolder_(ChunkPos.asLong(x, z));
        if (chunkHolder != null) {
            Chunk chunk = chunkHolder.getChunkIfComplete();
            if (chunk != null) {
                return chunk;
            }
        }
        return null;
    }
}
