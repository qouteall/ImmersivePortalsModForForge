package com.qouteall.immersive_portals.mixin.position_sync;

import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.ducks.IEPlayerPositionLookS2CPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SPlayerPositionLookPacket.class)
public class MixinPlayerPositionLookS2CPacket implements IEPlayerPositionLookS2CPacket {
    private RegistryKey<World> playerDimension;
    
    @Override
    public RegistryKey<World> getPlayerDimension() {
        return playerDimension;
    }
    
    @Override
    public void setPlayerDimension(RegistryKey<World> dimension) {
        playerDimension = dimension;
    }
    
    @Inject(method = "Lnet/minecraft/network/play/server/SPlayerPositionLookPacket;readPacketData(Lnet/minecraft/network/PacketBuffer;)V", at = @At("HEAD"))
    private void onRead(PacketBuffer packetByteBuf_1, CallbackInfo ci) {
        try {
            playerDimension = DimId.readWorldId(packetByteBuf_1, true);
        }
        catch (IndexOutOfBoundsException e) {
            throw new RuntimeException("The server doesn't install Immmersive Portals Mod");
        }
    }
    
    @Inject(method = "Lnet/minecraft/network/play/server/SPlayerPositionLookPacket;writePacketData(Lnet/minecraft/network/PacketBuffer;)V", at = @At("HEAD"))
    private void onWrite(PacketBuffer packetByteBuf_1, CallbackInfo ci) {
        DimId.writeWorldId(packetByteBuf_1, playerDimension, false);
    }
}
