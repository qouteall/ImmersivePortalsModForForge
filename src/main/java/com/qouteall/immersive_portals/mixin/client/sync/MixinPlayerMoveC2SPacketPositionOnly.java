package com.qouteall.immersive_portals.mixin.client.sync;

import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(CPlayerPacket.PositionPacket.class)
public class MixinPlayerMoveC2SPacketPositionOnly {
    @OnlyIn(Dist.CLIENT)
    @Inject(
        method = "<init>(DDDZ)V",
        at = @At("RETURN")
    )
    private void onConstruct1(
        double double_1,
        double double_2,
        double double_3,
        boolean boolean_1,
        CallbackInfo ci
    ) {
        RegistryKey<World> dimension = Minecraft.getInstance().player.world.func_234923_W_();
        ((IEPlayerMoveC2SPacket) this).setPlayerDimension(dimension);
        assert dimension == Minecraft.getInstance().world.func_234923_W_();
    }
    
}
