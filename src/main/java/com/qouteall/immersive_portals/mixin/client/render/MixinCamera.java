package com.qouteall.immersive_portals.mixin.client.render;

import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.render.CrossPortalEntityRenderer;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderingHierarchy;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ActiveRenderInfo.class)
public abstract class MixinCamera implements IECamera {
    private static double lastClipSpaceResult = 1;
    
    @Shadow
    private Vector3d pos;
    @Shadow
    private IBlockReader world;
    @Shadow
    private Entity renderViewEntity;
    @Shadow
    private float height;
    @Shadow
    private float previousHeight;
    
    @Shadow
    protected abstract void setPostion(Vector3d vec3d_1);
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/ActiveRenderInfo;update(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/entity/Entity;ZZF)V",
        at = @At("RETURN")
    )
    private void onUpdateFinished(
        IBlockReader area, Entity focusedEntity, boolean thirdPerson,
        boolean inverseView, float tickDelta, CallbackInfo ci
    ) {
        RenderingHierarchy.adjustCameraPos((ActiveRenderInfo) (Object) this);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/ActiveRenderInfo;getFluidState()Lnet/minecraft/fluid/FluidState;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getSubmergedFluidState(CallbackInfoReturnable<FluidState> cir) {
        if (PortalRendering.isRendering()) {
            cir.setReturnValue(Fluids.EMPTY.getDefaultState());
            cir.cancel();
        }
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/ActiveRenderInfo;calcCameraDistance(D)D", at = @At("HEAD"), cancellable = true)
    private void onClipToSpaceHead(double double_1, CallbackInfoReturnable<Double> cir) {
        if (PortalRendering.isRendering()) {
            cir.setReturnValue(lastClipSpaceResult);
            cir.cancel();
        }
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/ActiveRenderInfo;calcCameraDistance(D)D", at = @At("RETURN"), cancellable = true)
    private void onClipToSpaceReturn(double double_1, CallbackInfoReturnable<Double> cir) {
        lastClipSpaceResult = cir.getReturnValue();
    }
    
    //to let the player be rendered when rendering portal
    @Inject(method = "Lnet/minecraft/client/renderer/ActiveRenderInfo;isThirdPerson()Z", at = @At("HEAD"), cancellable = true)
    private void onIsThirdPerson(CallbackInfoReturnable<Boolean> cir) {
        if (CrossPortalEntityRenderer.shouldRenderPlayerItself()) {
            cir.setReturnValue(true);
        }
    }
    
    @Override
    public void resetState(Vector3d pos, ClientWorld currWorld) {
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
    
    @Override
    public void portal_setPos(Vector3d pos) {
        setPostion(pos);
    }
    
    @Override
    public void portal_setFocusedEntity(Entity arg) {
        renderViewEntity = arg;
    }
}
