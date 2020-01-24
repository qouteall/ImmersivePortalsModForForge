package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import com.qouteall.immersive_portals.ducks.IEPlayerPositionLookS2CPacket;
import com.qouteall.immersive_portals.ducks.IEServerPlayNetworkHandler;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CConfirmTeleportPacket;
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

@Mixin(value = ServerPlayNetHandler.class)
public abstract class MixinServerPlayNetHandler implements IEServerPlayNetworkHandler {
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
    protected abstract boolean func_223133_a(IWorldReader viewableWorld_1);
    
    //do not process move packet when client dimension and server dimension are not synced
    @Inject(
        method = "processPlayer",
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
        
        if (player.dimension != packetDimension) {
            ci.cancel();
        }
    }
    
    /**
     * make PlayerPositionLookS2CPacket contain dimension data
     *
     * @author qouteall
     * @reason
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
        Helper.log(String.format("request teleport %s in %s to %s %s %s",
            player.getName().getString(),
            player.dimension,
            double_1, double_2, double_3
        ));
        
        double double_4 = set_1.contains(SPlayerPositionLookPacket.Flags.X) ? this.player.posX : 0.0D;
        double double_5 = set_1.contains(SPlayerPositionLookPacket.Flags.Y) ? this.player.posY : 0.0D;
        double double_6 = set_1.contains(SPlayerPositionLookPacket.Flags.Z) ? this.player.posZ : 0.0D;
        float float_3 = set_1.contains(SPlayerPositionLookPacket.Flags.Y_ROT) ? this.player.rotationYaw : 0.0F;
        float float_4 = set_1.contains(SPlayerPositionLookPacket.Flags.X_ROT) ? this.player.rotationPitch : 0.0F;
        this.targetPos = new Vec3d(double_1, double_2, double_3);
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
        method = "processPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/play/ServerPlayNetHandler;func_223133_a(Lnet/minecraft/world/IWorldReader;)Z"
        )
    )
    private boolean onCheckPlayerCollision(
        ServerPlayNetHandler serverPlayNetworkHandler,
        IWorldReader world
    ) {
        boolean portalsNearby = !player.world.getEntitiesWithinAABB(
            Portal.class,
            player.getBoundingBox().grow(4),
            e -> true
        ).isEmpty();
        if (portalsNearby) {
            return true;
        }
        return func_223133_a(world);
    }
    
    @Inject(
        method = "processConfirmTeleport",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onProcessConfirmTeleport(
        CConfirmTeleportPacket packetIn,
        CallbackInfo ci
    ) {
        if (targetPos == null) {
            ci.cancel();
        }
    }
    
    @Override
    public void cancelTeleportRequest() {
        targetPos = null;
    }
}
