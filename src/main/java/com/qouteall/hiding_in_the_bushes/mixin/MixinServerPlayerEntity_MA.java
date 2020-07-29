package com.qouteall.hiding_in_the_bushes.mixin;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.chunk_loading.ChunkDataSyncManager;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity_MA {
    
    @Inject(method = "changeDimension", at = @At("HEAD"), remap = false)
    private void onChangeDimensionByVanilla(
        ServerWorld p_changeDimension_1_,
        ITeleporter p_changeDimension_2_,
        CallbackInfoReturnable<Entity> cir
    ) {
        ServerPlayerEntity oldPlayer = (ServerPlayerEntity) (Object) this;
        Global.chunkDataSyncManager.onPlayerRespawn(oldPlayer);
        
    }
    
    @Inject(
        method = "Lnet/minecraft/entity/player/ServerPlayerEntity;teleport(Lnet/minecraft/world/server/ServerWorld;DDDFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/server/ServerWorld;removePlayer(Lnet/minecraft/entity/player/ServerPlayerEntity;Z)V"
        )
    )
    private void onForgeTeleport(
        ServerWorld serverWorld,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        CallbackInfo ci
    ) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        
        //fix issue with good nights sleep
        player.clearBedPosition();
    
        Global.chunkDataSyncManager.onPlayerRespawn(player);
        
        GlobalPortalStorage.onPlayerLoggedIn(player);
    }
}
