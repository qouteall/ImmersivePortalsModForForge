package com.qouteall.hiding_in_the_bushes.mixin;

import com.qouteall.hiding_in_the_bushes.DimensionSyncManager;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerList.class)
public class MixinPlayerManager_MA {
    @Shadow
    @Final
    private List<ServerPlayerEntity> players;
    
    @Inject(
        method = "func_232644_a_",
        at = @At("HEAD")
    )
    private void onPlayerRespawn(
        ServerPlayerEntity p_232644_1_, boolean p_232644_2_,
        CallbackInfoReturnable<ServerPlayerEntity> cir
    ) {
        Global.chunkDataSyncManager.onPlayerRespawn(p_232644_1_);
    }
}
