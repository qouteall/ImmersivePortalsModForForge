package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.*;
import com.qouteall.immersive_portals.ducks.*;
import com.qouteall.immersive_portals.portal.Portal;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import org.lwjgl.opengl.GL11;

import java.util.function.Predicate;

public class MyGameRenderer {
    private Minecraft mc = Minecraft.getInstance();
    private double[] clipPlaneEquation;
    
    public MyGameRenderer() {
    
    }
    
    public void renderWorld(
        float partialTicks,
        WorldRenderer newWorldRenderer,
        ClientWorld newWorld,
        Vec3d oldCameraPos,
        ClientWorld oldWorld
    ) {
//        BuiltChunkStorage chunkRenderDispatcher =
//            ((IEWorldRenderer) newWorldRenderer).getBuiltChunkStorage();
//        chunkRenderDispatcher.updateCameraPosition(
//            mc.player.getX(), mc.player.getZ()
//        );
    
        IEGameRenderer ieGameRenderer = (IEGameRenderer) mc.gameRenderer;
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(newWorld.dimension.getType());
        NetworkPlayerInfo playerListEntry = CHelper.getClientPlayerListEntry();
        ActiveRenderInfo newCamera = new ActiveRenderInfo();
    
        //store old state
        WorldRenderer oldWorldRenderer = mc.worldRenderer;
        LightTexture oldLightmap = ieGameRenderer.getLightmapTextureManager();
        GameType oldGameMode = playerListEntry.getGameType();
        boolean oldNoClip = mc.player.noClip;
        boolean oldDoRenderHand = ieGameRenderer.getDoRenderHand();
        OFInterface.createNewRenderInfosNormal.accept(newWorldRenderer);
        ObjectList oldVisibleChunks = ((IEWorldRenderer) oldWorldRenderer).getVisibleChunks();
    
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(new ObjectArrayList());
    
        //switch
        ((IEMinecraftClient) mc).setWorldRenderer(newWorldRenderer);
        mc.world = newWorld;
        ieGameRenderer.setLightmapTextureManager(helper.lightmapTexture);
        helper.lightmapTexture.updateLightmap(0);
        helper.lightmapTexture.enableLightmap();
        TileEntityRendererDispatcher.instance.world = newWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(GameType.SPECTATOR);
        mc.player.noClip = true;
        ieGameRenderer.setDoRenderHand(false);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        FogRendererContext.swappingManager.pushSwapping(newWorld.dimension.getType());
        ((IEParticleManager) mc.particles).mySetWorld(newWorld);
    
        mc.getProfiler().startSection("render_portal_content");
    
        //invoke it!
        OFInterface.beforeRenderCenter.accept(partialTicks);
        mc.gameRenderer.renderWorld(
            partialTicks, getChunkUpdateFinishTime(),
            new MatrixStack()
        );
        OFInterface.afterRenderCenter.run();
    
        mc.getProfiler().endSection();
    
        //recover
        ((IEMinecraftClient) mc).setWorldRenderer(oldWorldRenderer);
        mc.world = oldWorld;
        ieGameRenderer.setLightmapTextureManager(oldLightmap);
        TileEntityRendererDispatcher.instance.world = oldWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
        mc.player.noClip = oldNoClip;
        ieGameRenderer.setDoRenderHand(oldDoRenderHand);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        ((IEParticleManager) mc.particles).mySetWorld(oldWorld);
    
        FogRendererContext.swappingManager.popSwapping();
    
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(oldVisibleChunks);
        ((IECamera) mc.gameRenderer.getActiveRenderInfo()).resetState(oldCameraPos, oldWorld);
    }
    
    public void endCulling() {
        GL11.glDisable(GL11.GL_CLIP_PLANE0);
    }
    
    public void startCulling() {
        //shaders do not compatible with glCullPlane
        //I have to modify shader code
        if (CGlobal.useFrontCulling && !OFInterface.isShaders.getAsBoolean()) {
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
        }
    }
    
    //NOTE the actual culling plane is related to current model view matrix
    public void updateCullingPlane(MatrixStack matrixStack) {
        McHelper.runWithTransformation(
            matrixStack,
            () -> {
                clipPlaneEquation = calcClipPlaneEquation();
                if (!OFInterface.isShaders.getAsBoolean()) {
                    GL11.glClipPlane(GL11.GL_CLIP_PLANE0, clipPlaneEquation);
                }
            }
        );
    }
    
    private long getChunkUpdateFinishTime() {
        return 0;
    }
    
    //invoke this before rendering portal
    //its result depends on camra pos
    private double[] calcClipPlaneEquation() {
        Portal portal = CGlobal.renderer.getRenderingPortal();
    
        Vec3d planeNormal = portal.getContentDirection();
    
        Vec3d portalPos = portal.destination
            .subtract(portal.getNormal().scale(-0.01))//avoid z fighting
            .subtract(mc.gameRenderer.getActiveRenderInfo().getProjectedView());
    
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.scale(-1).dotProduct(portalPos);
    
        return new double[]{
            planeNormal.x,
            planeNormal.y,
            planeNormal.z,
            c
        };
    }
    
    public double[] getClipPlaneEquation() {
        return clipPlaneEquation;
    }
    
    
    public void renderPlayerItself(Runnable doRenderEntity) {
        EntityRendererManager entityRenderDispatcher =
            ((IEWorldRenderer) mc.worldRenderer).getEntityRenderDispatcher();
        NetworkPlayerInfo playerListEntry = CHelper.getClientPlayerListEntry();
        GameType originalGameMode = MyRenderHelper.originalGameMode;
        
        Entity player = mc.renderViewEntity;
        assert player != null;
        
        Vec3d oldPos = player.getPositionVec();
        Vec3d oldLastTickPos = McHelper.lastTickPosOf(player);
        GameType oldGameMode = playerListEntry.getGameType();
        
        McHelper.setPosAndLastTickPos(
            player, MyRenderHelper.originalPlayerPos, MyRenderHelper.originalPlayerLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(originalGameMode);
        
        doRenderEntity.run();
        
        McHelper.setPosAndLastTickPos(
            player, oldPos, oldLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
    }
    
    public void resetFog() {
        ActiveRenderInfo camera = mc.gameRenderer.getActiveRenderInfo();
        float g = mc.gameRenderer.getFarPlaneDistance();
        
        Vec3d cameraPos = camera.getProjectedView();
        double d = cameraPos.getX();
        double e = cameraPos.getY();
        double f = cameraPos.getZ();
        
        boolean bl2 = mc.world.dimension.doesXZShowFog(
            MathHelper.floor(d),
            MathHelper.floor(e)
        ) || mc.ingameGUI.getBossOverlay().shouldCreateFog();
        
        FogRenderer.setupFog(
            camera,
            FogRenderer.FogType.FOG_TERRAIN,
            Math.max(g - 16.0F, 32.0F),
            bl2
        );
        
    }
    
    //render fewer chunks when rendering portal
    //only active when graphic option is not fancy
    //NOTE we should not prune these chunks in setupTerrain()
    //because if it's pruned there these chunks will be rebuilt
    //then it will generate lag when player cross the portal by building chunks
    //we want the far chunks to be built but not rendered
    public void pruneVisibleChunks(ObjectList<?> visibleChunks, int renderDistance) {
        Vec3d cameraPos = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
        double range = ((renderDistance * 16) / 3) * ((renderDistance * 16) / 3);
    
        Predicate<Object> predicate = obj -> {
            ChunkRenderDispatcher.ChunkRender builtChunk = ((IEWorldRendererChunkInfo) obj).getBuiltChunk();
            Vec3d center = builtChunk.boundingBox.getCenter();
            return center.squareDistanceTo(cameraPos) > range;
        };
    
        int pruneIndex = visibleChunks.size();
        for (int i = 0; i < visibleChunks.size(); i++) {
            Object obj = visibleChunks.get(i);
            if (predicate.test(obj)) {
                pruneIndex = i;
                break;
            }
        }
    
        visibleChunks.removeElements(pruneIndex, visibleChunks.size());
    
    }
    
}
