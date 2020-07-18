package com.qouteall.immersive_portals.mixin_client.sync;

import com.qouteall.immersive_portals.ducks.IEPlayerMoveC2SPacket;
import net.minecraft.client.Minecraft;
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
}
