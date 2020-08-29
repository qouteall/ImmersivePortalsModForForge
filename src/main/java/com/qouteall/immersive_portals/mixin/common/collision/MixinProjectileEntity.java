package com.qouteall.immersive_portals.mixin.common.collision;

import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class MixinProjectileEntity extends MixinEntity {
    
    @Shadow
    public abstract void onImpact(RayTraceResult hitResult);
    
    @Inject(method = "Lnet/minecraft/entity/projectile/ProjectileEntity;onImpact(Lnet/minecraft/util/math/RayTraceResult;)V", at = @At(value = "HEAD"), cancellable = true)
    protected void onImpact(RayTraceResult hitResult, CallbackInfo ci) {
        if (hitResult instanceof BlockRayTraceResult) {
            Block hittingBlock = this.world.getBlockState(((BlockRayTraceResult) hitResult).getPos()).getBlock();
            if (hitResult.getType() == RayTraceResult.Type.BLOCK &&
                hittingBlock == PortalPlaceholderBlock.instance
            ) {
                ci.cancel();
            }
        }
    }
    
    
}