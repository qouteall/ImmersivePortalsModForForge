package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.exposer.IEPlayerMoveC2SPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(CPlayerPacket.RotationPacket.class)
public class MixinPlayerMoveC2SPacketLookOnly {
    @OnlyIn(Dist.CLIENT)
    @Inject(
        method = "Lnet/minecraft/server/network/packet/PlayerMoveC2SPacket$LookOnly;<init>(FFZ)V",
        at = @At("RETURN")
    )
    private void onConstruct(float float_1, float float_2, boolean boolean_1, CallbackInfo ci) {
        DimensionType dimension = Minecraft.getInstance().player.dimension;
        ((IEPlayerMoveC2SPacket) this).setPlayerDimension(dimension);
        assert dimension == Minecraft.getInstance().world.dimension.getType();
    }
    
    
}
