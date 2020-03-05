package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.hiding_in_the_bushes.alternate_dimension.AlternateDimension;
import com.qouteall.hiding_in_the_bushes.network.NetworkMain;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SChangeGameStatePacket;
import net.minecraft.network.play.server.SUpdateTimePacket;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

public class WorldInfoSender {
    public static void init() {
        ModMain.postServerTickSignal.connect(() -> {
            if (McHelper.getServerGameTime() % 100 == 42) {
                for (ServerPlayerEntity player : McHelper.getCopiedPlayerList()) {
                    if (player.dimension != DimensionType.OVERWORLD) {
                        sendWorldInfo(
                            player,
                            McHelper.getServer().getWorld(DimensionType.OVERWORLD)
                        );
                    }
                    McHelper.getServer().getWorlds().forEach(thisWorld -> {
                        if (thisWorld.dimension instanceof AlternateDimension) {
                            sendWorldInfo(
                                player,
                                thisWorld
                            );
                        }
                    });
    
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
                //start raining
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
        else {
            NetworkMain.sendRedirected(
                player, remoteDimension,
                //stop raining
                new SChangeGameStatePacket(7, world.getRainStrength(1))
            );
            NetworkMain.sendRedirected(
                player, remoteDimension,
                new SChangeGameStatePacket(8, world.getThunderStrength(1.0F))
            );
        }
    }
}
