package com.qouteall.immersive_portals.teleportation;

import com.immersive_portals.network.CtsTeleport;
import com.immersive_portals.network.NetworkMain;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.exposer.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.exposer.IEClientWorld;
import com.qouteall.immersive_portals.exposer.IEMinecraftClient;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.DownloadTerrainScreen;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.dimension.DimensionType;

public class ClientTeleportationManager {
    Minecraft mc = Minecraft.getInstance();
    private long tickTimeForTeleportation = 0;
    private long lastTeleportGameTime = 0;
    
    public ClientTeleportationManager() {
        ModMain.preRenderSignal.connectWithWeakRef(
            this, ClientTeleportationManager::manageTeleportation
        );
        ModMain.postClientTickSignal.connectWithWeakRef(
            this, ClientTeleportationManager::tick
        );
    }
    
    private static void tick(ClientTeleportationManager this_) {
        this_.manageTeleportation();
        this_.tickTimeForTeleportation++;
    }
    
    public void acceptSynchronizationDataFromServer(
        DimensionType dimension,
        Vec3d pos,
        boolean forceAccept
    ) {
        if (!forceAccept) {
            if (isTeleportingFrequently()) {
                return;
            }
        }
        if (mc.player.dimension != dimension) {
            forceTeleportPlayer(dimension, pos);
        }
        getOutOfLoadingScreen(dimension, pos);
    }
    
    private void manageTeleportation() {
        if (mc.world != null && mc.player != null) {
            Helper.getEntitiesNearby(
                mc.player,
                Portal.class,
                10
            ).filter(
                portal -> portal.shouldEntityTeleport(mc.player)
            ).findFirst().ifPresent(
                portal -> onEntityGoInsidePortal(mc.player, portal)
            );
        }
    }
    
    private void onEntityGoInsidePortal(Entity entity, Portal portal) {
        if (entity instanceof ClientPlayerEntity) {
            assert entity.dimension == portal.dimension;
            teleportPlayer(portal);
        }
    }
    
    private void teleportPlayer(Portal portal) {
        if (isTeleportingFrequently()) {
            return;
        }
        lastTeleportGameTime = tickTimeForTeleportation;
        
        ClientPlayerEntity player = mc.player;
        
        DimensionType toDimension = portal.dimensionTo;
        
        if (!portal.shouldEntityTeleport(mc.player)) {
            return;
        }
    
        if (mc.player.isBeingRidden() || mc.player.isPassenger()) {
            return;
        }
        
        Vec3d oldPos = player.getPositionVec();
        
        Vec3d newPos = portal.applyTransformationToPoint(oldPos);
        Vec3d newLastTickPos = portal.applyTransformationToPoint(Helper.lastTickPosOf(player));
        
        ClientWorld fromWorld = mc.world;
        DimensionType fromDimension = fromWorld.dimension.getType();
        
        if (fromDimension != toDimension) {
            ClientWorld toWorld = CGlobal.clientWorldLoader.getOrCreateFakedWorld(toDimension);
    
            changePlayerDimension(player, fromWorld, toWorld, newPos);
        }
        
        player.setPosition(newPos.x, newPos.y, newPos.z);
        Helper.setPosAndLastTickPos(player, newPos, newLastTickPos);
    
        NetworkMain.sendToServer(new CtsTeleport(
            fromDimension,
            oldPos,
            portal.getEntityId()
        ));
        
        amendChunkEntityStatus(player);
    
        slowDownIfTooFast(player);
    }
    
    private boolean isTeleportingFrequently() {
        if (tickTimeForTeleportation - lastTeleportGameTime <= 2) {
            return true;
        }
        else {
            return false;
        }
    }
    
    private void forceTeleportPlayer(DimensionType toDimension, Vec3d destination) {
        Helper.log("force teleported " + toDimension + destination);
        
        ClientWorld fromWorld = mc.world;
        DimensionType fromDimension = fromWorld.dimension.getType();
        ClientPlayerEntity player = mc.player;
        if (fromDimension == toDimension) {
            player.setPosition(
                destination.x,
                destination.y,
                destination.z
            );
        }
        else {
            ClientWorld toWorld = CGlobal.clientWorldLoader.getOrCreateFakedWorld(toDimension);
            
            changePlayerDimension(player, fromWorld, toWorld, destination);
        }
    
        amendChunkEntityStatus(player);
    }
    
    private void changePlayerDimension(
        ClientPlayerEntity player, ClientWorld fromWorld, ClientWorld toWorld, Vec3d destination
    ) {
    
        ClientPlayNetHandler workingNetHandler = ((IEClientWorld) fromWorld).getNetHandler();
        ClientPlayNetHandler fakedNetHandler = ((IEClientWorld) toWorld).getNetHandler();
        ((IEClientPlayNetworkHandler) workingNetHandler).setWorld(toWorld);
        ((IEClientPlayNetworkHandler) fakedNetHandler).setWorld(fromWorld);
        ((IEClientWorld) fromWorld).setNetHandler(fakedNetHandler);
        ((IEClientWorld) toWorld).setNetHandler(workingNetHandler);
    
        fromWorld.removeEntityFromWorld(player.getEntityId());
        player.removed = false;
        player.world = toWorld;
        player.dimension = toWorld.dimension.getType();
        player.posX = destination.x;
        player.posY = destination.y;
        player.posZ = destination.z;
    
        toWorld.addPlayer(player.getEntityId(), player);
    
        mc.world = toWorld;
        mc.worldRenderer = CGlobal.clientWorldLoader.getWorldRenderer(toWorld.dimension.getType());
    
        toWorld.setScoreboard(fromWorld.getScoreboard());
    
        if (mc.particles != null)
            mc.particles.clearEffects(toWorld);
    
        TileEntityRendererDispatcher.instance.setWorld(toWorld);
    
        CGlobal.clientWorldLoader
            .getDimensionRenderHelper(toWorld.dimension.getType())
            .switchToMe();
        
        Helper.log(String.format(
            "Client Changed Dimension from %s to %s time: %s",
            fromWorld.dimension.getType(),
            toWorld.dimension.getType(),
            tickTimeForTeleportation
        ));
    
        Helper.log("Portal Number Near Player Now" +
            Helper.getEntitiesNearby(mc.player, Portal.class, 10).count()
        );

//        if (OFHelper.getIsUsingShader()) {
//            OFGlobal.shaderContextManager.onPlayerTraveled(
//                fromWorld.dimension.getType(),
//                toWorld.dimension.getType()
//            );
//        }
    }
    
    private void amendChunkEntityStatus(Entity entity) {
        Chunk worldChunk1 = entity.world.getChunkAt(entity.getPosition());
        IChunk chunk2 = entity.world.getChunk(entity.chunkCoordX, entity.chunkCoordZ);
        removeEntityFromChunk(entity, worldChunk1);
        if (chunk2 instanceof Chunk) {
            removeEntityFromChunk(entity, ((Chunk) chunk2));
        }
        worldChunk1.addEntity(entity);
    }
    
    private void removeEntityFromChunk(Entity entity, Chunk worldChunk) {
        for (ClassInheritanceMultiMap<Entity> section : worldChunk.getEntityLists()) {
            section.remove(entity);
        }
    }
    
    private void getOutOfLoadingScreen(DimensionType dimension, Vec3d playerPos) {
        if (((IEMinecraftClient) mc).getCurrentScreen() instanceof DownloadTerrainScreen) {
            Helper.err("Manually getting out of loading screen. The game is in abnormal state.");
            if (mc.player.dimension != dimension) {
                Helper.err("Manually fix dimension state while loading terrain");
                ClientWorld toWorld = CGlobal.clientWorldLoader.getOrCreateFakedWorld(dimension);
                changePlayerDimension(mc.player, mc.world, toWorld, playerPos);
            }
            mc.player.setPosition(playerPos.x, playerPos.y, playerPos.z);
            mc.displayGuiScreen(null);
        }
    }
    
    //if player is falling through looping portals, make it slower
    private void slowDownIfTooFast(ClientPlayerEntity player) {
        if (player.getMotion().length() > 1) {
            player.setMotion(player.getMotion().scale(0.5));
        }
    }
}
