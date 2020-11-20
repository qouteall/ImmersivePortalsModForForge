package com.qouteall.immersive_portals.mixin.common.entity_sync;

import com.google.common.collect.Lists;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.ducks.IEEntityTracker;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.network.CommonNetwork;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SMountEntityPacket;
import net.minecraft.network.play.server.SSetPassengersPacket;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChunkManager.class)
public abstract class MixinThreadedAnvilChunkStorage_E implements IEThreadedAnvilChunkStorage {
    
    @Shadow
    @Final
    public Int2ObjectMap<ChunkManager.EntityTracker> entities;
    
    @Shadow
    abstract void setPlayerTracking(ServerPlayerEntity player, boolean added);
    
    @Shadow
    @Final
    private ServerWorld world;
    
    @Inject(
        method = "Lnet/minecraft/world/server/ChunkManager;untrack(Lnet/minecraft/entity/Entity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onUnloadEntity(Entity entity, CallbackInfo ci) {
        //when the player leave this dimension, do not stop tracking entities
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            if (Global.serverTeleportationManager.isTeleporting(player)) {
                Object tracker = entities.remove(entity.getEntityId());
                ((IEEntityTracker) tracker).stopTrackingToAllPlayers_();
                setPlayerTracking(player, false);
                ci.cancel();
            }
        }
    }
    
    // Managed by EntitySync
    @Inject(method = "Lnet/minecraft/world/server/ChunkManager;tickEntityTracker()V", at = @At("HEAD"), cancellable = true)
    private void onTickPlayerMovement(CallbackInfo ci) {
        ci.cancel();
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        entities.values().forEach(obj -> {
            ((IEEntityTracker) obj).onPlayerRespawn(oldPlayer);
        });
    }
    
    /**
     * {@link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)}
     */
    @Override
    public void updateEntityTrackersAfterSendingChunkPacket(
        Chunk chunk, ServerPlayerEntity player
    ) {
        List<Entity> attachedEntityList = Lists.newArrayList();
        List<Entity> passengerList = Lists.newArrayList();
        
        for (Object entityTracker : this.entities.values()) {
            Entity entity = ((IEEntityTracker) entityTracker).getEntity_();
            if (entity != player && entity.chunkCoordX == chunk.getPos().x && entity.chunkCoordZ == chunk.getPos().z) {
                ((IEEntityTracker) entityTracker).updateEntityTrackingStatus(player);
                if (entity instanceof MobEntity && ((MobEntity) entity).getLeashHolder() != null) {
                    attachedEntityList.add(entity);
                }
                
                if (!entity.getPassengers().isEmpty()) {
                    passengerList.add(entity);
                }
            }
        }
        
        CommonNetwork.withForceRedirect(
            world.func_234923_W_(),
            () -> {
                for (Entity entity : attachedEntityList) {
                    player.connection.sendPacket(new SMountEntityPacket(
                        entity, ((MobEntity) entity).getLeashHolder()
                    ));
                }
                
                for (Entity entity : passengerList) {
                    player.connection.sendPacket(new SSetPassengersPacket(entity));
                }
            }
        );
    }
    
    @Override
    public void resendSpawnPacketToTrackers(Entity entity) {
        Object tracker = entities.get(entity.getEntityId());
        ((IEEntityTracker) tracker).resendSpawnPacketToTrackers();
    }
    
    @Override
    public Int2ObjectMap<ChunkManager.EntityTracker> getEntityTrackerMap() {
        return entities;
    }
}
