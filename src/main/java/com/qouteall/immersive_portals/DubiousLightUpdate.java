package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

public class DubiousLightUpdate {
    public static void init() {
        ModMain.postClientTickSignal.connect(DubiousLightUpdate::tick);
    }
    
    //fix light issue https://github.com/qouteall/ImmersivePortalsMod/issues/45
    //it's not an elegant solution
    //the issue could be caused by other things
    private static void tick() {
        ClientWorld world = Minecraft.getInstance().world;
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (world == null) {
            return;
        }
        if (player == null) {
            return;
        }
        if (world.getGameTime() % 233 == 34) {
            doUpdateLight(player);
        }
    }
    
    private static void doUpdateLight(ClientPlayerEntity player) {
        Minecraft.getInstance().getProfiler().startSection("my_light_update");
        MyClientChunkManager.updateLightStatus(player.world.getChunk(
            player.chunkCoordX, player.chunkCoordZ
        ));
        Minecraft.getInstance().getProfiler().endSection();
    }
}
