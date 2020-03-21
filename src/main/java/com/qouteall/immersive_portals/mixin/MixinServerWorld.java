package com.qouteall.immersive_portals.mixin;

import com.google.common.collect.Lists;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEServerWorld;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld implements IEServerWorld {
    
    @Shadow
    public abstract DimensionSavedDataManager getSavedData();
    
    @Shadow
    public abstract ServerChunkProvider getChunkProvider();
    
    private static LongSortedSet dummy;
    
    static {
        dummy = new LongLinkedOpenHashSet();
        dummy.add(23333);
    }
    
    //in vanilla if a dimension has no player and no forced chunks then it will not tick
    @Redirect(
        method = "Lnet/minecraft/world/server/ServerWorld;tick(Ljava/util/function/BooleanSupplier;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/server/ServerWorld;getForcedChunks()Lit/unimi/dsi/fastutil/longs/LongSet;"
        )
    )
    private LongSet redirectGetForcedChunks(ServerWorld world) {
        if (NewChunkTrackingGraph.shouldLoadDimension(world.dimension.getType())) {
            return dummy;
        }
        else {
            return world.getForcedChunks();
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
