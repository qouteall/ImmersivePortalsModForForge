package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.render.FrustumCuller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.util.math.AxisAlignedBB;

@Mixin(ClippingHelperImpl.class)
public class MixinFrustum {
    @Shadow
    private double cameraX;
    @Shadow
    private double cameraY;
    @Shadow
    private double cameraZ;
    
    private FrustumCuller frustumCuller;
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/culling/ClippingHelperImpl;setCameraPosition(DDD)V",
        at = @At("TAIL")
    )
    private void onSetOrigin(double double_1, double double_2, double double_3, CallbackInfo ci) {
        if (frustumCuller == null) {
            frustumCuller = new FrustumCuller();
        }
        frustumCuller.update(cameraX, cameraY, cameraZ);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/culling/ClippingHelperImpl;isBoxInFrustum(DDDDDD)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIntersectionTest(
        double double_1,
        double double_2,
        double double_3,
        double double_4,
        double double_5,
        double double_6,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (CGlobal.doUseAdvancedFrustumCulling) {
            //this allocation should be avoided by jvm
            Supplier<AxisAlignedBB> boxInLocalCoordinateSupplier = () -> new AxisAlignedBB(
                double_1, double_2, double_3,
                double_4, double_5, double_6
            ).offset(
                -cameraX, -cameraY, -cameraZ
            );
    
            if (frustumCuller.canDetermineInvisible(boxInLocalCoordinateSupplier)) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }
    
    
}
