package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.ducks.IEWorldRendererChunkInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkRender;
import net.minecraft.client.renderer.chunk.IChunkRendererFactory;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = WorldRenderer.class)
public abstract class MixinWorldRenderer implements IEWorldRenderer {
    
    @Shadow
    private ClientWorld world;
    
    @Shadow
    private IChunkRendererFactory renderChunkFactory;
    
    @Shadow
    private ViewFrustum viewFrustum;
    
    @Shadow
    private AbstractChunkRenderContainer renderContainer;
    
    @Shadow
    private List renderInfos;
    
    @Shadow
    @Final
    private EntityRendererManager renderManager;
    
    @Shadow
    private int renderEntitiesStartupCounter;
    
    @Shadow
    @Final
    public Minecraft mc;
    
    @Shadow
    private double prevRenderSortX;
    
    @Shadow
    private double prevRenderSortY;
    
    @Shadow
    private double prevRenderSortZ;
    
    @Shadow
    protected abstract void renderBlockLayer(BlockRenderLayer blockLayerIn);
    
    @Override
    public ViewFrustum getChunkRenderDispatcher() {
        return viewFrustum;
    }
    
    @Override
    public AbstractChunkRenderContainer getChunkRenderList() {
        return renderContainer;
    }
    
    @Override
    public List getChunkInfos() {
        return renderInfos;
    }
    
    @Override
    public void setChunkInfos(List list) {
        renderInfos = list;
    }
    
    @Inject(
        method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;)V",
        at = @At("HEAD")
    )
    private void onStartRenderLayer(BlockRenderLayer blockRenderLayer_1, CallbackInfo ci) {
        if (CGlobal.renderer.isRendering()) {
            CGlobal.myGameRenderer.startCulling();
        }
    }
    
    @Inject(
        method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;)V",
        at = @At("TAIL")
    )
    private void onStopRenderLayer(BlockRenderLayer blockRenderLayer_1, CallbackInfo ci) {
        if (CGlobal.renderer.isRendering()) {
            CGlobal.myGameRenderer.endCulling();
        }
    }
    
    @Inject(
        method = "renderEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;enableLightmap()V",
            shift = At.Shift.AFTER
        )
    )
    private void onEndRenderEntities(
        ActiveRenderInfo camera_1,
        ICamera visibleRegion_1,
        float float_1,
        CallbackInfo ci
    ) {
        CGlobal.myGameRenderer.renderPlayerItselfIfNecessary();
    }
    
    private static boolean isReloadingOtherWorldRenderers = false;
    
    //reload other world renderers when the main world renderer is reloaded
    @Inject(method = "loadRenderers", at = @At("TAIL"))
    private void onReload(CallbackInfo ci) {
        ClientWorldLoader clientWorldLoader = CGlobal.clientWorldLoader;
        WorldRenderer this_ = (WorldRenderer) (Object) this;
        if (isReloadingOtherWorldRenderers) {
            return;
        }
        if (CGlobal.renderer.isRendering()) {
            return;
        }
        if (clientWorldLoader.getIsLoadingFakedWorld()) {
            return;
        }
        if (this_ != Minecraft.getInstance().worldRenderer) {
            return;
        }
        
        isReloadingOtherWorldRenderers = true;
        
        for (WorldRenderer worldRenderer : clientWorldLoader.worldRendererMap.values()) {
            if (worldRenderer != this_) {
                worldRenderer.loadRenderers();
            }
        }
        isReloadingOtherWorldRenderers = false;
    }
    
    //avoid resort transparency when rendering portal
    @Inject(
        method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;Lnet/minecraft/client/renderer/ActiveRenderInfo;)I",
        at = @At("HEAD"),
        cancellable = true
    )
    public void onRenderLayer(
        BlockRenderLayer blockLayerIn,
        ActiveRenderInfo activeRenderInfo,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (blockLayerIn == BlockRenderLayer.TRANSLUCENT && CGlobal.renderer.isRendering()) {
            //run my version and avoid resort transparency
    
            RenderHelper.disableStandardItemLighting();
    
            this.mc.getProfiler().startSection("filterempty");
            int l = 0;
            boolean flag = blockLayerIn == BlockRenderLayer.TRANSLUCENT;
            int i1 = flag ? this.renderInfos.size() - 1 : 0;
            int i = flag ? -1 : this.renderInfos.size();
            int j1 = flag ? -1 : 1;
            
            for (int j = i1; j != i; j += j1) {
                ChunkRender chunkrender = ((IEWorldRendererChunkInfo) this.renderInfos.get(j)).getChunkRenderer();
                if (!chunkrender.getCompiledChunk().isLayerEmpty(blockLayerIn)) {
                    ++l;
                    this.renderContainer.addRenderChunk(chunkrender, blockLayerIn);
                }
            }
            
            if (l == 0) {
                this.mc.getProfiler().endSection();
            }
            else {
                if (CHelper.shouldDisableFog()) {
                    GlStateManager.disableFog();
                }
    
                this.mc.getProfiler().endStartSection(() -> {
                    return "render_" + blockLayerIn;
                });
                this.renderBlockLayer(blockLayerIn);
                this.mc.getProfiler().endSection();
            }
            
            cir.setReturnValue(l);
            cir.cancel();
        }
    }
    
    @Override
    public EntityRendererManager getEntityRenderDispatcher() {
        return renderManager;
    }
}
