package com.qouteall.immersive_portals.mixin.position_sync;

import com.qouteall.immersive_portals.ducks.IEPlayerPositionLookS2CPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SPlayerPositionLookPacket.class)
public class MixinPlayerPositionLookS2CPacket implements IEPlayerPositionLookS2CPacket {
    private DimensionType playerDimension;
    
    @Override
    public DimensionType getPlayerDimension() {
        return playerDimension;
    }
    
    @Override
    public void setPlayerDimension(DimensionType dimension) {
        playerDimension = dimension;
    }
    
    @Inject(
        method = "readPacketData",
        at = @At("HEAD")
    )
    private void onRead(PacketBuffer packetByteBuf_1, CallbackInfo ci) {
        playerDimension = DimensionType.getById(packetByteBuf_1.readInt());
    }
    
    @Inject(
        method = "writePacketData",
        at = @At("HEAD")
    )
    private void onWrite(PacketBuffer packetByteBuf_1, CallbackInfo ci) {
        packetByteBuf_1.writeInt(playerDimension.getId());
    }
}
