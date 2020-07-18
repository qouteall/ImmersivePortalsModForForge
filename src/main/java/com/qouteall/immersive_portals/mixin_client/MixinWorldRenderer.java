package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.render.CrossPortalEntityRenderer;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.PixelCuller;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.TransformationManager;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
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

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Set;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer implements IEWorldRenderer {
    
    @Shadow
    private ClientWorld world;
    
    @Shadow
    @Final
    private EntityRendererManager renderManager;
    
    @Shadow
    @Final
    private Minecraft mc;
    
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
    
    @Shadow
    private Set<ChunkRenderDispatcher.ChunkRender> chunksToUpdate;
    
    @Shadow
    private ShaderGroup field_239227_K_;
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/IRenderTypeBuffer$Impl;finish()V",
            ordinal = 1,
            shift = At.Shift.AFTER
        )
    )
    private void onBeforeTranslucentRendering(
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
        CGlobal.renderer.onBeforeTranslucentRendering(matrices);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
        at = @At("RETURN")
    )
    private void onAfterTranslucentRendering(
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
        CrossPortalEntityRenderer.onEndRenderingEntities(matrices);
        CGlobal.renderer.onAfterTranslucentRendering(matrices);
    
        //make hand rendering normal
        RenderHelper.setupLevelDiffuseLighting(matrices.getLast().getMatrix());
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderBlockLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V"
        )
    )
    private void redirectRenderLayer(
        WorldRenderer worldRenderer,
        RenderType renderLayer,
        MatrixStack matrices,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        ObjectList<?> visibleChunks = this.renderInfos;
        if (renderLayer == RenderType.getSolid()) {
            MyGameRenderer.doPruneVisibleChunks(visibleChunks);
        }
        
        if (PortalRendering.isRendering()) {
            PixelCuller.updateCullingPlaneInner(
                matrices,
                PortalRendering.getRenderingPortal(),
                true
            );
            PixelCuller.startCulling();
            if (PortalRendering.isRenderingOddNumberOfMirrors()) {
                MyRenderHelper.applyMirrorFaceCulling();
            }
        }
        
        renderBlockLayer(
            renderLayer, matrices,
            cameraX, cameraY, cameraZ
        );
        
        if (PortalRendering.isRendering()) {
            PixelCuller.endCulling();
            MyRenderHelper.recoverFaceCulling();
        }
        
        if (renderLayer == RenderType.getCutout()) {
            CrossPortalEntityRenderer.onBeginRenderingEnties(matrices);
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
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
    
    //to let the player be rendered when rendering portal
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;isThirdPerson()Z"
        )
    )
    private boolean redirectIsThirdPerson(ActiveRenderInfo camera) {
        if (CrossPortalEntityRenderer.shouldRenderPlayerItself()) {
            return true;
        }
        return camera.isThirdPerson();
    }
    
    //render player itself when rendering portal
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderEntity(Lnet/minecraft/entity/Entity;DDDFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;)V"
        )
    )
    private void redirectRenderEntity(
        WorldRenderer worldRenderer,
        Entity entity,
        double cameraX,
        double cameraY,
        double cameraZ,
        float tickDelta,
        MatrixStack matrixStack,
        IRenderTypeBuffer vertexConsumerProvider
    ) {
        ActiveRenderInfo camera = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
        if (entity == camera.getRenderViewEntity()) {
            if (CrossPortalEntityRenderer.shouldRenderPlayerItself()) {
                MyGameRenderer.renderPlayerItself(() -> {
                    double distanceToCamera =
                        entity.getEyePosition(RenderStates.tickDelta)
                            .distanceTo(mc.gameRenderer.getActiveRenderInfo().getProjectedView());
                    //avoid rendering player too near and block view except mirror
                    if (distanceToCamera > 1 || PortalRendering.isRenderingOddNumberOfMirrors()) {
                        CrossPortalEntityRenderer.beforeRenderingEntity(entity, matrixStack);
                        renderEntity(
                            entity,
                            cameraX, cameraY, cameraZ,
                            tickDelta,
                            matrixStack, vertexConsumerProvider
                        );
                        CrossPortalEntityRenderer.afterRenderingEntity(entity);
                    }
                    else {
//                        Helper.log("dis");
                    }
                });
                return;
            }
        }
        
        CrossPortalEntityRenderer.beforeRenderingEntity(entity, matrixStack);
        renderEntity(
            entity,
            cameraX, cameraY, cameraZ,
            tickDelta,
            matrixStack, vertexConsumerProvider
        );
        CrossPortalEntityRenderer.afterRenderingEntity(entity);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
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
        if (PortalRendering.isRendering()) {
            PixelCuller.updateCullingPlaneInner(
                matrices,
                PortalRendering.getRenderingPortal(),
                true
            );
            PixelCuller.startCulling();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
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
        if (PortalRendering.isRendering()) {
            PixelCuller.endCulling();
        }
    }
    
    //avoid render glowing entities when rendering portal
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;func_238206_b_(Lnet/minecraft/entity/Entity;)Z"
        )
    )
    private boolean redirectGlowing(Minecraft client, Entity entity) {
        if (PortalRendering.isRendering()) {
            return false;
        }
        return client.func_238206_b_(entity);
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
        if (PortalRendering.isRendering()) {
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
            target = "Lnet/minecraft/client/renderer/RenderType;getTranslucent()Lnet/minecraft/client/renderer/RenderType;",
            ordinal = 0
        )
    )
    private RenderType redirectGetTranslucent() {
        if (PortalRendering.isRendering()) {
            return null;
        }
        return RenderType.getTranslucent();
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;renderSky(Lcom/mojang/blaze3d/matrix/MatrixStack;F)V", at = @At("HEAD"))
    private void onRenderSkyBegin(MatrixStack matrixStack_1, float float_1, CallbackInfo ci) {
        if (PortalRendering.isRendering()) {
            //reset gl states
            RenderType.getBlockRenderTypes().get(0).setupRenderState();
            RenderType.getBlockRenderTypes().get(0).clearRenderState();
            
            //fix sky abnormal with optifine and fog disabled
            if (OFInterface.isFogDisabled.getAsBoolean()) {
                GL11.glEnable(GL11.GL_FOG);
            }
        }
        
        if (PortalRendering.isRenderingOddNumberOfMirrors()) {
            MyRenderHelper.applyMirrorFaceCulling();
        }
    }
    
    //fix sun abnormal with optifine and fog disabled
    @Inject(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;renderSky(Lcom/mojang/blaze3d/matrix/MatrixStack;F)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/render/WorldRenderer;SUN:Lnet/minecraft/util/Identifier;"
        )
    )
    private void onStartRenderingSun(MatrixStack matrixStack, float f, CallbackInfo ci) {
        if (OFInterface.isFogDisabled.getAsBoolean()) {
            GL11.glDisable(GL11.GL_FOG);
        }
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;renderSky(Lcom/mojang/blaze3d/matrix/MatrixStack;F)V", at = @At("RETURN"))
    private void onRenderSkyEnd(MatrixStack matrixStack_1, float float_1, CallbackInfo ci) {

//        if (client.world.dimension instanceof AlternateDimension) {
//            AlternateSkyRenderer.renderAlternateSky(matrixStack_1, float_1);
//        }
        
        if (PortalRendering.isRendering()) {
            //fix sky abnormal with optifine and fog disabled
            GL11.glDisable(GL11.GL_FOG);
            RenderSystem.enableFog();
            RenderSystem.disableFog();
        }
        
        MyRenderHelper.recoverFaceCulling();
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V", at = @At("HEAD"))
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
        TransformationManager.processTransformation(camera, matrices);
    }
    
    //update builtChunkStorage every frame
    //update terrain when rendering portal
    @Inject(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;setupTerrain(Lnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/culling/ClippingHelper;ZIZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;setRenderPosition(Lnet/minecraft/util/math/vector/Vector3d;)V"
        )
    )
    private void onBeforeChunkBuilderSetCameraPosition(
        ActiveRenderInfo camera_1,
        ClippingHelper frustum_1,
        boolean boolean_1,
        int int_1,
        boolean boolean_2,
        CallbackInfo ci
    ) {
        if (CGlobal.useHackedChunkRenderDispatcher) {
            this.viewFrustum.updateChunkPositions(this.mc.player.getPosX(), this.mc.player.getPosZ());
        }
        
        if (PortalRendering.isRendering()) {
            displayListEntitiesDirty = true;
        }
    }
    
    //reduce lag spike
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;updateChunks(J)V"
        )
    )
    private void redirectUpdateChunks(WorldRenderer worldRenderer, long limitTime) {
        if (PortalRendering.isRendering() && (!OFInterface.isOptifinePresent)) {
            portal_updateChunks();
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
        if (PortalRendering.isRendering()) {
            return 256.0;
        }
        else {
            return original;
        }
    }
    
    //disable cull when rendering mirror
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/IRenderTypeBuffer$Impl;finish(Lnet/minecraft/client/renderer/RenderType;)V"
        )
    )
    private void redirectVertexDraw(IRenderTypeBuffer.Impl immediate, RenderType layer) {
        RenderStates.shouldForceDisableCull = PortalRendering.isRenderingOddNumberOfMirrors();
        immediate.finish(layer);
        RenderStates.shouldForceDisableCull = false;
    }
    
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/IRenderTypeBuffer$Impl;finish()V"
        )
    )
    private void redirectVertexDraw1(IRenderTypeBuffer.Impl immediate) {
        RenderStates.shouldForceDisableCull = PortalRendering.isRenderingOddNumberOfMirrors();
        immediate.finish();
        RenderStates.shouldForceDisableCull = false;
    }
    
    //redirect sky rendering dimension
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;updateCameraAndRender(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderSky(Lcom/mojang/blaze3d/matrix/MatrixStack;F)V"
        )
    )
    private void redirectRenderSky(WorldRenderer worldRenderer, MatrixStack matrixStack, float f) {
        if (Global.edgelessSky) {
            if (PortalRendering.isRendering()) {
                if (PortalRendering.getRenderingPortal() instanceof GlobalTrackedPortal) {
                    MyGameRenderer.renderSkyFor(
                        RenderDimensionRedirect.getRedirectedDimension(RenderStates.originalPlayerDimension),
                        matrixStack, f
                    );
                    return;
                }
            }
        }
        
        if (OFInterface.isShaders.getAsBoolean()) {
            RegistryKey<World> dim = Minecraft.getInstance().world.func_234923_W_();
            RegistryKey<World> redirectedDimension = RenderDimensionRedirect.getRedirectedDimension(
                dim);
            
            MyGameRenderer.renderSkyFor(redirectedDimension, matrixStack, f);
            return;
        }
        
        worldRenderer.renderSky(matrixStack, f);
    }
    
    //fix cloud fog abnormal with OptiFine and fog disabled
    @Inject(
        method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;FDDD)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableFog()V",
            shift = At.Shift.AFTER
        )
    )
    private void onEnableFogInRenderClouds(
        MatrixStack matrices,
        float tickDelta,
        double cameraX,
        double cameraY,
        double cameraZ,
        CallbackInfo ci
    ) {
        if (OFInterface.isFogDisabled.getAsBoolean()) {
            MyGameRenderer.forceResetFogState();
            GL11.glEnable(GL11.GL_FOG);
        }
    }
    
    //the camera position is used for translucent sort
    //avoid messing it
    @Redirect(
        method = "Lnet/minecraft/client/renderer/WorldRenderer;setupTerrain(Lnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/culling/ClippingHelper;ZIZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;setRenderPosition(Lnet/minecraft/util/math/vector/Vector3d;)V"
        )
    )
    private void onSetChunkBuilderCameraPosition(ChunkRenderDispatcher chunkBuilder, Vector3d cameraPosition) {
        if (PortalRendering.isRendering()) {
            if (mc.world.func_234923_W_() == RenderStates.originalPlayerDimension) {
                return;
            }
        }
        chunkBuilder.setRenderPosition(cameraPosition);
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
    
    @Override
    public void myRenderEntity(
        Entity entity,
        double cameraX,
        double cameraY,
        double cameraZ,
        float tickDelta,
        MatrixStack matrixStack,
        IRenderTypeBuffer vertexConsumerProvider
    ) {
        renderEntity(
            entity, cameraX, cameraY, cameraZ, tickDelta, matrixStack, vertexConsumerProvider
        );
    }
    
    @Override
    public ShaderGroup portal_getTransparencyShader() {
        return field_239227_K_;
    }
    
    @Override
    public void portal_setTransparencyShader(ShaderGroup arg) {
        field_239227_K_ = arg;
    }
    
    private void portal_updateChunks() {
        
        ChunkRenderDispatcher chunkBuilder = this.renderDispatcher;
        boolean uploaded = chunkBuilder.runChunkUploads();
        this.displayListEntitiesDirty |= uploaded;//no short circuit
        
        int limit = 1;
//        if (CGlobal.renderer.getPortalLayer() > 1) {
//            limit = 1;
//        }
        
        int num = 0;
        for (Iterator<ChunkRenderDispatcher.ChunkRender> iterator = chunksToUpdate.iterator(); iterator.hasNext(); ) {
            ChunkRenderDispatcher.ChunkRender builtChunk = iterator.next();
            
            //vanilla's updateChunks() does not check shouldRebuild()
            //so it may create many rebuild tasks and cancelling it which creates performance cost
            if (builtChunk.shouldStayLoaded()) {
                builtChunk.rebuildChunkLater(chunkBuilder);
                builtChunk.clearNeedsUpdate();
                
                iterator.remove();
                
                num++;
                
                if (num >= limit) {
                    break;
                }
            }
        }
        
    }
    
}
