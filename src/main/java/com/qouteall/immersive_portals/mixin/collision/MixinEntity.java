package com.qouteall.immersive_portals.mixin.collision;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.teleportation.CollisionHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
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
    
    private Entity collidingPortal;
    private long collidingPortalActiveTickTime;
    
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
    
    @Shadow
    public abstract double getPosX();
    
    @Shadow
    public abstract double getPosY();
    
    @Shadow
    public abstract double getPosZ();
    
    @Shadow
    protected abstract BlockPos getOnPosition();
    
    @Shadow
    public boolean preventEntitySpawning;
    
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
            getCollidingPortal(),
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

//    @Inject(
//        method = "setOnFireFor",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private void onSetOnFireFor(int int_1, CallbackInfo ci) {
//        if (CollisionHelper.isNearbyPortal((Entity) (Object) this)) {
//            ci.cancel();
//        }
//    }
//
//    @Redirect(
//        method = "move",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/entity/Entity;isWet()Z"
//        )
//    )
//    private boolean redirectIsWet(Entity entity) {
//        if (collidingPortal != null) {
//            return true;
//        }
//        return entity.isWet();
//    }
    
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
    
    @Override
    public Portal getCollidingPortal() {
        return ((Portal) collidingPortal);
    }
    
    @Override
    public void tickCollidingPortal(float tickDelta) {
        Entity this_ = (Entity) (Object) this;
        
        if (collidingPortal != null) {
            if (collidingPortal.world != world) {
                collidingPortal = null;
            }
    
            if (Math.abs(world.getGameTime() - collidingPortalActiveTickTime) >= 3) {
                collidingPortal = null;
            }
        }
        
        if (world.isRemote) {
            McHelper.onClientEntityTick(this_);
        }
    }
    
    @Override
    public void notifyCollidingWithPortal(Entity portal) {
        collidingPortal = portal;
        collidingPortalActiveTickTime = world.getGameTime();
    }
}
