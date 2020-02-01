package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.teleportation.CollisionHelper;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class)
public abstract class MixinEntity implements IEEntity {
    //world.getEntities is not reliable
    //it has a small chance to ignore collided entities
    //this would cause player to fall through floor when halfway though portal
    //so when player stops colliding a portal, it will not stop colliding instantly
    //it will stop colliding when counter turn to 0
    
    private Entity collidingPortal;
    private int stopCollidingPortalCounter;
    
    @Shadow
    public abstract AxisAlignedBB getBoundingBox();
    
    @Shadow
    public World world;
    
    @Shadow
    protected abstract Vec3d getAllowedMovement(Vec3d vec3d_1);
    
    @Shadow
    public abstract void setBoundingBox(AxisAlignedBB box_1);
    
    @Shadow
    protected abstract void dealFireDamage(int int_1);
    
    @Shadow
    public DimensionType dimension;
    
    @Shadow
    public double posX;
    
    @Shadow
    public double posZ;
    
    @Shadow
    public double posY;
    
    @Shadow
    public abstract float getEyeHeight();
    
    //maintain collidingPortal field
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTicking(CallbackInfo ci) {
        if (collidingPortal != null) {
            if ((collidingPortal).dimension != dimension) {
                collidingPortal = null;
            }
        }
        
        Entity nowCollidingPortal =
            (Entity) (Object) CollisionHelper.getCollidingPortalUnreliable((Entity) (Object) this);
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
    }
    
    @Redirect(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getAllowedMovement(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;"
        )
    )
    private Vec3d redirectHandleCollisions(Entity entity, Vec3d attemptedMove) {
        if (attemptedMove.lengthSquared() > 256) {
            Helper.err(entity + " moved too fast " + attemptedMove);
            return attemptedMove;
            //return getAllowedMovement(attemptedMove);
        }
    
        if (collidingPortal == null) {
            return getAllowedMovement(attemptedMove);
        }
    
        if (entity.isBeingRidden() || entity.isPassenger()) {
            return getAllowedMovement(attemptedMove);
        }
        
        Vec3d result = CollisionHelper.handleCollisionHalfwayInPortal1(
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
    @Inject(method = "setInLava", at = @At("HEAD"), cancellable = true)
    private void onSetInLava(CallbackInfo ci) {
        if (CollisionHelper.isNearbyPortal((Entity) (Object) this)) {
            ci.cancel();
        }
    }
    
    //don't burn when jumping into end portal
    @Redirect(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;dealFireDamage(I)V"
        )
    )
    private void redirectBurn(Entity entity, int int_1) {
        if (!CollisionHelper.isNearbyPortal((Entity) (Object) this)) {
            dealFireDamage(int_1);
        }
    }
    
    //don't burn when jumping into end portal
    @Redirect(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;isFlammableWithin(Lnet/minecraft/util/math/AxisAlignedBB;)Z"
        )
    )
    private boolean redirectDoesContainFireSource(World world, AxisAlignedBB box_1) {
        if (collidingPortal == null) {
            return world.isFlammableWithin(box_1);
        }
        else {
            return false;
        }
    }
    
    @Redirect(
        method = "doBlockCollisions",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getBoundingBox()Lnet/minecraft/util/math/AxisAlignedBB;"
        )
    )
    private AxisAlignedBB redirectBoundingBoxInCheckingBlockCollision(Entity entity) {
        return CollisionHelper.getActiveCollisionBox(entity);
    }
    
    @Inject(
        method = "read",
        at = @At("RETURN")
    )
    private void onReadFinished(CompoundNBT compound, CallbackInfo ci) {
        if (dimension == null) {
            Helper.err("Invalid Dimension Id Read From NBT " + this);
            if (world != null) {
                dimension = world.dimension.getType();
            }
            else {
                Helper.err("World Field is Null");
                dimension = DimensionType.OVERWORLD;
            }
        }
    }
    
    @Override
    public Entity getCollidingPortal() {
        return collidingPortal;
    }
}
