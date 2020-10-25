package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.SodiumInterface;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.ducks.IEPlayerListEntry;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.ducks.IEWorldRendererChunkInfo;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.DimensionRenderHelper;
import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import com.qouteall.immersive_portals.render.context_management.RenderInfo;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderTypeBuffers;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class MyGameRenderer {
    public static Minecraft client = Minecraft.getInstance();
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(10);
    
    // portal rendering and outer world rendering uses different buffer builder storages
    // theoretically every layer of portal rendering should have its own buffer builder storage
    // but it's more complex
    private static RenderTypeBuffers secondaryBufferBuilderStorage = new RenderTypeBuffers();
    
    public static void renderWorldNew(
        RenderInfo renderInfo,
        Consumer<Runnable> invokeWrapper
    ) {
        RenderInfo.pushRenderInfo(renderInfo);
        
        switchAndRenderTheWorld(
            renderInfo.world,
            renderInfo.cameraPos,
            renderInfo.cameraPos,
            invokeWrapper,
            renderInfo.renderDistance
        );
        
        RenderInfo.popRenderInfo();
    }
    
    private static void switchAndRenderTheWorld(
        ClientWorld newWorld,
        Vector3d thisTickCameraPos,
        Vector3d lastTickCameraPos,
        Consumer<Runnable> invokeWrapper,
        int renderDistance
    ) {
        resetGlStates();
        
        Entity cameraEntity = client.renderViewEntity;
        
        Vector3d oldEyePos = McHelper.getEyePos(cameraEntity);
        Vector3d oldLastTickEyePos = McHelper.getLastTickEyePos(cameraEntity);
        
        RegistryKey<World> oldEntityDimension = cameraEntity.world.func_234923_W_();
        ClientWorld oldEntityWorld = ((ClientWorld) cameraEntity.world);
        
        RegistryKey<World> newDimension = newWorld.func_234923_W_();
        
        //switch the camera entity pos
        McHelper.setEyePos(cameraEntity, thisTickCameraPos, lastTickCameraPos);
        cameraEntity.world = newWorld;
        
        WorldRenderer worldRenderer = CGlobal.clientWorldLoader.getWorldRenderer(newDimension);
        
        CHelper.checkGlError();
        
        float tickDelta = RenderStates.tickDelta;
        
        if (CGlobal.useHackedChunkRenderDispatcher) {
            ((IEWorldRenderer) worldRenderer).getBuiltChunkStorage().updateChunkPositions(
                cameraEntity.getPosX(),
                cameraEntity.getPosZ()
            );
        }
        
        if (Global.looseVisibleChunkIteration) {
            client.renderChunksMany = false;
        }
        
        IEGameRenderer ieGameRenderer = (IEGameRenderer) client.gameRenderer;
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(
                RenderDimensionRedirect.getRedirectedDimension(newDimension)
            );
        NetworkPlayerInfo playerListEntry = CHelper.getClientPlayerListEntry();
        ActiveRenderInfo newCamera = new ActiveRenderInfo();
        
        //store old state
        WorldRenderer oldWorldRenderer = client.worldRenderer;
        LightTexture oldLightmap = client.gameRenderer.getLightTexture();
        GameType oldGameMode = playerListEntry.getGameType();
        boolean oldNoClip = client.player.noClip;
        boolean oldDoRenderHand = ieGameRenderer.getDoRenderHand();
        OFInterface.createNewRenderInfosNormal.accept(worldRenderer);
        ObjectList oldVisibleChunks = ((IEWorldRenderer) oldWorldRenderer).getVisibleChunks();
        RayTraceResult oldCrosshairTarget = client.objectMouseOver;
        ActiveRenderInfo oldCamera = client.gameRenderer.getActiveRenderInfo();
        ShaderGroup oldTransparencyShader =
            ((IEWorldRenderer) oldWorldRenderer).portal_getTransparencyShader();
        ShaderGroup newTransparencyShader = ((IEWorldRenderer) worldRenderer).portal_getTransparencyShader();
        RenderTypeBuffers oldBufferBuilder = ((IEWorldRenderer) worldRenderer).getBufferBuilderStorage();
        RenderTypeBuffers oldClientBufferBuilder = client.getRenderTypeBuffers();
        
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(new ObjectArrayList());
        
        int oldRenderDistance = ((IEWorldRenderer) worldRenderer).portal_getRenderDistance();
        
        //switch
        ((IEMinecraftClient) client).setWorldRenderer(worldRenderer);
        client.world = newWorld;
        ieGameRenderer.setLightmapTextureManager(helper.lightmapTexture);
        
        TileEntityRendererDispatcher.instance.world = newWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(GameType.SPECTATOR);
        client.player.noClip = true;
        ieGameRenderer.setDoRenderHand(false);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        FogRendererContext.swappingManager.pushSwapping(
            RenderDimensionRedirect.getRedirectedDimension(newDimension)
        );
        ((IEParticleManager) client.particles).mySetWorld(newWorld);
        if (BlockManipulationClient.remotePointedDim == newDimension) {
            client.objectMouseOver = BlockManipulationClient.remoteHitResult;
        }
        ieGameRenderer.setCamera(newCamera);
        
        if (Global.useSecondaryEntityVertexConsumer) {
            ((IEWorldRenderer) worldRenderer).setBufferBuilderStorage(secondaryBufferBuilderStorage);
            ((IEMinecraftClient) client).setBufferBuilderStorage(secondaryBufferBuilderStorage);
        }
        
        Object newSodiumContext = SodiumInterface.createNewRenderingContext.apply(worldRenderer);
        Object oldSodiumContext = SodiumInterface.switchRenderingContext.apply(worldRenderer, newSodiumContext);
        
        ((IEWorldRenderer) oldWorldRenderer).portal_setTransparencyShader(null);
        ((IEWorldRenderer) worldRenderer).portal_setTransparencyShader(null);
        ((IEWorldRenderer) worldRenderer).portal_setRenderDistance(renderDistance);
        
        //update lightmap
        if (!RenderStates.isDimensionRendered(newDimension)) {
            helper.lightmapTexture.updateLightmap(0);
        }
        
        //invoke rendering
        try {
            invokeWrapper.accept(() -> {
                client.getProfiler().startSection("render_portal_content");
                client.gameRenderer.renderWorld(
                    tickDelta,
                    Util.nanoTime(),
                    new MatrixStack()
                );
                client.getProfiler().endSection();
            });
        }
        catch (Throwable e) {
            limitedLogger.invoke(e::printStackTrace);
        }
        
        //recover
        SodiumInterface.switchRenderingContext.apply(worldRenderer, oldSodiumContext);
        
        ((IEMinecraftClient) client).setWorldRenderer(oldWorldRenderer);
        client.world = oldEntityWorld;
        ieGameRenderer.setLightmapTextureManager(oldLightmap);
        TileEntityRendererDispatcher.instance.world = oldEntityWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
        client.player.noClip = oldNoClip;
        ieGameRenderer.setDoRenderHand(oldDoRenderHand);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        ((IEParticleManager) client.particles).mySetWorld(oldEntityWorld);
        client.objectMouseOver = oldCrosshairTarget;
        ieGameRenderer.setCamera(oldCamera);
        
        ((IEWorldRenderer) oldWorldRenderer).portal_setTransparencyShader(oldTransparencyShader);
        ((IEWorldRenderer) worldRenderer).portal_setTransparencyShader(newTransparencyShader);
        
        FogRendererContext.swappingManager.popSwapping();
        
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(oldVisibleChunks);
        
        ((IEWorldRenderer) worldRenderer).setBufferBuilderStorage(oldBufferBuilder);
        ((IEMinecraftClient) client).setBufferBuilderStorage(oldClientBufferBuilder);
        
        ((IEWorldRenderer) worldRenderer).portal_setRenderDistance(oldRenderDistance);
        
        if (Global.looseVisibleChunkIteration) {
            client.renderChunksMany = true;
        }
        
        client.getRenderManager()
            .cacheActiveRenderInfo(
                client.world,
                oldCamera,
                client.pointedEntity
            );
        
        CHelper.checkGlError();
        
        //restore the camera entity pos
        cameraEntity.world = oldEntityWorld;
        McHelper.setEyePos(cameraEntity, oldEyePos, oldLastTickEyePos);
        
        resetGlStates();
    }
    
    /**
     * For example the Cull assumes that the culling is enabled before using it
     * {@link net.minecraft.client.render.RenderPhase.Cull}
     */
    public static void resetGlStates() {
        GlStateManager.disableAlphaTest();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        Minecraft.getInstance().gameRenderer.getLightTexture().disableLightmap();
        client.gameRenderer.getOverlayTexture().teardownOverlayColor();
    }
    
    public static void renderPlayerItself(Runnable doRenderEntity) {
        EntityRendererManager entityRenderDispatcher =
            ((IEWorldRenderer) client.worldRenderer).getEntityRenderDispatcher();
        NetworkPlayerInfo playerListEntry = CHelper.getClientPlayerListEntry();
        GameType originalGameMode = RenderStates.originalGameMode;
        
        Entity player = client.renderViewEntity;
        assert player != null;
        
        Vector3d oldPos = player.getPositionVec();
        Vector3d oldLastTickPos = McHelper.lastTickPosOf(player);
        GameType oldGameMode = playerListEntry.getGameType();
        
        McHelper.setPosAndLastTickPos(
            player, RenderStates.originalPlayerPos, RenderStates.originalPlayerLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(originalGameMode);
        
        doRenderEntity.run();
        
        McHelper.setPosAndLastTickPos(
            player, oldPos, oldLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
    }
    
    public static void resetFogState() {
        if (OFInterface.isFogDisabled.getAsBoolean()) {
            return;
        }
        
        if (OFInterface.isShaders.getAsBoolean()) {
            return;
        }
        
        forceResetFogState();
    }
    
    public static void forceResetFogState() {
        ActiveRenderInfo camera = client.gameRenderer.getActiveRenderInfo();
        float g = client.gameRenderer.getFarPlaneDistance();
        
        Vector3d cameraPos = camera.getProjectedView();
        double d = cameraPos.getX();
        double e = cameraPos.getY();
        double f = cameraPos.getZ();
        
        boolean bl2 = client.world.func_239132_a_().func_230493_a_(MathHelper.floor(d), MathHelper.floor(e)) ||
            client.ingameGUI.getBossOverlay().shouldCreateFog();
        
        FogRenderer.setupFog(camera, FogRenderer.FogType.FOG_TERRAIN, Math.max(g - 16.0F, 32.0F), bl2);
        FogRenderer.applyFog();
    }
    
    public static void updateFogColor() {
        FogRenderer.updateFogColor(
            client.gameRenderer.getActiveRenderInfo(),
            RenderStates.tickDelta,
            client.world,
            client.gameSettings.renderDistanceChunks,
            client.gameRenderer.getBossColorModifier(RenderStates.tickDelta)
        );
    }
    
    public static void resetDiffuseLighting(MatrixStack matrixStack) {
        RenderHelper.func_237533_a_(matrixStack.getLast().getMatrix());
    }
    
    public static void pruneRenderList(ObjectList<?> visibleChunks) {
        if (PortalRendering.isRendering()) {
            if (Global.cullSectionsBehind) {
                // this thing has no optimization effect -_-
                
                Portal renderingPortal = PortalRendering.getRenderingPortal();
                
                int firstInsideOne = Helper.indexOf(
                    visibleChunks,
                    obj -> {
                        ChunkRenderDispatcher.ChunkRender builtChunk =
                            ((IEWorldRendererChunkInfo) obj).getBuiltChunk();
                        AxisAlignedBB boundingBox = builtChunk.boundingBox;
                        
                        return FrustumCuller.isTouchingInsideContentArea(renderingPortal, boundingBox);
                    }
                );
                
                if (firstInsideOne != -1) {
                    visibleChunks.removeElements(0, firstInsideOne);
                }
                else {
                    visibleChunks.clear();
                }
            }
        }
    }
    
    public static void renderWorldInfoFramebuffer(
        RenderInfo renderInfo,
        Framebuffer framebuffer
    ) {
        CHelper.checkGlError();
        
        Framebuffer mcFb = client.getFramebuffer();
        
        Validate.isTrue(mcFb != framebuffer);
        
        ((IEMinecraftClient) client).setFrameBuffer(framebuffer);
        
        framebuffer.bindFramebuffer(true);
        
        CGlobal.renderer.invokeWorldRendering(renderInfo);
        
        ((IEMinecraftClient) client).setFrameBuffer(mcFb);
        
        mcFb.bindFramebuffer(true);
        
        CHelper.checkGlError();
    }
    
}
