package com.qouteall.immersive_portals.mixin.common.position_sync;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import com.qouteall.immersive_portals.ducks.IEPlayerPositionLookS2CPacket;
import com.qouteall.immersive_portals.ducks.IEServerPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CConfirmTeleportPacket;
import net.minecraft.network.play.client.CMoveVehiclePacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = ServerPlayNetHandler.class, priority = 900)
public abstract class MixinServerPlayNetworkHandler implements IEServerPlayNetworkHandler {
    @Shadow
    public ServerPlayerEntity player;
    @Shadow
    private Vector3d targetPos;
    @Shadow
    private int teleportId;
    @Shadow
    private int lastPositionUpdate;
    @Shadow
    private int networkTickCount;
    
    @Shadow
    private boolean vehicleFloating;
    
    @Shadow
    private double lowestRiddenX1;
    
    @Shadow
    private double lowestRiddenY1;
    
    @Shadow
    private double lowestRiddenZ1;
    
    @Shadow
    protected abstract boolean func_217264_d();
    
    @Shadow
    protected abstract boolean func_241163_a_(IWorldReader worldView, AxisAlignedBB box);
    
    //do not process move packet when client dimension and server dimension are not synced
    @Inject(
        method = "Lnet/minecraft/network/play/ServerPlayNetHandler;processPlayer(Lnet/minecraft/network/play/client/CPlayerPacket;)V",
        at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/IPacket;Lnet/minecraft/network/INetHandler;Lnet/minecraft/world/server/ServerWorld;)V"
        ),
        cancellable = true
    )
    private void onProcessMovePacket(CPlayerPacket packet, CallbackInfo ci) {
        RegistryKey<World> packetDimension = ((IEPlayerMoveC2SPacket) packet).getPlayerDimension();
        
        if (packetDimension == null) {
            Helper.err("Player move packet is missing dimension info. Maybe the player client doesn't have IP");
            ModMain.serverTaskList.addTask(() -> {
                player.connection.disconnect(new StringTextComponent(
                    "The client does not have Immersive Portals mod"
                ));
                return true;
            });
            return;
        }
        
        if (Global.serverTeleportationManager.isJustTeleported(player, 100)) {
            cancelTeleportRequest();
        }
        
        if (player.world.func_234923_W_() != packetDimension) {
            ModMain.serverTaskList.addTask(() -> {
                Global.serverTeleportationManager.acceptDubiousMovePacket(
                    player, packet, packetDimension
                );
                return true;
            });
            ci.cancel();
        }
    }
    
    @Redirect(
        method = "Lnet/minecraft/network/play/ServerPlayNetHandler;processPlayer(Lnet/minecraft/network/play/client/CPlayerPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/play/ServerPlayNetHandler;func_217264_d()Z"
        )
    )
    private boolean redirectIsServerOwnerOnPlayerMove(ServerPlayNetHandler serverPlayNetworkHandler) {
        if (shouldAcceptDubiousMovement(player)) {
            return true;
        }
        return func_217264_d();
    }
    
    @Redirect(
        method = "Lnet/minecraft/network/play/ServerPlayNetHandler;processPlayer(Lnet/minecraft/network/play/client/CPlayerPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/ServerPlayerEntity;isInvulnerableDimensionChange()Z"
        ),
        require = 0 // don't crash with carpet
    )
    private boolean redirectIsInTeleportationState(ServerPlayerEntity player) {
        if (shouldAcceptDubiousMovement(player)) {
            return true;
        }
        return player.isInvulnerableDimensionChange();
    }
    
    /**
     * make PlayerPositionLookS2CPacket contain dimension data
     *
     * @author qouteall
     */
    @Overwrite
    public void setPlayerLocation(
        double destX,
        double destY,
        double destZ,
        float destYaw,
        float destPitch,
        Set<SPlayerPositionLookPacket.Flags> updates
    ) {
        if (player.removed) {
            ModMain.serverTaskList.addTask(() -> {
                doSendTeleportRequest(destX, destY, destZ, destYaw, destPitch, updates);
                return true;
            });
        }
        else {
            doSendTeleportRequest(destX, destY, destZ, destYaw, destPitch, updates);
        }
    }
    
    private void doSendTeleportRequest(
        double destX, double destY, double destZ, float destYaw, float destPitch,
        Set<SPlayerPositionLookPacket.Flags> updates
    ) {
        if (Global.teleportationDebugEnabled) {
            new Throwable().printStackTrace();
            Helper.log(String.format("request teleport %s %s (%d %d %d)->(%d %d %d)",
                player.getName().getUnformattedComponentText(),
                player.world.func_234923_W_(),
                (int) player.getPosX(), (int) player.getPosY(), (int) player.getPosZ(),
                (int) destX, (int) destY, (int) destZ
            ));
        }
        
        double currX = updates.contains(SPlayerPositionLookPacket.Flags.X) ? this.player.getPosX() : 0.0D;
        double currY = updates.contains(SPlayerPositionLookPacket.Flags.Y) ? this.player.getPosY() : 0.0D;
        double currZ = updates.contains(SPlayerPositionLookPacket.Flags.Z) ? this.player.getPosZ() : 0.0D;
        float currYaw = updates.contains(SPlayerPositionLookPacket.Flags.Y_ROT) ? this.player.rotationYaw : 0.0F;
        float currPitch = updates.contains(SPlayerPositionLookPacket.Flags.X_ROT) ? this.player.rotationPitch : 0.0F;
        
        if (!Global.serverTeleportationManager.isJustTeleported(player, 100)) {
            this.targetPos = new Vector3d(destX, destY, destZ);
        }
        
        if (++this.teleportId == Integer.MAX_VALUE) {
            this.teleportId = 0;
        }
        
        this.lastPositionUpdate = this.networkTickCount;
        this.player.setPositionAndRotation(destX, destY, destZ, destYaw, destPitch);
        SPlayerPositionLookPacket lookPacket = new SPlayerPositionLookPacket(
            destX - currX,
            destY - currY,
            destZ - currZ,
            destYaw - currYaw,
            destPitch - currPitch,
            updates,
            this.teleportId
        );
        //noinspection ConstantConditions
        ((IEPlayerPositionLookS2CPacket) lookPacket).setPlayerDimension(player.world.func_234923_W_());
        this.player.connection.sendPacket(lookPacket);
    }
    
    //server will check the collision when receiving position packet from client
    //we treat collision specially when player is halfway through a portal
    //"isPlayerNotCollidingWithBlocks" is wrong now
    @Redirect(
        method = "Lnet/minecraft/network/play/ServerPlayNetHandler;processPlayer(Lnet/minecraft/network/play/client/CPlayerPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/play/ServerPlayNetHandler;func_241163_a_(Lnet/minecraft/world/IWorldReader;Lnet/minecraft/util/math/AxisAlignedBB;)Z"
        )
    )
    private boolean onCheckPlayerCollision(
        ServerPlayNetHandler serverPlayNetworkHandler,
        IWorldReader worldView,
        AxisAlignedBB box
    ) {
        if (Global.serverTeleportationManager.isJustTeleported(player, 100)) {
            return false;
        }
        if (((IEEntity) player).getCollidingPortal() != null) {
            return false;
        }
        boolean portalsNearby = McHelper.getNearbyPortals(
            player,
            16
        ).findAny().isPresent();
        if (portalsNearby) {
            return false;
        }
        return func_241163_a_(worldView, box);
    }
    
    @Inject(
        method = "Lnet/minecraft/network/play/ServerPlayNetHandler;processConfirmTeleport(Lnet/minecraft/network/play/client/CConfirmTeleportPacket;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onOnTeleportConfirm(CConfirmTeleportPacket packet, CallbackInfo ci) {
        if (targetPos == null) {
            ci.cancel();
        }
    }
    
    //do not reject move when player is riding and entering portal
    //the client packet is not validated (validating it needs dimension info in packet)
    @Inject(
        method = "Lnet/minecraft/network/play/ServerPlayNetHandler;processVehicleMove(Lnet/minecraft/network/play/client/CMoveVehiclePacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/play/ServerPlayNetHandler;isMoveVehiclePacketInvalid(Lnet/minecraft/network/play/client/CMoveVehiclePacket;)Z"
        ),
        cancellable = true
    )
    private void onOnVehicleMove(CMoveVehiclePacket packet, CallbackInfo ci) {
        if (Global.serverTeleportationManager.isJustTeleported(player, 40)) {
            Entity entity = this.player.getLowestRidingEntity();
            
            if (entity != player) {
                double currX = entity.getPosX();
                double currY = entity.getPosY();
                double currZ = entity.getPosZ();
                
                double newX = packet.getX();
                double newY = packet.getY();
                double newZ = packet.getZ();
                
                if (entity.getPositionVec().squareDistanceTo(
                    newX, newY, newZ
                ) < 256) {
                    float yaw = packet.getYaw();
                    float pitch = packet.getPitch();
                    
                    entity.setPositionAndRotation(newX, newY, newZ, yaw, pitch);
                    
                    this.player.getServerWorld().getChunkProvider().updatePlayerPosition(this.player);
                    
                    vehicleFloating = true;
                    lowestRiddenX1 = entity.getPosX();
                    lowestRiddenY1 = entity.getPosY();
                    lowestRiddenZ1 = entity.getPosZ();
                }
            }
            
            ci.cancel();
        }
    }
    
    private static boolean shouldAcceptDubiousMovement(ServerPlayerEntity player) {
        if (Global.serverTeleportationManager.isJustTeleported(player, 100)) {
            return true;
        }
        if (Global.looseMovementCheck) {
            return true;
        }
        if (((IEEntity) player).getCollidingPortal() != null) {
            return true;
        }
        boolean portalsNearby = McHelper.getNearbyPortals(player, 16).findFirst().isPresent();
        if (portalsNearby) {
            return true;
        }
        return false;
    }
    
    @Override
    public void cancelTeleportRequest() {
        targetPos = null;
    }
}
