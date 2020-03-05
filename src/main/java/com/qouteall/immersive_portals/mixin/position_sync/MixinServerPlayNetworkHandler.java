package com.qouteall.immersive_portals.mixin.position_sync;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import com.qouteall.immersive_portals.ducks.IEPlayerPositionLookS2CPacket;
import com.qouteall.immersive_portals.ducks.IEServerPlayNetworkHandler;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CConfirmTeleportPacket;
import net.minecraft.network.play.client.CMoveVehiclePacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerPlayNetHandler.class)
public abstract class MixinServerPlayNetworkHandler implements IEServerPlayNetworkHandler {
    @Shadow
    public ServerPlayerEntity player;
    @Shadow
    private Vec3d targetPos;
    @Shadow
    private int teleportId;
    @Shadow
    private int lastPositionUpdate;
    @Shadow
    private int networkTickCount;
    
    @Shadow
    protected abstract boolean func_223133_a(IWorldReader worldView_1);
    
    @Shadow
    private boolean vehicleFloating;
    
    @Shadow
    private double lowestRiddenX1;
    
    @Shadow
    private double lowestRiddenY1;
    
    @Shadow
    private double lowestRiddenZ1;
    
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
        DimensionType packetDimension = ((IEPlayerMoveC2SPacket) packet).getPlayerDimension();
    
        assert packetDimension != null;
    
        if (Global.serverTeleportationManager.isJustTeleported(player, 100)) {
            cancelTeleportRequest();
        }
    
        if (player.dimension != packetDimension) {
            Global.serverTeleportationManager.acceptDubiousMovePacket(
                player, packet, packetDimension
            );
            ci.cancel();
        }
    }
    
    /**
     * make PlayerPositionLookS2CPacket contain dimension data
     *
     * @author qouteall
     */
    @Overwrite
    public void setPlayerLocation(
        double double_1,
        double double_2,
        double double_3,
        float float_1,
        float float_2,
        Set<SPlayerPositionLookPacket.Flags> set_1
    ) {
        Helper.log(String.format("request teleport %s %s (%d %d %d)->(%d %d %d)",
            player.getName().getUnformattedComponentText(),
            player.dimension,
            (int) player.getPosX(), (int) player.getPosY(), (int) player.getPosZ(),
            (int) double_1, (int) double_2, (int) double_3
        ));
    
        double double_4 = set_1.contains(SPlayerPositionLookPacket.Flags.X) ? this.player.getPosX() : 0.0D;
        double double_5 = set_1.contains(SPlayerPositionLookPacket.Flags.Y) ? this.player.getPosY() : 0.0D;
        double double_6 = set_1.contains(SPlayerPositionLookPacket.Flags.Z) ? this.player.getPosZ() : 0.0D;
        float float_3 = set_1.contains(SPlayerPositionLookPacket.Flags.Y_ROT) ? this.player.rotationYaw : 0.0F;
        float float_4 = set_1.contains(SPlayerPositionLookPacket.Flags.X_ROT) ? this.player.rotationPitch : 0.0F;
        //this.requestedTeleportPos = new Vec3d(double_1, double_2, double_3);
        if (++this.teleportId == Integer.MAX_VALUE) {
            this.teleportId = 0;
        }
    
        this.lastPositionUpdate = this.networkTickCount;
        this.player.setPositionAndRotation(double_1, double_2, double_3, float_1, float_2);
        SPlayerPositionLookPacket packet_1 = new SPlayerPositionLookPacket(
            double_1 - double_4,
            double_2 - double_5,
            double_3 - double_6,
            float_1 - float_3,
            float_2 - float_4,
            set_1,
            this.teleportId
        );
        ((IEPlayerPositionLookS2CPacket) packet_1).setPlayerDimension(player.dimension);
        this.player.connection.sendPacket(packet_1);
    }
    
    //server will check the collision when receiving position packet from client
    //we treat collision specially when player is halfway through a portal
    @Redirect(
        method = "Lnet/minecraft/network/play/ServerPlayNetHandler;processPlayer(Lnet/minecraft/network/play/client/CPlayerPacket;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/play/ServerPlayNetHandler;func_223133_a(Lnet/minecraft/world/IWorldReader;)Z"
        )
    )
    private boolean onCheckPlayerCollision(
        ServerPlayNetHandler serverPlayNetworkHandler,
        IWorldReader worldView_1
    ) {
        if (Global.serverTeleportationManager.isJustTeleported(player, 100)) {
            return true;
        }
        boolean portalsNearby = !player.world.getEntitiesWithinAABB(
            Portal.class,
            player.getBoundingBox().grow(4),
            e -> true
        ).isEmpty();
        if (portalsNearby) {
            return true;
        }
        return func_223133_a(worldView_1);
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
    
    @Override
    public void cancelTeleportRequest() {
        targetPos = null;
    }
}
