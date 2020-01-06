package com.qouteall.immersive_portals.teleportation;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEServerPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEServerPlayerEntity;
import com.qouteall.immersive_portals.network.NetworkMain;
import com.qouteall.immersive_portals.network.StcDimensionConfirm;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

import java.util.*;
import java.util.stream.Stream;

public class ServerTeleportationManager {
    private Set<ServerPlayerEntity> teleportingEntities = new HashSet<>();
    private WeakHashMap<Entity, Long> lastTeleportGameTime = new WeakHashMap<>();
    
    public ServerTeleportationManager() {
        ModMain.postServerTickSignal.connectWithWeakRef(this, ServerTeleportationManager::tick);
        Portal.serverPortalTickSignal.connectWithWeakRef(
            this, (this_, portal) ->
                portal.getEntitiesToTeleport().forEach(entity -> {
                    if (!(entity instanceof ServerPlayerEntity)) {
                        ModMain.serverTaskList.addTask(() -> {
                            teleportRegularEntity(entity, portal);
                            return true;
                        });
                    }
                })
        );
    }
    
    public void onPlayerTeleportedInClient(
        ServerPlayerEntity player,
        DimensionType dimensionBefore,
        Vec3d posBefore,
        UUID portalId
    ) {
        ServerWorld beforeWorld = McHelper.getServer().getWorld(dimensionBefore);
        Entity portalEntity = beforeWorld.getEntityByUuid(portalId);
        if (portalEntity == null) {
            portalEntity = GlobalPortalStorage.get(beforeWorld).data.stream()
                .filter(
                    p -> p.getUniqueID().equals(portalId)
                ).findFirst().orElse(null);
        }
        lastTeleportGameTime.put(player, McHelper.getServerGameTime());
        
        if (canPlayerTeleport(player, dimensionBefore, posBefore, portalEntity)) {
            if (isTeleporting(player)) {
                Helper.err(player.toString() + "is teleporting frequently");
            }
    
            DimensionType dimensionTo = ((Portal) portalEntity).dimensionTo;
            Vec3d newPos = ((Portal) portalEntity).applyTransformationToPoint(posBefore);
    
            teleportPlayer(player, dimensionTo, newPos);
    
            ((Portal) portalEntity).onEntityTeleportedOnServer(player);
        }
        else {
            Helper.err(String.format(
                "Player cannot teleport through portal %s %s %s %s",
                player.getName().getFormattedText(),
                player.dimension,
                player.getPositionVec(),
                portalEntity
            ));
        }
    }
    
    private boolean canPlayerTeleport(
        ServerPlayerEntity player,
        DimensionType dimensionBefore,
        Vec3d posBefore,
        Entity portalEntity
    ) {
        return canPlayerReachPos(player, dimensionBefore, posBefore) &&
            portalEntity instanceof Portal &&
            ((Portal) portalEntity).getDistanceToNearestPointInPortal(posBefore) < 10 &&
            !player.isPassenger();
    }
    
    private boolean canPlayerReachPos(
        ServerPlayerEntity player,
        DimensionType dimension,
        Vec3d pos
    ) {
        return player.dimension == dimension ?
            isClose(pos, player.getPositionVec())
            :
            McHelper.getEntitiesNearby(player, Portal.class, 10)
                .anyMatch(
                    portal -> portal.dimensionTo == dimension &&
                        isClose(pos, portal.destination)
                );
    }
    
    private static boolean isClose(Vec3d a, Vec3d b) {
        return a.squareDistanceTo(b) < 15 * 15;
    }
    
    private void teleportPlayer(
        ServerPlayerEntity player,
        DimensionType dimensionTo,
        Vec3d newPos
    ) {
        ServerWorld fromWorld = (ServerWorld) player.world;
        ServerWorld toWorld = McHelper.getServer().getWorld(dimensionTo);
    
        if (player.dimension == dimensionTo) {
            player.setPosition(newPos.x, newPos.y, newPos.z);
        }
        else {
            changePlayerDimension(player, fromWorld, toWorld, newPos);
            ((IEServerPlayNetworkHandler) player.connection).cancelTeleportRequest();
        }
    
        ((IEServerPlayerEntity) player).setIsInTeleportationState(true);
        player.connection.captureCurrentPosition();
    }
    
    //TODO add forge events
    
    /**
     * {@link ServerPlayerEntity#changeDimension(DimensionType)}
     */
    private void changePlayerDimension(
        ServerPlayerEntity player,
        ServerWorld fromWorld,
        ServerWorld toWorld,
        Vec3d destination
    ) {
        BlockPos oldPos = player.getPosition();
        
        teleportingEntities.add(player);
    
        //TODO fix travel when riding entity
        player.detach();
    
        //new dimension transition method
        player.dimension = toWorld.dimension.getType();
        fromWorld.removeEntity(player, true);
        player.revive();

//        fromWorld.removePlayer(player);
//        player.removed = false;
    
        player.posX = destination.x;
        player.posY = destination.y;
        player.posZ = destination.z;
        
        player.world = toWorld;
        player.dimension = toWorld.dimension.getType();
        toWorld.addRespawnedPlayer(player);
    
        toWorld.chunkCheck(player);
    
        McHelper.getServer().getPlayerList().sendWorldInfo(
            player, toWorld
        );
        
        player.interactionManager.setWorld(toWorld);
    
        Helper.log(String.format(
            "%s teleported from %s %s to %s %s",
            player.getName().getFormattedText(),
            fromWorld.dimension.getType(),
            oldPos,
            toWorld.dimension.getType(),
            player.getPosition()
        ));
    
        //this is used for the advancement of "we need to go deeper"
        //and the advancement of travelling for long distance through nether
        if (toWorld.dimension.getType() == DimensionType.THE_NETHER) {
            //this is used for
            ((IEServerPlayerEntity) player).setEnteredNetherPos(player.getPositionVec());
        }
        ((IEServerPlayerEntity) player).updateDimensionTravelAdvancements(fromWorld);
    
        net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerChangedDimensionEvent(
            player,
            fromWorld.dimension.getType(),
            toWorld.dimension.getType()
        );
    }
    
    private void sendPositionConfirmMessage(ServerPlayerEntity player) {
        NetworkMain.sendToPlayer(player, new StcDimensionConfirm(
            player.dimension, player.getPositionVec()
        ));
    }
    
    private void tick() {
        teleportingEntities = new HashSet<>();
        long tickTimeNow = McHelper.getServerGameTime();
        if (tickTimeNow % 10 == 7) {
            ArrayList<ServerPlayerEntity> copiedPlayerList =
                McHelper.getCopiedPlayerList();
            for (ServerPlayerEntity player : copiedPlayerList) {
                if (!player.queuedEndExit) {
                    Long lastTeleportGameTime =
                        this.lastTeleportGameTime.getOrDefault(player, 0L);
                    if (tickTimeNow - lastTeleportGameTime > 60) {
                        sendPositionConfirmMessage(player);
                        ((IEServerPlayerEntity) player).setIsInTeleportationState(false);
                    }
                    else {
                        ((IEServerPlayNetworkHandler) player.connection).cancelTeleportRequest();
                    }
                }
            }
        }
    }
    
    public boolean isTeleporting(ServerPlayerEntity entity) {
        return teleportingEntities.contains(entity);
    }
    
    private void teleportRegularEntity(Entity entity, Portal portal) {
        assert entity.dimension == portal.dimension;
        assert !(entity instanceof ServerPlayerEntity);
    
        long currGameTime = McHelper.getServerGameTime();
        Long lastTeleportGameTime = this.lastTeleportGameTime.getOrDefault(entity, 0L);
        if (currGameTime - lastTeleportGameTime < 5) {
            return;
        }
        this.lastTeleportGameTime.put(entity, currGameTime);
        
        if (entity.isPassenger() || !entity.getPassengers().isEmpty()) {
            return;
        }
        
        Vec3d newPos = portal.applyTransformationToPoint(entity.getPositionVec());
        
        if (portal.dimensionTo != entity.dimension) {
            changeEntityDimension(entity, portal.dimensionTo, newPos);
        }
        
        entity.setPosition(
            newPos.x, newPos.y, newPos.z
        );
    
        portal.onEntityTeleportedOnServer(entity);
    }
    
    /**
     * {@link Entity#changeDimension(DimensionType)}
     */
    private void changeEntityDimension(
        Entity entity,
        DimensionType toDimension,
        Vec3d destination
    ) {
        ServerWorld fromWorld = (ServerWorld) entity.world;
        ServerWorld toWorld = McHelper.getServer().getWorld(toDimension);
        entity.detach();
    
        Stream<ServerPlayerEntity> watchingPlayers = McHelper.getEntitiesNearby(
            entity,
            ServerPlayerEntity.class,
            128
        );
        
        fromWorld.removeEntity(entity);
        entity.removed = false;
        
        entity.posX = destination.x;
        entity.posY = destination.y;
        entity.posZ = destination.z;
        
        entity.world = toWorld;
        entity.dimension = toDimension;
    
        //this is important
        toWorld.func_217460_e(entity);
    }
}
