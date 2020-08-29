package com.qouteall.immersive_portals.mixin.common.entity_sync;

import com.google.common.collect.HashMultimap;
import com.mojang.authlib.GameProfile;
import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.ducks.IEServerPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.SDestroyEntitiesPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements IEServerPlayerEntity {
    @Shadow
    public ServerPlayNetHandler connection;
    @Shadow
    private Vector3d enteredNetherPosition;
    
    private HashMultimap<RegistryKey<World>, Entity> myRemovedEntities;
    
    @Shadow
    private boolean invulnerableDimensionChange;
    
    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }
    
    @Inject(
        method = "Lnet/minecraft/entity/player/ServerPlayerEntity;sendChunkUnload(Lnet/minecraft/util/math/ChunkPos;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendUnloadChunkPacket(ChunkPos chunkPos_1, CallbackInfo ci) {
        ci.cancel();
    }
    
    @Inject(
        method = "Lnet/minecraft/entity/player/ServerPlayerEntity;tick()V",
        at = @At("TAIL")
    )
    private void onTicking(CallbackInfo ci) {
        if (myRemovedEntities != null) {
            myRemovedEntities.keySet().forEach(dimension -> {
                Set<Entity> list = myRemovedEntities.get(dimension);
                connection.sendPacket(
                    MyNetwork.createRedirectedMessage(
                        dimension,
                        new SDestroyEntitiesPacket(
                            list.stream().mapToInt(
                                Entity::getEntityId
                            ).toArray()
                        )
                    )
                );
            });
            myRemovedEntities = null;
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/entity/player/ServerPlayerEntity;copyFrom(Lnet/minecraft/entity/player/ServerPlayerEntity;Z)V",
        at = @At("RETURN")
    )
    private void onCopyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        HashMultimap<RegistryKey<World>, Entity> oldPlayerRemovedEntities =
            ((MixinServerPlayerEntity) (Object) oldPlayer).myRemovedEntities;
        if (oldPlayerRemovedEntities != null) {
            myRemovedEntities = HashMultimap.create();
            this.myRemovedEntities.putAll(oldPlayerRemovedEntities);
        }
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void removeEntity(Entity entity_1) {
        if (entity_1 instanceof PlayerEntity) {
            this.connection.sendPacket(
                MyNetwork.createRedirectedMessage(
                    entity_1.world.func_234923_W_(),
                    new SDestroyEntitiesPacket(entity_1.getEntityId())
                )
            );
        }
        else {
            if (myRemovedEntities == null) {
                myRemovedEntities = HashMultimap.create();
            }
            //do not use entity.dimension
            //or it will work abnormally when changeDimension() is run
            myRemovedEntities.put(entity_1.world.func_234923_W_(), entity_1);
        }
        
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void addEntity(Entity entity_1) {
        if (myRemovedEntities != null) {
            myRemovedEntities.remove(entity_1.world.func_234923_W_(), entity_1);
        }
    }
    
    @Override
    public void setEnteredNetherPos(Vector3d pos) {
        enteredNetherPosition = pos;
    }
    
    @Override
    public void updateDimensionTravelAdvancements(ServerWorld fromWorld) {
    
    }
    
    @Override
    public void setIsInTeleportationState(boolean arg) {
        invulnerableDimensionChange = arg;
    }
    
    @Override
    public void stopRidingWithoutTeleportRequest() {
        super.stopRiding();
    }
    
    @Override
    public void startRidingWithoutTeleportRequest(Entity newVehicle) {
        super.startRiding(newVehicle, true);
    }
}
