package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ActiveRenderInfo.class)
public abstract class MixinCamera implements IECamera {
    double lastClipSpaceResult;
    
    @Shadow
    private net.minecraft.util.math.Vec3d pos;
    @Shadow
    private IBlockReader world;
    
    @Shadow
    protected abstract void setPostion(Vec3d posIn);
    
    @Inject(
        method = "getFluidState",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getSubmergedFluidState(CallbackInfoReturnable<IFluidState> cir) {
        if (CGlobal.renderer.isRendering()) {
            cir.setReturnValue(Fluids.EMPTY.getDefaultState());
            cir.cancel();
        }
    }
    
    @Override
    public void resetState(Vec3d pos, ClientWorld currWorld) {
        setPostion(pos);
        world = currWorld;
    }
    
    @Inject(
        method = "calcCameraDistance",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCalcCameraDistanceHead(
        double startingDistance,
        CallbackInfoReturnable<Double> cir
    ) {
        if (CGlobal.renderer.isRendering()) {
            cir.setReturnValue(lastClipSpaceResult);
            cir.cancel();
        }
    }
    
    @Inject(
        method = "calcCameraDistance",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onCalcCameraDistanceReturn(
        double startingDistance,
        CallbackInfoReturnable<Double> cir
    ) {
        lastClipSpaceResult = cir.getReturnValue();
    }
    
}
