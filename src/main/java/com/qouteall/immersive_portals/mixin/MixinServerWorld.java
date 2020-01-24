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
        Class<? extends T> entityClass, AxisAlignedBB aabb, Predicate<? super T> predicate
    ) {
        int i = MathHelper.floor((aabb.minX - 2) / 16.0D);
        int j = MathHelper.ceil((aabb.maxX + 2) / 16.0D);
        int k = MathHelper.floor((aabb.minZ - 2) / 16.0D);
        int l = MathHelper.ceil((aabb.maxZ + 2) / 16.0D);
        List<T> list = Lists.newArrayList();
        AbstractChunkProvider abstractchunkprovider = this.getChunkProvider();
        
        for (int i1 = i; i1 < j; ++i1) {
            for (int j1 = k; j1 < l; ++j1) {
                Chunk chunk = portal_getChunkIfLoaded(i1, j1);
                if (chunk != null) {
                    chunk.getEntitiesOfTypeWithinAABB(entityClass, aabb, list, predicate);
                }
            }
        }
        
        return list;
    }
    
    private Chunk portal_getChunkIfLoaded(
        int x, int z
    ) {
        ChunkManager storage = getChunkProvider().chunkManager;
        IEThreadedAnvilChunkStorage ieStorage = (IEThreadedAnvilChunkStorage) storage;
        ChunkHolder chunkHolder = ieStorage.getChunkHolder_(ChunkPos.asLong(x, z));
        if (chunkHolder != null) {
            Chunk chunk = chunkHolder.func_219298_c();
            if (chunk != null) {
                return chunk;
            }
        }
        return null;
    }
}
