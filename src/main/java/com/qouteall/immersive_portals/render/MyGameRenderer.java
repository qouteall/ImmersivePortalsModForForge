package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.*;
import com.qouteall.immersive_portals.ducks.IEChunkRenderList;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEPlayerListEntry;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class MyGameRenderer {
    private Minecraft mc = Minecraft.getInstance();
    private double[] clipPlaneEquation;
    
    public MyGameRenderer() {
    
    }
    
    public void renderWorld(
        float partialTicks,
        WorldRenderer newWorldRenderer,
        ClientWorld newWorld,
        Vec3d oldCameraPos
    ) {
        ViewFrustum chunkRenderDispatcher =
            ((IEWorldRenderer) newWorldRenderer).getChunkRenderDispatcher();
        chunkRenderDispatcher.updateChunkPositions(
            mc.player.posX, mc.player.posZ
        );
        
        IEGameRenderer ieGameRenderer = (IEGameRenderer) mc.gameRenderer;
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(newWorld.dimension.getType());
        NetworkPlayerInfo playerListEntry = CHelper.getClientPlayerListEntry();
        ActiveRenderInfo newCamera = new ActiveRenderInfo();
    
        //store old state
        WorldRenderer oldWorldRenderer = mc.worldRenderer;
        ClientWorld oldWorld = mc.world;
        LightTexture oldLightmap = ieGameRenderer.getLightmapTextureManager();
        FogRenderer oldFogRenderer = ieGameRenderer.getBackgroundRenderer();
        GameType oldGameMode = playerListEntry.getGameType();
        boolean oldNoClip = mc.player.noClip;
        boolean oldDoRenderHand = ieGameRenderer.getDoRenderHand();
        List oldChunkInfos = ((IEWorldRenderer) mc.worldRenderer).getChunkInfos();
        IEChunkRenderList oldChunkRenderList =
            (IEChunkRenderList) ((IEWorldRenderer) oldWorldRenderer).getChunkRenderList();
        //List<ChunkRenderer> oldChunkRenderers = oldChunkRenderList.getChunkRenderers();
    
        OFInterface.createNewRenderInfosNormal.accept(newWorldRenderer);
        
        //switch
        mc.worldRenderer = newWorldRenderer;
        mc.world = newWorld;
        ieGameRenderer.setBackgroundRenderer(helper.fogRenderer);
        ieGameRenderer.setLightmapTextureManager(helper.lightmapTexture);
        helper.lightmapTexture.updateLightmap(0);
        helper.lightmapTexture.enableLightmap();
        TileEntityRendererDispatcher.instance.world = newWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(GameType.SPECTATOR);
        mc.player.noClip = true;
        ieGameRenderer.setDoRenderHand(false);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
    
        CGlobal.renderInfoNumMap.put(
            newWorld.dimension.getType(),
            ((IEWorldRenderer) mc.worldRenderer).getChunkInfos().size()
        );
    
        updateCullingPlane();
        
        //this is important
        GlStateManager.disableBlend();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        RenderHelper.disableStandardItemLighting();
        ((GameRenderer) ieGameRenderer).disableLightmap();
    
        mc.getProfiler().startSection("render_portal_content");
    
        CGlobal.switchedFogRenderer = ieGameRenderer.getBackgroundRenderer();
        
        //invoke it!
        OFInterface.beforeRenderCenter.accept(partialTicks);
        ieGameRenderer.renderCenter_(partialTicks, getChunkUpdateFinishTime());
        OFInterface.afterRenderCenter.run();
    
        mc.getProfiler().endSection();
    
        //recover
        mc.worldRenderer = oldWorldRenderer;
        mc.world = oldWorld;
        ieGameRenderer.setBackgroundRenderer(oldFogRenderer);
        ieGameRenderer.setLightmapTextureManager(oldLightmap);
        TileEntityRendererDispatcher.instance.world = oldWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
        mc.player.noClip = oldNoClip;
        ieGameRenderer.setDoRenderHand(oldDoRenderHand);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
        ((IEWorldRenderer) mc.worldRenderer).setChunkInfos(oldChunkInfos);
    
    
        oldChunkRenderList.setCameraPos(oldCameraPos.x, oldCameraPos.y, oldCameraPos.z);
        // oldChunkRenderList.setChunkRenderers(oldChunkRenderers);
        
    }
    
    public void endCulling() {
        GL11.glDisable(GL11.GL_CLIP_PLANE0);
    }
    
    public void startCulling() {
        //shaders does not compatible with glCullPlane
        //I have to modify shader code
        if (CGlobal.useFrontCulling && !OFInterface.isShaders.getAsBoolean()) {
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
        }
    }
    
    //NOTE the actual culling plane is related to current model view matrix
    public void updateCullingPlane() {
        clipPlaneEquation = calcClipPlaneEquation();
        if (!OFInterface.isShaders.getAsBoolean()) {
            GL11.glClipPlane(GL11.GL_CLIP_PLANE0, clipPlaneEquation);
        }
    }
    
    private long getChunkUpdateFinishTime() {
        return 0;
    }
    
    //invoke this before rendering portal
    //its result depends on camra pos
    private double[] calcClipPlaneEquation() {
        Portal portal = CGlobal.renderer.getRenderingPortal();
    
        Vec3d planeNormal = portal.getNormal().scale(-1);
    
        if (portal instanceof Mirror && OFInterface.isShaders.getAsBoolean()) {
            planeNormal = planeNormal.scale(-1);
        }
        
        Vec3d portalPos = portal.getPositionVec().subtract(
            mc.gameRenderer.getActiveRenderInfo().getProjectedView()
        );
    
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
    
    public void renderPlayerItselfIfNecessary() {
        if (CGlobal.renderer.shouldRenderPlayerItself()) {
            renderPlayerItself(
                MyRenderHelper.originalPlayerPos,
                MyRenderHelper.originalPlayerLastTickPos,
                MyRenderHelper.partialTicks
            );
        }
    }
    
    private void renderPlayerItself(Vec3d playerPos, Vec3d playerLastTickPos, float patialTicks) {
        EntityRendererManager entityRenderDispatcher =
            ((IEWorldRenderer) mc.worldRenderer).getEntityRenderDispatcher();
        NetworkPlayerInfo playerListEntry = CHelper.getClientPlayerListEntry();
        GameType originalGameMode = MyRenderHelper.originalGameMode;
        
        Entity player = mc.renderViewEntity;
        assert player != null;
        
        Vec3d oldPos = player.getPositionVec();
        Vec3d oldLastTickPos = Helper.lastTickPosOf(player);
        GameType oldGameMode = playerListEntry.getGameType();
    
        McHelper.setPosAndLastTickPos(
            player, playerPos, playerLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(originalGameMode);
    
        entityRenderDispatcher.renderEntityStatic(player, patialTicks, false);
    
        McHelper.setPosAndLastTickPos(
            player, oldPos, oldLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
    }
}
