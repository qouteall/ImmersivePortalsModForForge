package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.hiding_in_the_bushes.alternate_dimension.AlternateDimension;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.alternate_dimension.AlternateSky;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.far_scenery.FarSceneryRenderer;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer implements IEWorldRenderer {
    
    @Shadow
    private ClientWorld world;
    
    @Shadow
    @Final
    private EntityRendererManager renderManager;
    
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
    private ViewFrustum viewFrustum;
    
    @Shadow
    protected abstract void renderBlockLayer(
        RenderType renderLayer_1,
        MatrixStack matrixStack_1,
        double double_1,
        double double_2,
        double double_3
    );
    
    @Shadow
    protected abstract void renderEntity(
        Entity entity_1,
        double double_1,
        double double_2,
        double double_3,
        float float_1,
        MatrixStack matrixStack_1,
        IRenderTypeBuffer vertexConsumerProvider_1
    );
    
    @Mutable
    @Shadow
    @Final
    private ObjectList<?> renderInfos;
    
    @Shadow
    private int renderDistanceChunks;
    
    @Shadow
    private boolean displayListEntitiesDirty;
    
    
    @Shadow
    private VertexBuffer skyVBO;
    
    @Shadow
    @Final
    private VertexFormat skyVertexFormat;
    
    @Shadow
    @Final
    private TextureManager textureManager;
    
    @Shadow
    private VertexBuffer sky2VBO;
    
    @Shadow
    private ChunkRenderDispatcher renderDispatcher;
    
    @Shadow
    protected abstract void updateChunks(long limitTime);
    
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderBlockLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V"
        )
    )
    private void onRenderBeforeRenderLayer(
        WorldRenderer worldRenderer,
        RenderType renderLayer_1,
        MatrixStack matrices,
        double double_1,
        double double_2,
        double double_3
    ) {
        boolean isTranslucent = renderLayer_1 == RenderType.getTranslucent();
        if (isTranslucent) {
            CGlobal.renderer.onBeforeTranslucentRendering(matrices);
            FarSceneryRenderer.onBeforeTranslucentRendering(matrices);
        }
        renderBlockLayer(
            renderLayer_1, matrices,
            double_1, double_2, double_3
        );
        if (isTranslucent) {
            CGlobal.renderer.onAfterTranslucentRendering(matrices);
    
        }
        
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"
        )
    )
    private void redirectClearing(int int_1, boolean boolean_1) {
        if (!CGlobal.renderer.shouldSkipClearing()) {
            RenderSystem.clear(int_1, boolean_1);
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;loadRenderers()V",
        at = @At(
            value = "NEW",
            target = "net/minecraft/client/renderer/ViewFrustum"
        )
    )
    private ViewFrustum redirectConstructingBuildChunkStorage(
        ChunkRenderDispatcher chunkBuilder_1,
        World world_1,
        int int_1,
        WorldRenderer worldRenderer_1
    ) {
        if (CGlobal.useHackedChunkRenderDispatcher) {
            return new MyBuiltChunkStorage(
                chunkBuilder_1, world_1, int_1, worldRenderer_1
            );
        }
        else {
            return new ViewFrustum(
                chunkBuilder_1, world_1, int_1, worldRenderer_1
            );
        }
    }
    
    //apply culling and apply optimization
    @Inject(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;renderBlockLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V",
        at = @At("HEAD")
    )
    private void onStartRenderLayer(
        RenderType renderLayer_1,
        MatrixStack matrixStack_1,
        double double_1,
        double double_2,
        double double_3,
        CallbackInfo ci
    ) {
        ObjectList<?> visibleChunks = this.renderInfos;
        if (renderLayer_1 == RenderType.getSolid()) {
            MyGameRenderer.doPruneVisibleChunks(visibleChunks);
        }
    
        if (CGlobal.renderer.isRendering()) {
            CGlobal.myGameRenderer.updateCullingPlane(matrixStack_1);
            CGlobal.myGameRenderer.startCulling();
            if (MyRenderHelper.isRenderingMirror()) {
                MyRenderHelper.applyMirrorFaceCulling();
            }
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;renderBlockLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V",
        at = @At("TAIL")
    )
    private void onStopRenderLayer(
        RenderType renderLayer_1,
        MatrixStack matrixStack_1,
        double double_1,
        double double_2,
        double double_3,
        CallbackInfo ci
    ) {
        if (CGlobal.renderer.isRendering()) {
            CGlobal.myGameRenderer.endCulling();
            MyRenderHelper.recoverFaceCulling();
        }
    }
    
    //to let the player be rendered when rendering portal
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;isThirdPerson()Z"
        )
    )
    private boolean redirectIsThirdPerson(ActiveRenderInfo camera) {
        if (CGlobal.renderer.shouldRenderPlayerItself()) {
            return true;
        }
        return camera.isThirdPerson();
    }
    
    //render player itself when rendering portal
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderEntity(Lnet/minecraft/entity/Entity;DDDFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;)V"
        )
    )
    private void redirectRenderEntity(
        WorldRenderer worldRenderer,
        Entity entity_1,
        double double_1,
        double double_2,
        double double_3,
        float float_1,
        MatrixStack matrixStack_1,
        IRenderTypeBuffer vertexConsumerProvider_1
    ) {
        ActiveRenderInfo camera = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
        if (entity_1 == camera.getRenderViewEntity()) {
            if (CGlobal.renderer.shouldRenderPlayerItself()) {
                CGlobal.myGameRenderer.renderPlayerItself(() -> {
                    renderEntity(
                        entity_1,
                        double_1, double_2, double_3,
                        float_1,
                        matrixStack_1, vertexConsumerProvider_1
                    );
                });
                return;
            }
        }
        
        renderEntity(
            entity_1,
            double_1, double_2, double_3,
            float_1,
            matrixStack_1, vertexConsumerProvider_1
        );
    
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderRainSnow(Lnet/minecraft/client/renderer/LightTexture;FDDD)V"
        )
    )
    private void beforeRenderingWeather(
        MatrixStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        ActiveRenderInfo camera,
        GameRenderer gameRenderer,
        LightTexture lightmapTextureManager,
        Matrix4f matrix4f,
        CallbackInfo ci
    ) {
        if (CGlobal.renderer.isRendering()) {
            CGlobal.myGameRenderer.updateCullingPlane(matrices);
            CGlobal.myGameRenderer.startCulling();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderRainSnow(Lnet/minecraft/client/renderer/LightTexture;FDDD)V",
            shift = At.Shift.AFTER
        )
    )
    private void afterRenderingWeather(
        MatrixStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        ActiveRenderInfo camera,
        GameRenderer gameRenderer,
        LightTexture lightmapTextureManager,
        Matrix4f matrix4f,
        CallbackInfo ci
    ) {
        if (CGlobal.renderer.isRendering()) {
            CGlobal.myGameRenderer.endCulling();
        }
    }
    
    //avoid render glowing entities when rendering portal
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;isGlowing()Z"
        )
    )
    private boolean redirectGlowing(Entity entity) {
        if (CGlobal.renderer.isRendering()) {
            return false;
        }
        return entity.isGlowing();
    }
    
    private static boolean isReloadingOtherWorldRenderers = false;
    
    //reload other world renderers when the main world renderer is reloaded
    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;loadRenderers()V", at = @At("TAIL"))
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
    
    //avoid translucent sort while rendering portal
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;renderBlockLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderType;getTranslucent()Lnet/minecraft/client/renderer/RenderType;"
        )
    )
    private RenderType redirectGetTranslucent() {
        if (CGlobal.renderer.isRendering()) {
            return null;
        }
        return RenderType.getTranslucent();
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;renderSky(Lcom/mojang/blaze3d/matrix/MatrixStack;F)V", at = @At("HEAD"))
    private void onRenderSkyBegin(MatrixStack matrixStack_1, float float_1, CallbackInfo ci) {
        if (CGlobal.renderer.isRendering()) {
            //reset gl states
            RenderType.getBlockRenderTypes().get(0).setupRenderState();
            RenderType.getBlockRenderTypes().get(0).clearRenderState();
        }
        
        if (MyRenderHelper.isRenderingMirror()) {
            MyRenderHelper.applyMirrorFaceCulling();
        }
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;renderSky(Lcom/mojang/blaze3d/matrix/MatrixStack;F)V", at = @At("RETURN"))
    private void onRenderSkyEnd(MatrixStack matrixStack_1, float float_1, CallbackInfo ci) {
        
        if (mc.world.dimension instanceof AlternateDimension) {
            AlternateSky.renderAlternateSky(matrixStack_1, float_1);
        }
        
        MyRenderHelper.recoverFaceCulling();
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/Matrix4f;)V", at = @At("HEAD"))
    private void onBeforeRender(
        MatrixStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        ActiveRenderInfo camera,
        GameRenderer gameRenderer,
        LightTexture lightmapTextureManager,
        Matrix4f matrix4f,
        CallbackInfo ci
    ) {
        MyRenderHelper.setupTransformationForMirror(camera, matrices);
    }
    
    @Override
    public EntityRendererManager getEntityRenderDispatcher() {
        return renderManager;
    }
    
    @Override
    public ViewFrustum getBuiltChunkStorage() {
        return viewFrustum;
    }
    
    @Override
    public ObjectList getVisibleChunks() {
        return renderInfos;
    }
    
    @Override
    public void setVisibleChunks(ObjectList l) {
        renderInfos = l;
    }
    
    @Override
    public ChunkRenderDispatcher getChunkBuilder() {
        return renderDispatcher;
    }
    
    //update builtChunkStorage every frame
    //update terrain when rendering portal
    @Inject(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;setupTerrain(Lnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/culling/ClippingHelperImpl;ZIZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;setRenderPosition(Lnet/minecraft/util/math/Vec3d;)V"
        )
    )
    private void onBeforeChunkBuilderSetCameraPosition(
        ActiveRenderInfo camera_1,
        ClippingHelperImpl frustum_1,
        boolean boolean_1,
        int int_1,
        boolean boolean_2,
        CallbackInfo ci
    ) {
        if (CGlobal.useHackedChunkRenderDispatcher) {
            this.viewFrustum.updateChunkPositions(
                this.mc.player.getPosX(),
                this.mc.player.getPosZ()
            );
        }
        
        if (CGlobal.renderer.isRendering()) {
            displayListEntitiesDirty = true;
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;updateChunks(J)V"
        )
    )
    private void redirectUpdateChunks(WorldRenderer worldRenderer, long limitTime) {
        if (CGlobal.renderer.isRendering()) {
            updateChunks(0);
        }
        else {
            updateChunks(limitTime);
        }
    }
    
    //rebuild less chunk in render thread while rendering portal to reduce lag spike
    //minecraft has two places rebuilding chunks in render thread
    //one in updateChunks() one in setupTerrain()
    @ModifyConstant(
        method = "setupTerrain",
        constant = @Constant(doubleValue = 768.0D)
    )
    private double modifyRebuildRange(double original) {
        if (CGlobal.renderer.isRendering()) {
            return 256.0;
        }
        else {
            return original;
        }
    }
    
    
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/renderer/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/IRenderTypeBuffer$Impl;finish(Lnet/minecraft/client/renderer/RenderType;)V"
        )
    )
    private void redirectVertexDraw(IRenderTypeBuffer.Impl immediate, RenderType layer) {
        MyRenderHelper.shouldForceDisableCull = MyRenderHelper.isRenderingMirror();
        immediate.finish(layer);
        MyRenderHelper.shouldForceDisableCull = false;
    }
}
