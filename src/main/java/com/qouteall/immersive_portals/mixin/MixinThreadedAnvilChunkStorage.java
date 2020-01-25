package com.qouteall.immersive_portals.mixin;

import com.mojang.datafixers.util.Either;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.ducks.IEEntityTracker;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.util.concurrent.ITaskExecutor;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkTaskPriorityQueueSorter;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Mixin(value = ChunkManager.class)
public abstract class MixinThreadedAnvilChunkStorage implements IEThreadedAnvilChunkStorage {
    @Shadow
    private int viewDistance;
    
    @Shadow
    @Final
    private ServerWorldLightManager lightManager;
    
    @Shadow
    @Final
    private ServerWorld world;
    
    @Shadow
    protected abstract ChunkHolder func_219219_b(long long_1);
    
    @Shadow
    abstract void setPlayerTracking(
        ServerPlayerEntity serverPlayerEntity_1,
        boolean boolean_1
    );
    
    @Shadow
    @Final
    private Int2ObjectMap entities;
    
    @Shadow
    private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> immutableLoadedChunks;
    
    @Shadow
    @Final
    private AtomicInteger field_219268_v;
    
    @Shadow
    @Final
    private ITaskExecutor<ChunkTaskPriorityQueueSorter.FunctionEntry<Runnable>> field_219265_s;
    
    @Override
    public int getWatchDistance() {
        return viewDistance;
    }
    
    @Override
    public ServerWorld getWorld() {
        return world;
    }
    
    @Override
    public ServerWorldLightManager getLightingProvider() {
        return lightManager;
    }
    
    @Override
    public ChunkHolder getChunkHolder_(long long_1) {
        return func_219219_b(long_1);
    }
    
    /**
     * @author qouteall
     * @reason
     */
    @Overwrite
    public void sendChunkData(
        ServerPlayerEntity player,
        IPacket<?>[] packets_1,
        Chunk worldChunk_1
    ) {
        //chunk data packet will be sent on ChunkDataSyncManager
    }
    
    @Inject(
        method = "untrack",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onUnloadEntity(Entity entity, CallbackInfo ci) {
        //when the player leave this dimension, do not stop tracking entities
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            if (SGlobal.serverTeleportationManager.isTeleporting(player)) {
                entities.remove(entity.getEntityId());
                setPlayerTracking(player, false);
                ci.cancel();
            }
        }
    }
    
    
    //cancel vanilla packet sending
    @Redirect(
        method = "func_219179_a",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;thenAcceptAsync(Ljava/util/function/Consumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
        )
    )
    private CompletableFuture<Void> redirectThenAcceptAsync(
        CompletableFuture completableFuture,
        Consumer<?> action,
        Executor executor
    ) {
        return null;
    }
    
    //do my packet sending
    @Inject(
        method = "func_219179_a",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onCreateTickingFuture(
        ChunkHolder chunkHolder,
        CallbackInfoReturnable<CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>>> cir
    ) {
        CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> future = cir.getReturnValue();
        
        future.thenAcceptAsync((either) -> {
            either.mapLeft((worldChunk) -> {
                this.field_219268_v.getAndIncrement();
                
                SGlobal.chunkDataSyncManager.onChunkProvidedDeferred(worldChunk);
                
                return Either.left(worldChunk);
            });
        }, (runnable) -> {
            this.field_219265_s.enqueue(ChunkTaskPriorityQueueSorter.func_219081_a(
                chunkHolder,
                runnable
            ));
        });
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        entities.values().forEach(obj -> {
            ((IEEntityTracker) obj).onPlayerRespawn(oldPlayer);
        });
    }
    
    @Override
    public int getChunkHolderNum() {
        return immutableLoadedChunks.size();
    }
}
