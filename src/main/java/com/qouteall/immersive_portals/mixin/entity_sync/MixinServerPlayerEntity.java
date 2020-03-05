package com.qouteall.immersive_portals.mixin.entity_sync;

import com.google.common.collect.HashMultimap;
import com.mojang.authlib.GameProfile;
import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.ducks.IEServerPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.SDestroyEntitiesPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
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
    private Vec3d enteredNetherPosition;
    
    private HashMultimap<DimensionType, Entity> myRemovedEntities;
    
    public MixinServerPlayerEntity(
        World world,
        GameProfile profile
    ) {
        super(world, profile);
        throw new IllegalStateException();
    }
    
    @Shadow
    private boolean invulnerableDimensionChange;
    
    @Shadow
    protected abstract void func_213846_b(ServerWorld targetWorld);
    
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
    
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void removeEntity(Entity entity_1) {
        if (entity_1 instanceof PlayerEntity) {
            this.connection.sendPacket(
                MyNetwork.createRedirectedMessage(
                    entity_1.dimension,
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
            myRemovedEntities.put(entity_1.world.dimension.getType(), entity_1);
        }
        
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void addEntity(Entity entity_1) {
        if (myRemovedEntities != null) {
            myRemovedEntities.remove(entity_1.dimension, entity_1);
        }
    }
    
    @Override
    public void setEnteredNetherPos(Vec3d pos) {
        enteredNetherPosition = pos;
    }
    
    @Override
    public void updateDimensionTravelAdvancements(ServerWorld fromWorld) {
        func_213846_b(fromWorld);
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
