package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;

// temporary work around for bugs
public class DubiousThings {
    public static void init() {
        ModMain.postClientTickSignal.connect(DubiousThings::tick);
    }
    
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
            checkClientPlayerState();
        }
    }
    
    // TODO test is it necessary
    @Deprecated
    private static void doUpdateLight(ClientPlayerEntity player) {
        Minecraft.getInstance().getProfiler().startSection("my_light_update");
        MyClientChunkManager.updateLightStatus(player.world.getChunk(
            player.chunkCoordX, player.chunkCoordZ
        ));
        Minecraft.getInstance().getProfiler().endSection();
    }
    
    private static void checkClientPlayerState() {
        Minecraft client = Minecraft.getInstance();
        if (client.world != client.player.world) {
            Helper.err("Player world abnormal");
            //don't know how to fix it
        }
        Entity playerInWorld = client.world.getEntityByID(client.player.getEntityId());
        if (playerInWorld != client.player) {
            Helper.err("Client Player Mismatch");
            if (playerInWorld instanceof ClientPlayerEntity) {
                client.player = ((ClientPlayerEntity) playerInWorld);
                Helper.log("Force corrected");
            }
            else {
                Helper.err("Non-player entity in client has duplicate id");
            }
        }
    }
}
