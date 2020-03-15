package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ducks.IECamera;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ActiveRenderInfo.class)
public abstract class MixinCamera implements IECamera {
    private double lastClipSpaceResult = 1;
    
    @Shadow
    private net.minecraft.util.math.Vec3d pos;
    @Shadow
    private IBlockReader world;
    @Shadow
    private Entity renderViewEntity;
    @Shadow
    private float height;
    @Shadow
    private float previousHeight;
    
    @Shadow
    protected abstract void setPostion(net.minecraft.util.math.Vec3d vec3d_1);
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/ActiveRenderInfo;getFluidState()Lnet/minecraft/fluid/IFluidState;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getSubmergedFluidState(CallbackInfoReturnable<IFluidState> cir) {
        if (CGlobal.renderer.isRendering()) {
            cir.setReturnValue(Fluids.EMPTY.getDefaultState());
            cir.cancel();
        }
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/ActiveRenderInfo;calcCameraDistance(D)D", at = @At("HEAD"), cancellable = true)
    private void onClipToSpaceHead(double double_1, CallbackInfoReturnable<Double> cir) {
        if (CGlobal.renderer.isRendering()) {
            cir.setReturnValue(lastClipSpaceResult);
            cir.cancel();
        }
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/ActiveRenderInfo;calcCameraDistance(D)D", at = @At("RETURN"), cancellable = true)
    private void onClipToSpaceReturn(double double_1, CallbackInfoReturnable<Double> cir) {
        lastClipSpaceResult = cir.getReturnValue();
    }
    
    @Override
    public void resetState(Vec3d pos, ClientWorld currWorld) {
        setPostion(pos);
        world = currWorld;
    }
    
    @Override
    public float getCameraY() {
        return height;
    }
    
    @Override
    public float getLastCameraY() {
        return previousHeight;
    }
    
    @Override
    public void setCameraY(float cameraY_, float lastCameraY_) {
        height = cameraY_;
        previousHeight = lastCameraY_;
    }
}
