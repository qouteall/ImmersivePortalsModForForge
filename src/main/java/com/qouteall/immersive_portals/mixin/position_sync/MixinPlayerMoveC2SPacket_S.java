package com.qouteall.immersive_portals.mixin.position_sync;

import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CPlayerPacket.class)
public class MixinPlayerMoveC2SPacket_S implements IEPlayerMoveC2SPacket {
    private RegistryKey<World> playerDimension;
    
    @Inject(
        method = "Lnet/minecraft/network/play/client/CPlayerPacket;readPacketData(Lnet/minecraft/network/PacketBuffer;)V",
        at = @At("HEAD")
    )
    private void onRead(PacketBuffer buf, CallbackInfo ci) {
        try {
            playerDimension = DimId.readWorldId(buf, false);
        }
        catch (IndexOutOfBoundsException e) {
            //nothing
        }
    }
    
    @Override
    public RegistryKey<World> getPlayerDimension() {
        return playerDimension;
    }
    
    @Override
    public void setPlayerDimension(RegistryKey<World> dim) {
        playerDimension = dim;
    }
    
    
}
