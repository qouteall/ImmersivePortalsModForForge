package com.qouteall.immersive_portals.network;

import com.qouteall.hiding_in_the_bushes.O_O;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class RemoteProcedureCall {
    public static void tellClientToInvoke(
        ServerPlayerEntity player,
        String methodPath,
        Object... arguments
    ) {
        if (O_O.isForge()) {
            throw new RuntimeException("Not yet supported on the Forge version");
        }
        
        SCustomPayloadPlayPacket packet =
            ImplRemoteProcedureCall.createS2CPacket(methodPath, arguments);
        player.connection.sendPacket(packet);
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void tellServerToInvoke(
        String methodPath,
        Object... arguments
    ) {
        if (O_O.isForge()) {
            throw new RuntimeException("Not yet supported on the Forge version");
        }
        
        CCustomPayloadPacket packet =
            ImplRemoteProcedureCall.createC2SPacket(methodPath, arguments);
        Minecraft.getInstance().getConnection().sendPacket(packet);
    }
}
