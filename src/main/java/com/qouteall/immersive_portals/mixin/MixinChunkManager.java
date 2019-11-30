package com.qouteall.immersive_portals.mixin;

import com.google.common.collect.Lists;
import com.qouteall.immersive_portals.NetworkMain;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import com.qouteall.immersive_portals.ducks.IEEntityTracker;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.DebugPacketSender;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SChunkDataPacket;
import net.minecraft.network.play.server.SMountEntityPacket;
import net.minecraft.network.play.server.SSetPassengersPacket;
import net.minecraft.network.play.server.SUpdateLightPacket;
import net.minecraft.world.chunk.Chunk;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

@Mixin(value = ChunkManager.class)
public abstract class MixinChunkManager implements IEThreadedAnvilChunkStorage {
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
     * @reason overwriting is easier than injecting
     */
    @Overwrite
    private void sendChunkData(
        ServerPlayerEntity player,
        IPacket<?>[] packets_1,
        Chunk worldChunk_1
    ) {
        //vanilla will not manage inter dimensional chunk loading
        if (player.dimension != world.dimension.getType()) {
            return;
        }
        
        DimensionalChunkPos chunkPos = new DimensionalChunkPos(
            world.dimension.getType(), worldChunk_1.getPos()
        );
        boolean isChunkDataSent = SGlobal.chunkTrackingGraph.isChunkDataSent(player, chunkPos);
        if (isChunkDataSent) {
            return;
        }
    
        ModMain.serverTaskList.addTask(() -> {
            SGlobal.chunkTrackingGraph.onChunkDataSent(player, chunkPos);
            return true;
        });
        
        if (packets_1[0] == null) {
            packets_1[0] = NetworkMain.getRedirectedPacket(
                world.dimension.getType(),
                new SChunkDataPacket(worldChunk_1, 65535)
            );
            packets_1[1] = NetworkMain.getRedirectedPacket(
                world.dimension.getType(),
                new SUpdateLightPacket(
                    worldChunk_1.getPos(),
                    this.lightManager
                )
            );
        }
    
        player.sendChunkLoad(
            worldChunk_1.getPos(),
            packets_1[0],
            packets_1[1]
        );
    
        DebugPacketSender.sendChuckPos(this.world, worldChunk_1.getPos());
        List<Entity> list_1 = Lists.newArrayList();
        List<Entity> list_2 = Lists.newArrayList();
        ObjectIterator var6 = this.entities.values().iterator();
        
        while (var6.hasNext()) {
            IEEntityTracker threadedAnvilChunkStorage$EntityTracker_1 = (IEEntityTracker) var6.next();
            Entity entity_1 = threadedAnvilChunkStorage$EntityTracker_1.getEntity_();
            if (entity_1 != player && entity_1.chunkCoordX == worldChunk_1.getPos().x && entity_1.chunkCoordZ == worldChunk_1.getPos().z) {
                threadedAnvilChunkStorage$EntityTracker_1.updateCameraPosition_(player);
                if (entity_1 instanceof MobEntity && ((MobEntity) entity_1).getLeashHolder() != null) {
                    list_1.add(entity_1);
                }
        
                if (!entity_1.getPassengers().isEmpty()) {
                    list_2.add(entity_1);
                }
            }
        }
        
        Iterator var9;
        Entity entity_3;
        if (!list_1.isEmpty()) {
            var9 = list_1.iterator();
            
            while (var9.hasNext()) {
                entity_3 = (Entity) var9.next();
                NetworkMain.sendRedirected(
                    player,
                    world.getDimension().getType(),
                    new SMountEntityPacket(
                        entity_3,
                        ((MobEntity) entity_3).getLeashHolder()
                    )
                );
    
            }
        }
        
        if (!list_2.isEmpty()) {
            var9 = list_2.iterator();
            
            while (var9.hasNext()) {
                entity_3 = (Entity) var9.next();
                NetworkMain.sendRedirected(
                    player,
                    world.getDimension().getType(),
                    new SSetPassengersPacket(entity_3)
                );
            }
        }
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
