package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ActiveRenderInfo.class)
public abstract class MixinActiveRenderInfo implements IECamera {
    double lastClipSpaceResult;
    
    @Shadow
    private net.minecraft.util.math.Vec3d pos;
    @Shadow
    private IBlockReader world;
    @Shadow
    private Entity renderViewEntity;
    @Shadow
    private net.minecraft.util.math.Vec3d look;
    
    @Shadow
    protected abstract void setPosition(double x, double y, double z);
    
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
    public void setPos_(Vec3d pos) {
        setPostion(pos);
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
    
    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdated(
        IBlockReader p_216772_1_,
        Entity p_216772_2_,
        boolean p_216772_3_,
        boolean p_216772_4_,
        float p_216772_5_,
        CallbackInfo ci
    ) {
        MyRenderHelper.setupTransformationForMirror((ActiveRenderInfo) (Object) this);
    }
}
