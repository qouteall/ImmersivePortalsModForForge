package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.exposer.IEBackgroundRenderer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class MixinFogRenderer implements IEBackgroundRenderer {
    @Shadow private float red;
    @Shadow private float green;
    @Shadow private float blue;
    
    private DimensionType dimensionConstraint;
    
    @Override
    public Vec3d getFogColor() {
        return new Vec3d(red, green, blue);
    }
    
    @Override
    public void setDimensionConstraint(DimensionType dim) {
        dimensionConstraint = dim;
    }
    
    @Override
    public DimensionType getDimensionConstraint() {
        return dimensionConstraint;
    }
    
    @Inject(
        method = "func_217620_a",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onUpdateColorNotInWater(
        ActiveRenderInfo camera_1,
        World world_1,
        float float_1,
        CallbackInfo ci
    ) {
        if (dimensionConstraint != null) {
            if (world_1.dimension.getType() != dimensionConstraint) {
                ci.cancel();
            }
        }
    }
    
    
}
