package com.qouteall.immersive_portals.mixin.position_sync;

import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CPlayerPacket.class)
public class MixinPlayerMoveC2SPacket_S implements IEPlayerMoveC2SPacket {
    private DimensionType playerDimension;
    
    @Inject(
        method = "Lnet/minecraft/network/play/client/CPlayerPacket;readPacketData(Lnet/minecraft/network/PacketBuffer;)V",
        at = @At("HEAD")
    )
    private void onRead(PacketBuffer packetByteBuf_1, CallbackInfo ci) {
        playerDimension = DimensionType.getById(packetByteBuf_1.readInt());
    }
    
    @Inject(
        method = "Lnet/minecraft/network/play/client/CPlayerPacket;writePacketData(Lnet/minecraft/network/PacketBuffer;)V",
        at = @At("HEAD")
    )
    private void onWrite(PacketBuffer packetByteBuf_1, CallbackInfo ci) {
        packetByteBuf_1.writeInt(playerDimension.getId());
    }
    
    @Override
    public DimensionType getPlayerDimension() {
        return playerDimension;
    }
    
    @Override
    public void setPlayerDimension(DimensionType dim) {
        playerDimension = dim;
    }
    
    
}
