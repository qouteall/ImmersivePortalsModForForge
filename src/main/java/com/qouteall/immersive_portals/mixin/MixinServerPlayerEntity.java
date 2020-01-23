package com.qouteall.immersive_portals.mixin;

import com.google.common.collect.HashMultimap;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEServerPlayerEntity;
import com.qouteall.immersive_portals.network.NetworkMain;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.SDestroyEntitiesPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(value = ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity implements IEServerPlayerEntity {
    @Shadow
    public ServerPlayNetHandler connection;
    @Shadow
    private Vec3d enteredNetherPosition;
    
    private HashMultimap<DimensionType, Entity> myRemovedEntities;
    
    @Shadow
    public abstract void func_213846_b(ServerWorld serverWorld_1);
    
    @Shadow
    private boolean invulnerableDimensionChange;
    
    @Override
    public void setEnteredNetherPos(Vec3d pos) {
        enteredNetherPosition = pos;
    }
    
    @Override
    public void updateDimensionTravelAdvancements(ServerWorld fromWorld) {
        func_213846_b(fromWorld);
    }
    
    @Inject(
        method = "sendChunkUnload",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendUnloadChunkPacket(ChunkPos chunkPos_1, CallbackInfo ci) {
        ci.cancel();
    }
    
    @Inject(
        method = "tick",
        at = @At("TAIL")
    )
    private void onTicking(CallbackInfo ci) {
        if (myRemovedEntities != null) {
            myRemovedEntities.keySet().forEach(dimension -> {
                Set<Entity> list = myRemovedEntities.get(dimension);
                NetworkMain.sendRedirected(
                    connection.player, dimension,
                    new SDestroyEntitiesPacket(
                        list.stream().mapToInt(
                            Entity::getEntityId
                        ).toArray()
                    )
                );
            });
            myRemovedEntities = null;
        }
    }
    
    @Inject(method = "changeDimension", at = @At("HEAD"))
    private void onChangeDimensionByVanilla(
        DimensionType dimensionType_1,
        CallbackInfoReturnable<Entity> cir
    ) {
        SGlobal.chunkDataSyncManager.onPlayerRespawn((ServerPlayerEntity) (Object) this);
    }
    
    /**
     * @author qouteall
     * @reason
     */
    @Overwrite
    public void removeEntity(Entity entity_1) {
        if (entity_1 instanceof PlayerEntity) {
            NetworkMain.sendRedirected(
                connection.player, entity_1.dimension,
                new SDestroyEntitiesPacket(entity_1.getEntityId())
            );
        }
        else {
            if (myRemovedEntities == null) {
                myRemovedEntities = HashMultimap.create();
            }
            //do not use entity.dimension
            //or it will work abnormally when changeDimension() is run
            myRemovedEntities.put(entity_1.world.dimension.getType(), entity_1);
        }
        
    }
    
    /**
     * @author qouteall
     * @reason
     */
    @Overwrite
    public void addEntity(Entity entity_1) {
        if (myRemovedEntities != null) {
            myRemovedEntities.remove(entity_1.dimension, entity_1);
        }
    }
    
    @Inject(
        method = "teleport",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/server/ServerWorld;removePlayer(Lnet/minecraft/entity/player/ServerPlayerEntity;Z)V"
        )
    )
    private void onForgeTeleport(
        ServerWorld p_200619_1_,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        CallbackInfo ci
    ) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
    
        //fix issue with good nights sleep
        player.clearBedPosition();
    
        NewChunkTrackingGraph.forceRemovePlayer(player);
    
        GlobalPortalStorage.onPlayerLoggedIn(player);
    }
    
    @Override
    public void setIsInTeleportationState(boolean arg) {
        invulnerableDimensionChange = arg;
    }
}
