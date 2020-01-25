package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(value = CPlayerPacket.PositionPacket.class)
public class MixinPlayerMoveC2SPacket_Position {
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
        DimensionType dimension = Minecraft.getInstance().player.dimension;
        ((IEPlayerMoveC2SPacket) this).setPlayerDimension(dimension);
        assert dimension == Minecraft.getInstance().world.dimension.getType();
    }
    
}
