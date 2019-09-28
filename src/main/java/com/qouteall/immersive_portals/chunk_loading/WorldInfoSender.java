package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SChangeGameStatePacket;
import net.minecraft.network.play.server.SUpdateTimePacket;
import net.minecraft.world.GameRules;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;

public class WorldInfoSender {
    public static void init() {
        ModMain.postServerTickSignal.connect(() -> {
            if (Helper.getServerGameTime() % 100 == 42) {
                for (ServerPlayerEntity player : Helper.getCopiedPlayerList()) {
                    if (player.dimension != DimensionType.OVERWORLD) {
                        sendWorldInfo(
                            player,
                            Helper.getServer().getWorld(DimensionType.OVERWORLD)
                        );
                    }
                }
            }
        });
    }
    
    //send the daytime and weather info to player when player is in nether
    public static void sendWorldInfo(ServerPlayerEntity player, ServerWorld world) {
        DimensionType remoteDimension = world.dimension.getType();
        
        player.connection.sendPacket(
            MyNetwork.createRedirectedMessage(
                remoteDimension,
                new SUpdateTimePacket(
                    world.getTime(),
                    world.getTimeOfDay(),
                    world.getGameRules().getBoolean(
                        GameRules.DO_DAYLIGHT_CYCLE
                    )
                )
            )
        );
    
        /**{@link net.minecraft.client.network.ClientPlayNetworkHandler#onGameStateChange(GameStateChangeS2CPacket)}*/
        if (world.isRaining()) {
            player.connection.sendPacket(
                MyNetwork.createRedirectedMessage(
                    remoteDimension,
                    new SChangeGameStatePacket(1, 0.0F)
                )
            );
            player.connection.sendPacket(
                MyNetwork.createRedirectedMessage(
                    remoteDimension,
                    new SChangeGameStatePacket(7, world.getRainGradient(1.0F))
                )
            );
            player.connection.sendPacket(
                MyNetwork.createRedirectedMessage(
                    remoteDimension,
                    new SChangeGameStatePacket(8, world.getThunderGradient(1.0F))
                )
            );
        }
    }
}
