package com.qouteall.immersive_portals.mixin.client.sync;

import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import com.qouteall.immersive_portals.network.NetworkAdapt;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CPlayerPacket.class)
public class MixinPlayerMoveC2SPacket_C {
    @Inject(
        method = "<init>(Z)V",
        at = @At("RETURN")
    )
    private void onConstruct(boolean boolean_1, CallbackInfo ci) {
        RegistryKey<World> dimension = Minecraft.getInstance().player.world.func_234923_W_();
        ((IEPlayerMoveC2SPacket) this).setPlayerDimension(dimension);
        assert dimension == Minecraft.getInstance().world.func_234923_W_();
    }
    
    @Inject(
        method = "Lnet/minecraft/network/play/client/CPlayerPacket;writePacketData(Lnet/minecraft/network/PacketBuffer;)V",
        at = @At("HEAD")
    )
    private void onWrite(PacketBuffer buf, CallbackInfo ci) {
        if (NetworkAdapt.doesServerHasIP()) {
            RegistryKey<World> playerDimension = ((IEPlayerMoveC2SPacket) this).getPlayerDimension();
            DimId.writeWorldId(buf, playerDimension, true);
        }
    }
}
