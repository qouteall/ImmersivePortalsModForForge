package com.qouteall.immersive_portals.mixin.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.render.context_management.CloudContext;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

// Optimize cloud rendering by storing the context and
// avoiding rebuild the cloud mesh every time
@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer_Clouds {
    
    @Shadow
    private int cloudsCheckX;
    
    @Shadow
    private int cloudsCheckY;
    
    @Shadow
    private int cloudsCheckZ;
    
    @Shadow
    @Nullable
    private VertexBuffer cloudsVBO;
    
    @Shadow
    private ClientWorld world;
    
    @Shadow
    private boolean cloudsNeedUpdate;
    
    @Shadow
    private int ticks;
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;renderClouds(Lcom/mojang/blaze3d/matrix/MatrixStack;FDDD)V",
        at = @At("HEAD")
    )
    private void onBeginRenderClouds(MatrixStack matrices, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (RenderStates.getRenderedPortalNum() == 0) {
            return;
        }
        
        if (Global.cloudOptimization) {
            portal_onBeginCloudRendering(tickDelta, cameraX, cameraY, cameraZ);
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;renderClouds(Lcom/mojang/blaze3d/matrix/MatrixStack;FDDD)V",
        at = @At("RETURN")
    )
    private void onEndRenderClouds(MatrixStack matrices, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (RenderStates.getRenderedPortalNum() == 0) {
            return;
        }
        
        if (Global.cloudOptimization) {
            portal_onEndCloudRendering();
        }
    }
    
    private void portal_yieldCloudContext(CloudContext context) {
        Vector3d cloudsColor = this.world.getCloudColor(RenderStates.tickDelta);
        
        context.lastCloudsBlockX = cloudsCheckX;
        context.lastCloudsBlockY = cloudsCheckY;
        context.lastCloudsBlockZ = cloudsCheckZ;
        context.cloudsBuffer = cloudsVBO;
        context.dimension = world.func_234923_W_();
        context.cloudColor = cloudsColor;
        
        cloudsVBO = null;
        cloudsNeedUpdate = true;
    }
    
    private void portal_loadCloudContext(CloudContext context) {
        Validate.isTrue(context.dimension == world.func_234923_W_());
        
        cloudsCheckX = context.lastCloudsBlockX;
        cloudsCheckY = context.lastCloudsBlockY;
        cloudsCheckZ = context.lastCloudsBlockZ;
        cloudsVBO = context.cloudsBuffer;
        
        cloudsNeedUpdate = false;
    }
    
    private void portal_onBeginCloudRendering(
        float tickDelta, double cameraX, double cameraY, double cameraZ
    ) {
        float f = this.world.func_239132_a_().func_239213_a_();
        float g = 12.0F;
        float h = 4.0F;
        double d = 2.0E-4D;
        double e = (double) (((float) this.ticks + tickDelta) * 0.03F);
        double i = (cameraX + e) / 12.0D;
        double j = (double) (f - (float) cameraY + 0.33F);
        double k = cameraZ / 12.0D + 0.33000001311302185D;
        i -= (double) (MathHelper.floor(i / 2048.0D) * 2048);
        k -= (double) (MathHelper.floor(k / 2048.0D) * 2048);
        float l = (float) (i - (double) MathHelper.floor(i));
        float m = (float) (j / 4.0D - (double) MathHelper.floor(j / 4.0D)) * 4.0F;
        float n = (float) (k - (double) MathHelper.floor(k));
        Vector3d cloudsColor = this.world.getCloudColor(tickDelta);
        int kx = (int) Math.floor(i);
        int ky = (int) Math.floor(j / 4.0D);
        int kz = (int) Math.floor(k);
        
        @Nullable CloudContext context = CloudContext.findAndTakeContext(
            kx, ky, kz, world.func_234923_W_(), cloudsColor
        );
        
        if (context != null) {
            portal_loadCloudContext(context);
        }
    }
    
    private void portal_onEndCloudRendering() {
        if (!cloudsNeedUpdate) {
            final CloudContext newContext = new CloudContext();
            portal_yieldCloudContext(newContext);
            
            CloudContext.appendContext(newContext);
        }
    }
}
