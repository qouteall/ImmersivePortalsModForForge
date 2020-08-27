package com.qouteall.immersive_portals.teleportation;

import com.qouteall.hiding_in_the_bushes.MyNetworkClient;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.PehkuiInterface;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.TransformationManager;
import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class ClientTeleportationManager {
    Minecraft client = Minecraft.getInstance();
    public long tickTimeForTeleportation = 0;
    private long lastTeleportGameTime = 0;
    private Vector3d moveStartPoint = null;
    private long teleportTickTimeLimit = 0;
    
    // for debug
    public static boolean isTeleportingTick = false;
    public static boolean isTeleportingFrame = false;
    
    private static final int teleportLimit = 2;
    
    public ClientTeleportationManager() {
        ModMain.postClientTickSignal.connectWithWeakRef(
            this, ClientTeleportationManager::tick
        );
    }
    
    private void tick() {
        tickTimeForTeleportation++;
        changePlayerMotionIfCollidingWithPortal();
        
        isTeleportingTick = false;
    }
    
    public void acceptSynchronizationDataFromServer(
        RegistryKey<World> dimension,
        Vector3d pos,
        boolean forceAccept
    ) {
        if (!forceAccept) {
            if (isTeleportingFrequently()) {
                return;
            }
            // newly teleported by vanilla means
            if (client.player.ticksExisted < 200) {
                return;
            }
        }
        if (client.player.world.func_234923_W_() != dimension) {
            forceTeleportPlayer(dimension, pos);
        }
    }
    
    public void manageTeleportation(float tickDelta) {
        if (Global.disableTeleportation) {
            return;
        }
        
        isTeleportingFrame = false;
        
        if (client.world == null || client.player == null) {
            moveStartPoint = null;
        }
        else {
            //not initialized
            if (client.player.prevPosX == 0 && client.player.prevPosY == 0 && client.player.prevPosZ == 0) {
                return;
            }
            
            if (moveStartPoint != null) {
                for (int i = 0; i < teleportLimit; i++) {
                    boolean teleported = tryTeleport(tickDelta);
                    if (!teleported) {
                        break;
                    }
                    else {
                        if (i != 0) {
                            Helper.log("Nested teleport");
                        }
                    }
                }
            }
            
            moveStartPoint = getPlayerHeadPos(tickDelta);
        }
    }
    
    private boolean tryTeleport(float tickDelta) {
        Vector3d newHeadPos = getPlayerHeadPos(tickDelta);
        
        if (moveStartPoint.squareDistanceTo(newHeadPos) > 400) {
            Helper.log("The Player is Moving Too Fast!");
            return false;
        }
        
        Tuple<Portal, Vector3d> pair = CHelper.getClientNearbyPortals(32)
            .flatMap(portal -> {
                if (portal.isTeleportable()) {
                    Vector3d collidingPoint = portal.rayTrace(
                        moveStartPoint,
                        newHeadPos
                    );
                    if (collidingPoint != null) {
                        return Stream.of(new Tuple<>(portal, collidingPoint));
                    }
                }
                return Stream.empty();
            })
            .min(Comparator.comparingDouble(
                p -> p.getB().squareDistanceTo(moveStartPoint)
            ))
            .orElse(null);
        
        if (pair != null) {
            Portal portal = pair.getA();
            Vector3d collidingPos = pair.getB();
            
            teleportPlayer(portal);
            
            moveStartPoint = portal.transformPoint(collidingPos)
                .add(portal.getContentDirection().scale(0.001));
            //avoid teleporting through parallel portal due to floating point inaccuracy
            
            return true;
        }
        else {
            return false;
        }
    }
    
    private Vector3d getPlayerHeadPos(float tickDelta) {
        return client.player.getEyePosition(tickDelta);
//        Camera camera = client.gameRenderer.getCamera();
//        float cameraY = MathHelper.lerp(
//            tickDelta,
//            ((IECamera) camera).getLastCameraY(),
//            ((IECamera) camera).getCameraY()
//        );
//        return new Vec3d(
//            MathHelper.lerp((double) tickDelta, client.player.prevX, client.player.getX()),
//            MathHelper.lerp(
//                (double) tickDelta,
//                client.player.prevY,
//                client.player.getY()
//            ) + cameraY,
//            MathHelper.lerp((double) tickDelta, client.player.prevZ, client.player.getZ())
//        );
        
    }
    
    private void teleportPlayer(Portal portal) {
        if (tickTimeForTeleportation <= teleportTickTimeLimit) {
            return;
        }
        
        lastTeleportGameTime = tickTimeForTeleportation;
        
        ClientPlayerEntity player = client.player;
        
        RegistryKey<World> toDimension = portal.dimensionTo;
        
        Vector3d oldEyePos = McHelper.getEyePos(player);
        
        Vector3d newEyePos = portal.transformPoint(oldEyePos);
        Vector3d newLastTickEyePos = portal.transformPoint(McHelper.getLastTickEyePos(player));
        
        ClientWorld fromWorld = client.world;
        RegistryKey<World> fromDimension = fromWorld.func_234923_W_();
        
        if (fromDimension != toDimension) {
            ClientWorld toWorld = CGlobal.clientWorldLoader.getWorld(toDimension);
            
            changePlayerDimension(player, fromWorld, toWorld, newEyePos);
        }
        
        McHelper.setEyePos(player, newEyePos, newLastTickEyePos);
        McHelper.updateBoundingBox(player);
        
        PehkuiInterface.onClientPlayerTeleported.accept(portal);
        
        player.connection.sendPacket(MyNetworkClient.createCtsTeleport(
            fromDimension,
            oldEyePos,
            portal.getUniqueID()
        ));
        
        tickAfterTeleportation(player, newEyePos, newLastTickEyePos);
        
        amendChunkEntityStatus(player);
        
        McHelper.adjustVehicle(player);
        
        if (portal.teleportChangesScale) {
            player.setMotion(portal.transformLocalVecNonScale(player.getMotion()));
        }
        else {
            player.setMotion(portal.transformLocalVec(player.getMotion()));
        }
        
        TransformationManager.onClientPlayerTeleported(portal);
        
        if (player.getRidingEntity() != null) {
            disableTeleportFor(40);
        }
        
        //because the teleportation may happen before rendering
        //but after pre render info being updated
        RenderStates.updatePreRenderInfo(RenderStates.tickDelta);
        
        Helper.log(String.format("Client Teleported %s %s", portal, tickTimeForTeleportation));
        
        isTeleportingTick = true;
        isTeleportingFrame = true;
        
        if (portal.extension.adjustPositionAfterTeleport) {
            adjustPlayerPosition(player);
        }
    }
    
    
    public boolean isTeleportingFrequently() {
        return (tickTimeForTeleportation - lastTeleportGameTime <= 20) ||
            (tickTimeForTeleportation <= teleportTickTimeLimit);
    }
    
    private void forceTeleportPlayer(RegistryKey<World> toDimension, Vector3d destination) {
        Helper.log("force teleported " + toDimension + destination);
        
        ClientWorld fromWorld = client.world;
        RegistryKey<World> fromDimension = fromWorld.func_234923_W_();
        ClientPlayerEntity player = client.player;
        if (fromDimension == toDimension) {
            player.setPosition(
                destination.x,
                destination.y,
                destination.z
            );
            McHelper.adjustVehicle(player);
        }
        else {
            ClientWorld toWorld = CGlobal.clientWorldLoader.getWorld(toDimension);
            
            changePlayerDimension(player, fromWorld, toWorld, destination);
        }
        
        moveStartPoint = null;
        disableTeleportFor(20);
        
        amendChunkEntityStatus(player);
    }
    
    public void changePlayerDimension(
        ClientPlayerEntity player, ClientWorld fromWorld, ClientWorld toWorld, Vector3d newEyePos
    ) {
        Entity vehicle = player.getRidingEntity();
        player.detach();
        
        RegistryKey<World> toDimension = toWorld.func_234923_W_();
        RegistryKey<World> fromDimension = fromWorld.func_234923_W_();
        
        ClientPlayNetHandler workingNetHandler = ((IEClientWorld) fromWorld).getNetHandler();
        ClientPlayNetHandler fakedNetHandler = ((IEClientWorld) toWorld).getNetHandler();
        ((IEClientPlayNetworkHandler) workingNetHandler).setWorld(toWorld);
        ((IEClientPlayNetworkHandler) fakedNetHandler).setWorld(fromWorld);
        ((IEClientWorld) fromWorld).setNetHandler(fakedNetHandler);
        ((IEClientWorld) toWorld).setNetHandler(workingNetHandler);
        
        O_O.segregateClientEntity(fromWorld, player);
        
        player.world = toWorld;
        
        McHelper.setEyePos(player, newEyePos, newEyePos);
        McHelper.updateBoundingBox(player);
        
        toWorld.addPlayer(player.getEntityId(), player);
        
        client.world = toWorld;
        ((IEMinecraftClient) client).setWorldRenderer(
            CGlobal.clientWorldLoader.getWorldRenderer(toDimension)
        );
        
        toWorld.setScoreboard(fromWorld.getScoreboard());
        
        if (client.particles != null)
            client.particles.clearEffects(toWorld);
        
        TileEntityRendererDispatcher.instance.setWorld(toWorld);
        
        IEGameRenderer gameRenderer = (IEGameRenderer) Minecraft.getInstance().gameRenderer;
        gameRenderer.setLightmapTextureManager(CGlobal.clientWorldLoader
            .getDimensionRenderHelper(toDimension).lightmapTexture);
        
        if (vehicle != null) {
            Vector3d vehiclePos = new Vector3d(
                newEyePos.x,
                McHelper.getVehicleY(vehicle, player),
                newEyePos.z
            );
            moveClientEntityAcrossDimension(
                vehicle, toWorld,
                vehiclePos
            );
            player.startRiding(vehicle, true);
        }
        
        Helper.log(String.format(
            "Client Changed Dimension from %s to %s time: %s",
            fromDimension.func_240901_a_(),
            toDimension.func_240901_a_(),
            tickTimeForTeleportation
        ));
        
        
        OFInterface.onPlayerTraveled.accept(fromDimension, toDimension);
        
        FogRendererContext.onPlayerTeleport(fromDimension, toDimension);
        
        O_O.onPlayerChangeDimensionClient(fromDimension, toDimension);
    }
    
    private void amendChunkEntityStatus(Entity entity) {
        Chunk worldChunk1 = entity.world.getChunkAt(new BlockPos(entity.getPositionVec()));
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
    
    private void changePlayerMotionIfCollidingWithPortal() {
        ClientPlayerEntity player = client.player;
        List<Portal> portals = player.world.getEntitiesWithinAABB(
            Portal.class,
            player.getBoundingBox().grow(0.5),
            e -> !(e instanceof Mirror)
        );
        
        if (!portals.isEmpty()) {
            Portal portal = portals.get(0);
            if (portal.extension.motionAffinity > 0) {
                changeMotion(player, portal);
            }
            else if (portal.extension.motionAffinity < 0) {
                if (player.getMotion().length() > 0.7) {
                    changeMotion(player, portal);
                }
            }
        }
    }
    
    private void changeMotion(Entity player, Portal portal) {
        Vector3d velocity = player.getMotion();
        player.setMotion(velocity.scale(1 + portal.extension.motionAffinity));
    }
    
    //foot pos, not eye pos
    public static void moveClientEntityAcrossDimension(
        Entity entity,
        ClientWorld newWorld,
        Vector3d newPos
    ) {
        ClientWorld oldWorld = (ClientWorld) entity.world;
        O_O.segregateClientEntity(oldWorld, entity);
        entity.world = newWorld;
        entity.setPosition(newPos.x, newPos.y, newPos.z);
        newWorld.addEntity(entity.getEntityId(), entity);
    }
    
    public void disableTeleportFor(int ticks) {
        teleportTickTimeLimit = tickTimeForTeleportation + ticks;
    }
    
    private static void tickAfterTeleportation(ClientPlayerEntity player, Vector3d newEyePos, Vector3d newLastTickEyePos) {
        // update collidingPortal
        McHelper.findEntitiesByBox(
            Portal.class,
            player.world,
            player.getBoundingBox(),
            10,
            portal -> true
        ).forEach(CollisionHelper::notifyCollidingPortals);
        
        CollisionHelper.updateClientGlobalPortalCollidingPortal();
        
        player.tick();
        McHelper.setEyePos(player, newEyePos, newLastTickEyePos);
        McHelper.updateBoundingBox(player);
    }
    
    private static void adjustPlayerPosition(ClientPlayerEntity player) {
        AxisAlignedBB boundingBox = player.getBoundingBox();
        AxisAlignedBB bottomHalfBox = boundingBox.contract(0, boundingBox.getYSize() / 2, 0);
        Stream<VoxelShape> collisions = player.world.getCollisionShapes(
            player, bottomHalfBox
        );
        double maxCollisionY = collisions.mapToDouble(s -> s.getBoundingBox().maxY)
            .max().orElse(player.getPosY());
        
        double delta = maxCollisionY - player.getPosY();
        
        if (delta <= 0) {
            return;
        }
        
        final int ticks = 5;
        
        double originalY = player.getPosY();
        
        int[] counter = {0};
        ModMain.clientTaskList.addTask(() -> {
            if (player.removed) {
                return true;
            }
            if (player.getPosY() < originalY - 1 || player.getPosY() > maxCollisionY + 1) {
                return true;
            }
            
            if (counter[0] >= 5) {
                return true;
            }
            
            counter[0]++;
            
            double progress = ((double) counter[0]) / ticks;
            progress = TransformationManager.mapProgress(progress);
            double newY = MathHelper.lerp(
                progress,
                originalY, maxCollisionY
            );
            player.setRawPosition(player.getPosX(), newY, player.getPosZ());
            McHelper.updateBoundingBox(player);
            
            return false;
        });
        
    }
    
    
}
