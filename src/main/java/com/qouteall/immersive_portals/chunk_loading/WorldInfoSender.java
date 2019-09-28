package com.qouteall.immersive_portals.chunk_loading;

import com.immersive_portals.network.NetworkMain;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SChangeGameStatePacket;
import net.minecraft.network.play.server.SUpdateTimePacket;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

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
    
        NetworkMain.sendRedirected(
            player, remoteDimension,
            new SUpdateTimePacket(
                world.getGameTime(),
                world.getDayTime(),
                world.getGameRules().getBoolean(
                    GameRules.DO_DAYLIGHT_CYCLE
                )
            )
        );
    
    
        /**{@link net.minecraft.client.network.ClientPlayNetworkHandler#onGameStateChange(GameStateChangeS2CPacket)}*/
        if (world.isRaining()) {
            NetworkMain.sendRedirected(
                player, remoteDimension,
                new SChangeGameStatePacket(1, 0.0F)
            );
            NetworkMain.sendRedirected(
                player, remoteDimension,
                new SChangeGameStatePacket(7, world.getRainStrength(1.0F))
            );
            NetworkMain.sendRedirected(
                player, remoteDimension,
                new SChangeGameStatePacket(8, world.getThunderStrength(1.0F))
            );
        }
    }
}
