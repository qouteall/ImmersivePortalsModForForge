package com.qouteall.immersive_portals.mixin.common.entity_sync;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.chunk_loading.EntitySync;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerPlayNetHandler.class)
public class MixinServerPlayNetworkHandler_E {
    @ModifyVariable(
        method = "Lnet/minecraft/network/play/ServerPlayNetHandler;sendPacket(Lnet/minecraft/network/IPacket;Lio/netty/util/concurrent/GenericFutureListener;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private IPacket modifyPacket(IPacket originalPacket) {
        if (EntitySync.getForceRedirectDimension() == null) {
            return originalPacket;
        }
        
        return MyNetwork.createRedirectedMessage(EntitySync.getForceRedirectDimension(), originalPacket);
    }
}
