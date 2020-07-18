package com.qouteall.immersive_portals.mixin.collision;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.WorldWrappingPortal;
import com.qouteall.immersive_portals.teleportation.CollisionHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity implements IEEntity {
    //world.getEntities is not reliable
    //it has a small chance to ignore collided entities
    //this would cause player to fall through floor when halfway though portal
    //so when player stops colliding a portal, it will not stop colliding instantly
    //it will stop colliding when counter turn to 0
    
    private Portal collidingPortal;
    private int stopCollidingPortalCounter;
    
    @Shadow
    public abstract AxisAlignedBB getBoundingBox();
    
    @Shadow
    public World world;
    
    @Shadow
    public abstract void setBoundingBox(AxisAlignedBB box_1);
    
    @Shadow
    protected abstract Vector3d getAllowedMovement(Vector3d vec3d_1);
    
    @Shadow
    public abstract ITextComponent getName();
    
    @Shadow public abstract double getPosX();
    
    @Shadow public abstract double getPosY();
    
    @Shadow public abstract double getPosZ();
    
    @Shadow protected abstract BlockPos getOnPosition();
    
    @Shadow public boolean preventEntitySpawning;
    
    //maintain collidingPortal field
    @Inject(method = "Lnet/minecraft/entity/Entity;tick()V", at = @At("HEAD"))
    private void onTicking(CallbackInfo ci) {
        tickCollidingPortal(1);
    }
    
    @Redirect(
        method = "Lnet/minecraft/entity/Entity;move(Lnet/minecraft/entity/MoverType;Lnet/minecraft/util/math/vector/Vector3d;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getAllowedMovement(Lnet/minecraft/util/math/vector/Vector3d;)Lnet/minecraft/util/math/vector/Vector3d;"
        )
    )
    private Vector3d redirectHandleCollisions(Entity entity, Vector3d attemptedMove) {
        if (attemptedMove.lengthSquared() > 256) {
            Helper.err("Entity moving too fast " + entity + attemptedMove);
            return Vector3d.ZERO;
        }
        
        if (collidingPortal == null) {
            return getAllowedMovement(attemptedMove);
        }
        
        if (entity.isBeingRidden() || entity.isPassenger()) {
            return getAllowedMovement(attemptedMove);
        }
        
        Vector3d result = CollisionHelper.handleCollisionHalfwayInPortal(
            (Entity) (Object) this,
            attemptedMove,
            collidingPortal,
            attemptedMove1 -> getAllowedMovement(attemptedMove1)
        );
        return result;
    }
    
    //don't burn when jumping into end portal
    //teleportation is instant and accurate in client but not in server
    //so collision may sometimes be incorrect when client teleported but server did not teleport
    @Inject(method = "Lnet/minecraft/entity/Entity;setInLava()V", at = @At("HEAD"), cancellable = true)
    private void onSetInLava(CallbackInfo ci) {
        if (CollisionHelper.isNearbyPortal((Entity) (Object) this)) {
            ci.cancel();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/entity/Entity;func_230279_az_()Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIsFireImmune(CallbackInfoReturnable<Boolean> cir) {
        if (collidingPortal != null) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/entity/Entity;setFire(I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSetOnFireFor(int int_1, CallbackInfo ci) {
        if (CollisionHelper.isNearbyPortal((Entity) (Object) this)) {
            ci.cancel();
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/entity/Entity;move(Lnet/minecraft/entity/MoverType;Lnet/minecraft/util/math/vector/Vector3d;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;isInWaterRainOrBubbleColumn()Z"
        )
    )
    private boolean redirectIsWet(Entity entity) {
        if (collidingPortal != null) {
            return true;
        }
        return entity.isInWaterRainOrBubbleColumn();
    }
    
    @Redirect(
        method = "Lnet/minecraft/entity/Entity;doBlockCollisions()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getBoundingBox()Lnet/minecraft/util/math/AxisAlignedBB;"
        )
    )
    private AxisAlignedBB redirectBoundingBoxInCheckingBlockCollision(Entity entity) {
        return CollisionHelper.getActiveCollisionBox(entity);
    }
    
    @Inject(
        method = "Lnet/minecraft/entity/Entity;read(Lnet/minecraft/nbt/CompoundNBT;)V",
        at = @At("RETURN")
    )
    private void onReadFinished(CompoundNBT compound, CallbackInfo ci) {
//        if (dimension == null) {
//            Helper.err("Invalid Dimension Id Read From NBT " + this);
//            if (world != null) {
//                dimension = world.getRegistryKey();
//            }
//            else {
//                Helper.err("World Field is Null");
//                dimension = DimensionType.OVERWORLD;
//            }
//        }
    }
    
    //for teleportation debug
    @Inject(
        method = "Lnet/minecraft/entity/Entity;setRawPosition(DDD)V",
        at = @At("HEAD")
    )
    private void onSetPos(double nx, double ny, double nz, CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayerEntity) {
            if (Global.teleportationDebugEnabled) {
                if (Math.abs(getPosX() - nx) > 10 ||
                    Math.abs(getPosY() - ny) > 10 ||
                    Math.abs(getPosZ() - nz) > 10
                ) {
                    Helper.log(String.format(
                        "%s %s teleported from %s %s %s to %s %s %s",
                        getName().getUnformattedComponentText(),
                        world.func_234923_W_(),
                        (int) getPosX(), (int) getPosY(), (int) getPosZ(),
                        (int) nx, (int) ny, (int) nz
                    ));
                    new Throwable().printStackTrace();
                }
            }
        }
    }
    
    //avoid suffocation damage when crossing world wrapping portal with barrier
    @Inject(
        method = "Lnet/minecraft/entity/Entity;isEntityInsideOpaqueBlock()Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIsInsideWall(CallbackInfoReturnable<Boolean> cir) {
        if (collidingPortal instanceof WorldWrappingPortal) {
            cir.setReturnValue(false);
        }
    }
    
    @Override
    public Portal getCollidingPortal() {
        return collidingPortal;
    }
    
    @Override
    public void tickCollidingPortal(float tickDelta) {
        Entity this_ = (Entity) (Object) this;
        
        if (collidingPortal != null) {
            if (collidingPortal.world != world) {
                collidingPortal = null;
            }
        }
        
        //TODO change to portals discovering nearby entities instead
        // of entities discovering nearby portals
        world.getProfiler().startSection("getCollidingPortal");
        Portal nowCollidingPortal =
            CollisionHelper.getCollidingPortalUnreliable(this_, tickDelta);
        world.getProfiler().endSection();
        
        if (nowCollidingPortal == null) {
            if (stopCollidingPortalCounter > 0) {
                stopCollidingPortalCounter--;
            }
            else {
                collidingPortal = null;
            }
        }
        else {
            collidingPortal = nowCollidingPortal;
            stopCollidingPortalCounter = 1;
        }
        
        if (world.isRemote) {
            McHelper.onClientEntityTick(this_);
        }
    }
}
