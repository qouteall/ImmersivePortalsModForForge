package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MixinLivingEntity_C {
    @Shadow
    protected double interpTargetX;
    
    @Shadow
    protected double interpTargetY;
    
    @Shadow
    protected double interpTargetZ;
    
    @Shadow
    protected int newPosRotationIncrements;
    
    //avoid entity position interpolate when crossing portal when not travelling dimension
    @Inject(
        method = "Lnet/minecraft/entity/LivingEntity;setPositionAndRotationDirect(DDDFFIZ)V",
        at = @At("RETURN")
    )
    private void onUpdateTrackedPositionAndAngles(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        int interpolationSteps,
        boolean interpolate,
        CallbackInfo ci
    ) {
        Portal collidingPortal = ((IEEntity) this).getCollidingPortal();
        if (collidingPortal != null) {
            LivingEntity this_ = ((LivingEntity) (Object) this);
            double dx = this_.getPosX() - interpTargetX;
            double dy = this_.getPosY() - interpTargetY;
            double dz = this_.getPosZ() - interpTargetZ;
            if (dx * dx + dy * dy + dz * dz > 4) {
                Vec3d currPos = new Vec3d(interpTargetX, interpTargetY, interpTargetZ);
                McHelper.setPosAndLastTickPos(
                    this_,
                    currPos,
                    currPos.subtract(this_.getMotion())
                );
                McHelper.updateBoundingBox(this_);
            }
        }
    }
}
