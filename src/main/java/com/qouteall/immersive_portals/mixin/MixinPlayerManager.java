package com.qouteall.immersive_portals.mixin;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerList.class)
public class MixinPlayerManager {
    @Shadow
    @Final
    private List<ServerPlayerEntity> players;
    
    @Shadow
    @Final
    private MinecraftServer server;
    
    @Inject(
        method = "Lnet/minecraft/server/management/PlayerList;initializeConnectionToPlayer(Lnet/minecraft/network/NetworkManager;Lnet/minecraft/entity/player/ServerPlayerEntity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;<init>(ILnet/minecraft/world/GameMode;Lnet/minecraft/world/GameMode;JZLjava/util/Set;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Lnet/minecraft/util/registry/RegistryKey;Lnet/minecraft/util/registry/RegistryKey;IIZZZZ)V"
        )
    )
    private void onConnectionEstablished(
        NetworkManager connection,
        ServerPlayerEntity player,
        CallbackInfo ci
    ) {
        player.connection.sendPacket(MyNetwork.createDimSync());
    }
    
    @Inject(method = "Lnet/minecraft/server/management/PlayerList;sendWorldInfo(Lnet/minecraft/entity/player/ServerPlayerEntity;Lnet/minecraft/world/server/ServerWorld;)V", at = @At("RETURN"))
    private void onSendWorldInfo(ServerPlayerEntity player, ServerWorld world, CallbackInfo ci) {
        if (!Global.serverTeleportationManager.isFiringMyChangeDimensionEvent) {
            GlobalPortalStorage.onPlayerLoggedIn(player);
        }
    }
    
    //sometimes the server side player dimension is not same as client
    //so redirect it
    @Inject(
        method = "Lnet/minecraft/server/management/PlayerList;func_232642_a_(Lnet/minecraft/network/IPacket;Lnet/minecraft/util/RegistryKey;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    public void sendToDimension(IPacket<?> packet, RegistryKey<World> dimension, CallbackInfo ci) {
        players.stream()
            .filter(player -> player.world.func_234923_W_() == dimension)
            .forEach(player -> player.connection.sendPacket(
                MyNetwork.createRedirectedMessage(
                    dimension,
                    packet
                )
            ));
        ci.cancel();
    }
    
//    /**
//     * @author qouteall
//     */
//    @Overwrite
//    public void sendWorldInfo(ServerPlayerEntity player, ServerWorld world) {
//        WorldBorder worldBorder = this.server.getOverworld().getWorldBorder();
//        RegistryKey<World> dimension = world.getRegistryKey();
//        player.networkHandler.sendPacket(
//            MyNetwork.createRedirectedMessage(
//                dimension,
//                new WorldBorderS2CPacket(worldBorder, WorldBorderS2CPacket.Type.INITIALIZE)
//            )
//        );
//        player.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
//            dimension, new WorldTimeUpdateS2CPacket(
//                world.getTime(),
//                world.getTimeOfDay(),
//                world.getGameRules().getBoolean(
//                    GameRules.DO_DAYLIGHT_CYCLE)
//            ))
//        );
//        player.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
//            dimension, new PlayerSpawnPositionS2CPacket(world.getSpawnPos())
//        ));
//        if (world.isRaining()) {
//            player.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
//                dimension, new GameStateChangeS2CPacket(
//                    GameStateChangeS2CPacket.RAIN_STARTED,
//                    0.0F
//                )
//            ));
//            player.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
//                dimension, new GameStateChangeS2CPacket(
//                    GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED,
//                    world.getRainGradient(1.0F)
//                )
//            ));
//            player.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
//                dimension, new GameStateChangeS2CPacket(
//                    GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED,
//                    world.getThunderGradient(1.0F)
//                )
//            ));
//        }
//
//    }
}
