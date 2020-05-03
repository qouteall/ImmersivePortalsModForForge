package com.qouteall.immersive_portals.teleportation;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEServerPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEServerPlayerEntity;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Stream;

public class ServerTeleportationManager {
    private Set<ServerPlayerEntity> teleportingEntities = new HashSet<>();
    private WeakHashMap<Entity, Long> lastTeleportGameTime = new WeakHashMap<>();
    public boolean isFiringMyChangeDimensionEvent = false;
    public final WeakHashMap<ServerPlayerEntity, Tuple<DimensionType, Vec3d>> lastPosition =
        new WeakHashMap<>();
    
    public ServerTeleportationManager() {
        ModMain.postServerTickSignal.connectWithWeakRef(this, ServerTeleportationManager::tick);
        Portal.serverPortalTickSignal.connectWithWeakRef(
            this, (this_, portal) ->
                getEntitiesToTeleport(portal).forEach(entity -> {
                    tryToTeleportRegularEntity(portal, entity);
                })
        );
    }
    
    public void tryToTeleportRegularEntity(Portal portal, Entity entity) {
        if (entity instanceof ServerPlayerEntity) {
            return;
        }
        if (entity.getRidingEntity() != null || doesEntityClutterContainPlayer(entity)) {
            return;
        }
        //a new born entity may have last tick pos 0 0 0
        double motion = McHelper.lastTickPosOf(entity).squareDistanceTo(entity.getPositionVec());
        if (motion > 2) {
            return;
        }
        ModMain.serverTaskList.addTask(() -> {
            teleportRegularEntity(entity, portal);
            return true;
        });
    }
    
    private static Stream<Entity> getEntitiesToTeleport(Portal portal) {
        return portal.world.getEntitiesWithinAABB(
            Entity.class,
            portal.getBoundingBox().grow(2),
            e -> true
        ).stream().filter(
            e -> !(e instanceof Portal)
        ).filter(
            portal::shouldEntityTeleport
        );
    }
    
    public void onPlayerTeleportedInClient(
        ServerPlayerEntity player,
        DimensionType dimensionBefore,
        Vec3d oldEyePos,
        UUID portalId
    ) {
        recordLastPosition(player);
        
        Portal portal = findPortal(dimensionBefore, portalId);
        lastTeleportGameTime.put(player, McHelper.getServerGameTime());
        
        if (canPlayerTeleport(player, dimensionBefore, oldEyePos, portal)) {
            if (isTeleporting(player)) {
                Helper.err(player.toString() + "is teleporting frequently");
            }
            
            DimensionType dimensionTo = portal.dimensionTo;
            Vec3d newEyePos = portal.transformPoint(oldEyePos);
            
            teleportPlayer(player, dimensionTo, newEyePos);
            
            portal.onEntityTeleportedOnServer(player);
        }
        else {
            Helper.err(String.format(
                "Player cannot teleport through portal %s %s %s %s",
                player.getName().getUnformattedComponentText(),
                player.dimension,
                player.getPositionVec(),
                portal
            ));
        }
    }
    
    private Portal findPortal(DimensionType dimensionBefore, UUID portalId) {
        ServerWorld originalWorld = McHelper.getServer().getWorld(dimensionBefore);
        Entity portalEntity = originalWorld.getEntityByUuid(portalId);
        if (portalEntity == null) {
            portalEntity = GlobalPortalStorage.get(originalWorld).data
                .stream().filter(
                    p -> p.getUniqueID().equals(portalId)
                ).findFirst().orElse(null);
        }
        if (portalEntity == null) {
            return null;
        }
        if (portalEntity instanceof Portal) {
            return ((Portal) portalEntity);
        }
        return null;
    }
    
    public void recordLastPosition(ServerPlayerEntity player) {
        lastPosition.put(
            player,
            new Tuple<>(player.dimension, player.getPositionVec())
        );
    }
    
    private boolean canPlayerTeleport(
        ServerPlayerEntity player,
        DimensionType dimensionBefore,
        Vec3d posBefore,
        Entity portalEntity
    ) {
        if (player.getRidingEntity() != null) {
            return true;
        }
        return canPlayerReachPos(player, dimensionBefore, posBefore) &&
            portalEntity instanceof Portal &&
            ((Portal) portalEntity).getDistanceToPlane(posBefore) < 20;
    }
    
    public static boolean canPlayerReachPos(
        ServerPlayerEntity player,
        DimensionType dimension,
        Vec3d pos
    ) {
        Vec3d playerPos = player.getPositionVec();
        if (player.dimension == dimension) {
            if (playerPos.squareDistanceTo(pos) < 256) {
                return true;
            }
        }
        return McHelper.getServerPortalsNearby(player, 20)
            .filter(portal -> portal.dimensionTo == dimension)
            .map(portal -> portal.transformPointRough(playerPos))
            .anyMatch(mappedPos -> mappedPos.squareDistanceTo(pos) < 256);
    }
    
    public void teleportPlayer(
        ServerPlayerEntity player,
        DimensionType dimensionTo,
        Vec3d newEyePos
    ) {
        ServerWorld fromWorld = (ServerWorld) player.world;
        ServerWorld toWorld = McHelper.getServer().getWorld(dimensionTo);
        
        if (player.dimension == dimensionTo) {
            McHelper.setEyePos(player, newEyePos, newEyePos);
            McHelper.updateBoundingBox(player);
        }
        else {
            changePlayerDimension(player, fromWorld, toWorld, newEyePos);
            ((IEServerPlayNetworkHandler) player.connection).cancelTeleportRequest();
        }
        
        McHelper.adjustVehicle(player);
        player.connection.captureCurrentPosition();
    }
    
    public void invokeTpmeCommand(
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
            sendPositionConfirmMessage(player);
        }
        
        player.connection.setPlayerLocation(
            newPos.x,
            newPos.y,
            newPos.z,
            player.rotationYaw,
            player.rotationPitch
        );
        player.connection.captureCurrentPosition();
        ((IEServerPlayNetworkHandler) player.connection).cancelTeleportRequest();
        
    }
    
    /**
     * {@link ServerPlayerEntity#changeDimension(DimensionType)}
     */
    private void changePlayerDimension(
        ServerPlayerEntity player,
        ServerWorld fromWorld,
        ServerWorld toWorld,
        Vec3d newEyePos
    ) {
        teleportingEntities.add(player);
        
        Entity vehicle = player.getRidingEntity();
        if (vehicle != null) {
            ((IEServerPlayerEntity) player).stopRidingWithoutTeleportRequest();
        }
        
        BlockPos oldPos = player.getPosition();
        
        O_O.segregateServerPlayer(fromWorld, player);
        
        McHelper.setEyePos(player, newEyePos, newEyePos);
        McHelper.updateBoundingBox(player);
        
        player.world = toWorld;
        player.dimension = toWorld.dimension.getType();
        toWorld.func_217447_b(player);
        
        toWorld.chunkCheck(player);
        
        player.interactionManager.setWorld(toWorld);
        
        if (vehicle != null) {
            Vec3d vehiclePos = new Vec3d(
                newEyePos.x,
                McHelper.getVehicleY(vehicle, player),
                newEyePos.z
            );
            changeEntityDimension(
                vehicle,
                toWorld.dimension.getType(),
                vehiclePos.add(0, vehicle.getEyeHeight(), 0)
            );
            ((IEServerPlayerEntity) player).startRidingWithoutTeleportRequest(vehicle);
            McHelper.adjustVehicle(player);
        }
        
        Helper.log(String.format(
            "%s :: (%s %s %s %s)->(%s %s %s %s)",
            player.getName().getUnformattedComponentText(),
            fromWorld.dimension.getType(),
            oldPos.getX(), oldPos.getY(), oldPos.getZ(),
            toWorld.dimension.getType(),
            (int) player.getPosX(), (int) player.getPosY(), (int) player.getPosZ()
        ));
        
        O_O.onPlayerTravelOnServer(
            player,
            fromWorld.dimension.getType(),
            toWorld.dimension.getType()
        );
        
        //this is used for the advancement of "we need to go deeper"
        //and the advancement of travelling for long distance through nether
        if (toWorld.dimension.getType() == DimensionType.THE_NETHER) {
            //this is used for
            ((IEServerPlayerEntity) player).setEnteredNetherPos(player.getPositionVec());
        }
        ((IEServerPlayerEntity) player).updateDimensionTravelAdvancements(fromWorld);
        
        
        GlobalPortalStorage.onPlayerLoggedIn(player);
    }
    
    private void sendPositionConfirmMessage(ServerPlayerEntity player) {
        IPacket packet = MyNetwork.createStcDimensionConfirm(
            player.dimension,
            player.getPositionVec()
        );
        
        player.connection.sendPacket(packet);
    }
    
    private void tick() {
        teleportingEntities = new HashSet<>();
        long tickTimeNow = McHelper.getServerGameTime();
        ArrayList<ServerPlayerEntity> copiedPlayerList =
            McHelper.getCopiedPlayerList();
        if (tickTimeNow % 10 == 7) {
            for (ServerPlayerEntity player : copiedPlayerList) {
                if (!player.queuedEndExit) {
                    Long lastTeleportGameTime =
                        this.lastTeleportGameTime.getOrDefault(player, 0L);
                    if (tickTimeNow - lastTeleportGameTime > 60) {
                        sendPositionConfirmMessage(player);
                        
                        //for vanilla nether portal cooldown to work normally
                        player.clearInvulnerableDimensionChange();
                    }
                    else {
                        ((IEServerPlayNetworkHandler) player.connection).cancelTeleportRequest();
                    }
                }
            }
        }
        copiedPlayerList.forEach(player -> {
            McHelper.getServerEntitiesNearbyWithoutLoadingChunk(
                player.world,
                player.getPositionVec(),
                Entity.class,
                32
            ).filter(
                entity -> !(entity instanceof ServerPlayerEntity)
            ).forEach(entity -> {
                McHelper.getGlobalPortals(entity.world).stream()
                    .filter(
                        globalPortal -> globalPortal.shouldEntityTeleport(entity)
                    )
                    .findFirst()
                    .ifPresent(
                        globalPortal -> tryToTeleportRegularEntity(globalPortal, entity)
                    );
            });
        });
    }
    
    public boolean isTeleporting(ServerPlayerEntity entity) {
        return teleportingEntities.contains(entity);
    }
    
    private void teleportRegularEntity(Entity entity, Portal portal) {
        assert entity.dimension == portal.dimension;
        assert !(entity instanceof ServerPlayerEntity);
        
        long currGameTime = McHelper.getServerGameTime();
        Long lastTeleportGameTime = this.lastTeleportGameTime.getOrDefault(entity, 0L);
        if (currGameTime - lastTeleportGameTime < 3) {
            return;
        }
        this.lastTeleportGameTime.put(entity, currGameTime);
        
        if (entity.isPassenger() || doesEntityClutterContainPlayer(entity)) {
            return;
        }
    
        Vec3d velocity = entity.getMotion();
        
        List<Entity> passengerList = entity.getPassengers();
        
        Vec3d newEyePos = portal.transformPoint(McHelper.getEyePos(entity));
        
        if (portal.dimensionTo != entity.dimension) {
            changeEntityDimension(entity, portal.dimensionTo, newEyePos);
            for (Entity passenger : passengerList) {
                changeEntityDimension(passenger, portal.dimensionTo, newEyePos);
            }
            for (Entity passenger : passengerList) {
                passenger.startRiding(entity, true);
            }
        }
        
        McHelper.setEyePos(
            entity, newEyePos, newEyePos
        );
        McHelper.updateBoundingBox(entity);
        
        entity.setMotion(portal.transformLocalVec(velocity));
        
        portal.onEntityTeleportedOnServer(entity);
    }
    
    /**
     * {@link Entity#changeDimension(DimensionType)}
     */
    public void changeEntityDimension(
        Entity entity,
        DimensionType toDimension,
        Vec3d newEyePos
    ) {
        ServerWorld fromWorld = (ServerWorld) entity.world;
        ServerWorld toWorld = McHelper.getServer().getWorld(toDimension);
        entity.detach();
        
        O_O.segregateServerEntity(fromWorld, entity);
        
        McHelper.setEyePos(entity, newEyePos, newEyePos);
        McHelper.updateBoundingBox(entity);
        
        entity.world = toWorld;
        entity.dimension = toDimension;
        toWorld.func_217460_e(entity);
    }
    
    private boolean doesEntityClutterContainPlayer(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return true;
        }
        List<Entity> passengerList = entity.getPassengers();
        if (passengerList.isEmpty()) {
            return false;
        }
        return passengerList.stream().anyMatch(this::doesEntityClutterContainPlayer);
    }
    
    public boolean isJustTeleported(Entity entity, long valveTickTime) {
        long currGameTime = McHelper.getServerGameTime();
        Long lastTeleportGameTime = this.lastTeleportGameTime.getOrDefault(entity, 0L);
        return currGameTime - lastTeleportGameTime < valveTickTime;
    }
    
    public void acceptDubiousMovePacket(
        ServerPlayerEntity player,
        CPlayerPacket packet,
        DimensionType dimension
    ) {
        double x = packet.getX(player.getPosX());
        double y = packet.getY(player.getPosY());
        double z = packet.getZ(player.getPosZ());
        Vec3d newPos = new Vec3d(x, y, z);
        if (canPlayerReachPos(player, dimension, newPos)) {
            recordLastPosition(player);
            teleportPlayer(player, dimension, newPos);
            Helper.log(String.format("accepted dubious move packet %s %s %s %s %s %s %s",
                player.dimension, x, y, z, player.getPosX(), player.getPosY(), player.getPosZ()
            ));
        }
        else {
            Helper.log(String.format("ignored dubious move packet %s %s %s %s %s %s %s",
                player.dimension, x, y, z, player.getPosX(), player.getPosY(), player.getPosZ()
            ));
        }
    }
}
