package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.hiding_in_the_bushes.alternate_dimension.AlternateDimension;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.ducks.IEPlayerListEntry;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.ducks.IEWorldRendererChunkInfo;
import com.qouteall.immersive_portals.render.context_management.DimensionRenderHelper;
import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.OverworldDimension;
import org.lwjgl.opengl.GL11;

import java.util.function.Predicate;

public class MyGameRenderer {
    public static Minecraft client = Minecraft.getInstance();
    
    public static void doPruneVisibleChunks(ObjectList<?> visibleChunks) {
        if (CGlobal.renderer.isRendering()) {
            if (CGlobal.renderFewerInFastGraphic) {
                if (!Minecraft.getInstance().gameSettings.fancyGraphics) {
                    MyGameRenderer.pruneVisibleChunksInFastGraphics(visibleChunks);
                }
            }
        }
    }
    
    public static void renderWorld(
        float partialTicks,
        WorldRenderer newWorldRenderer,
        ClientWorld newWorld,
        Vec3d oldCameraPos,
        ClientWorld oldWorld
    ) {
        if (CGlobal.useHackedChunkRenderDispatcher) {
            ((IEWorldRenderer) newWorldRenderer).getBuiltChunkStorage().updateChunkPositions(
                client.renderViewEntity.getPosX(),
                client.renderViewEntity.getPosZ()
            );
        }
        
        if (Global.looseVisibleChunkIteration) {
            client.renderChunksMany = false;
        }
        
        IEGameRenderer ieGameRenderer = (IEGameRenderer) client.gameRenderer;
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(
                RenderDimensionRedirect.getRedirectedDimension(newWorld.dimension.getType())
            );
        NetworkPlayerInfo playerListEntry = CHelper.getClientPlayerListEntry();
        ActiveRenderInfo newCamera = new ActiveRenderInfo();
        
        //store old state
        WorldRenderer oldWorldRenderer = client.worldRenderer;
        LightTexture oldLightmap = client.gameRenderer.getLightTexture();
        GameType oldGameMode = playerListEntry.getGameType();
        boolean oldNoClip = client.player.noClip;
        boolean oldDoRenderHand = ieGameRenderer.getDoRenderHand();
        OFInterface.createNewRenderInfosNormal.accept(newWorldRenderer);
        ObjectList oldVisibleChunks = ((IEWorldRenderer) oldWorldRenderer).getVisibleChunks();
        RayTraceResult oldCrosshairTarget = client.objectMouseOver;
        ActiveRenderInfo oldCamera = client.gameRenderer.getActiveRenderInfo();
        
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(new ObjectArrayList());
        
        //switch
        ((IEMinecraftClient) client).setWorldRenderer(newWorldRenderer);
        client.world = newWorld;
        ieGameRenderer.setLightmapTextureManager(helper.lightmapTexture);
        helper.lightmapTexture.updateLightmap(0);
        helper.lightmapTexture.enableLightmap();
        TileEntityRendererDispatcher.instance.world = newWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(GameType.SPECTATOR);
        client.player.noClip = true;
        ieGameRenderer.setDoRenderHand(false);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        FogRendererContext.swappingManager.pushSwapping(
            RenderDimensionRedirect.getRedirectedDimension(newWorld.dimension.getType())
        );
        ((IEParticleManager) client.particles).mySetWorld(newWorld);
        if (BlockManipulationClient.remotePointedDim == newWorld.dimension.getType()) {
            client.objectMouseOver = BlockManipulationClient.remoteHitResult;
        }
        ieGameRenderer.setCamera(newCamera);
        
        client.getProfiler().startSection("render_portal_content");
        
        //invoke it!
        client.gameRenderer.renderWorld(
            partialTicks, 0,
            new MatrixStack()
        );
        
        client.getProfiler().endSection();
        
        //recover
        ((IEMinecraftClient) client).setWorldRenderer(oldWorldRenderer);
        client.world = oldWorld;
        ieGameRenderer.setLightmapTextureManager(oldLightmap);
        TileEntityRendererDispatcher.instance.world = oldWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
        client.player.noClip = oldNoClip;
        ieGameRenderer.setDoRenderHand(oldDoRenderHand);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        ((IEParticleManager) client.particles).mySetWorld(oldWorld);
        client.objectMouseOver = oldCrosshairTarget;
        ieGameRenderer.setCamera(oldCamera);
        
        FogRendererContext.swappingManager.popSwapping();
        
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(oldVisibleChunks);
        
        if (Global.looseVisibleChunkIteration) {
            client.renderChunksMany = true;
        }
        
        client.getRenderManager()
            .cacheActiveRenderInfo(client.world, oldCamera, client.pointedEntity);
    }
    
    public static void renderPlayerItself(Runnable doRenderEntity) {
        EntityRendererManager entityRenderDispatcher =
            ((IEWorldRenderer) client.worldRenderer).getEntityRenderDispatcher();
        NetworkPlayerInfo playerListEntry = CHelper.getClientPlayerListEntry();
        GameType originalGameMode = MyRenderHelper.originalGameMode;
        
        Entity player = client.renderViewEntity;
        assert player != null;
        
        Vec3d oldPos = player.getPositionVec();
        Vec3d oldLastTickPos = McHelper.lastTickPosOf(player);
        GameType oldGameMode = playerListEntry.getGameType();
        
        McHelper.setPosAndLastTickPos(
            player, MyRenderHelper.originalPlayerPos, MyRenderHelper.originalPlayerLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(originalGameMode);
        
        double distanceToCamera =
            player.getEyePosition(MyRenderHelper.tickDelta).distanceTo(client.gameRenderer.getActiveRenderInfo().getProjectedView());
        //avoid rendering player too near and block view
        if (distanceToCamera > 1) {
            doRenderEntity.run();
        }
        else {
//            Helper.log("ignored " + distanceToCamera);
        }
        
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
        
        Vec3d cameraPos = camera.getProjectedView();
        double d = cameraPos.getX();
        double e = cameraPos.getY();
        double f = cameraPos.getZ();
        
        boolean bl2 = client.world.dimension.doesXZShowFog(
            MathHelper.floor(d),
            MathHelper.floor(e)
        ) || client.ingameGUI.getBossOverlay().shouldCreateFog();
        
        FogRenderer.setupFog(
            camera,
            FogRenderer.FogType.FOG_TERRAIN,
            Math.max(g - 16.0F, 32.0F),
            bl2
        );
        FogRenderer.applyFog();
    }
    
    public static void updateFogColor() {
        FogRenderer.updateFogColor(
            client.gameRenderer.getActiveRenderInfo(),
            MyRenderHelper.tickDelta,
            client.world,
            client.gameSettings.renderDistanceChunks,
            client.gameRenderer.getBossColorModifier(MyRenderHelper.tickDelta)
        );
    }
    
    public static void resetDiffuseLighting(MatrixStack matrixStack) {
        RenderHelper.setupLevelDiffuseLighting(matrixStack.getLast().getMatrix());
    }
    
    //render fewer chunks when rendering portal
    //only active when graphic option is not fancy
    //NOTE we should not prune these chunks in setupTerrain()
    //because if it's pruned there these chunks will be rebuilt
    //then it will generate lag when player cross the portal by building chunks
    //we want the far chunks to be built but not rendered
    public static void pruneVisibleChunksInFastGraphics(ObjectList<?> visibleChunks) {
        int renderDistance = client.gameSettings.renderDistanceChunks;
        Vec3d cameraPos = client.gameRenderer.getActiveRenderInfo().getProjectedView();
        double range = ((renderDistance * 16) / 3) * ((renderDistance * 16) / 3);
        
        Predicate<ChunkRenderDispatcher.ChunkRender> builtChunkPredicate = (builtChunk) -> {
            Vec3d center = builtChunk.boundingBox.getCenter();
            return center.squareDistanceTo(cameraPos) > range;
        };
        
        pruneVisibleChunks(
            (ObjectList<Object>) visibleChunks,
            builtChunkPredicate
        );
    }
    
    private static void pruneVisibleChunks(
        ObjectList<Object> visibleChunks,
        Predicate<ChunkRenderDispatcher.ChunkRender> builtChunkPredicate
    ) {
        Helper.removeIf(
            visibleChunks,
            obj -> builtChunkPredicate.test(((IEWorldRendererChunkInfo) obj).getBuiltChunk())
        );
    }
    
    public static void renderSkyFor(
        DimensionType dimension,
        MatrixStack matrixStack,
        float tickDelta
    ) {
        
        ClientWorld newWorld = CGlobal.clientWorldLoader.getWorld(dimension);
        
        if (client.world.dimension instanceof AlternateDimension &&
            newWorld.dimension instanceof OverworldDimension
        ) {
            //avoid redirecting alternate to overworld
            //or sky will be dark when camera pos is low
            client.worldRenderer.renderSky(matrixStack, tickDelta);
            return;
        }
        
        WorldRenderer newWorldRenderer = CGlobal.clientWorldLoader.getWorldRenderer(dimension);
        
        ClientWorld oldWorld = client.world;
        WorldRenderer oldWorldRenderer = client.worldRenderer;
        FogRendererContext.swappingManager.pushSwapping(dimension);
        MyGameRenderer.forceResetFogState();
        
        client.world = newWorld;
        ((IEMinecraftClient) client).setWorldRenderer(newWorldRenderer);
        
        newWorldRenderer.renderSky(matrixStack, tickDelta);
        
        client.world = oldWorld;
        ((IEMinecraftClient) client).setWorldRenderer(oldWorldRenderer);
        FogRendererContext.swappingManager.popSwapping();
        MyGameRenderer.forceResetFogState();
    }
    
}
