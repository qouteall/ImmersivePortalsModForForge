package com.qouteall.hiding_in_the_bushes;

import com.qouteall.hiding_in_the_bushes.network.CtsPlayerAction;
import com.qouteall.hiding_in_the_bushes.network.CtsRightClick;
import com.qouteall.hiding_in_the_bushes.network.CtsTeleport;
import com.qouteall.hiding_in_the_bushes.network.NetworkMain;
import io.netty.buffer.Unpooled;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkDirection;

import java.io.IOException;
import java.util.UUID;

public class MyNetworkClient {
    public static IPacket createCtsPlayerAction(
        DimensionType dimension,
        CPlayerDiggingPacket packet
    ) {
        return NetworkMain.channel.toVanillaPacket(
            new CtsPlayerAction(
                dimension, packet
            ),
            NetworkDirection.PLAY_TO_SERVER
        );
    }
    
    public static IPacket createCtsRightClick(
        DimensionType dimension,
        CPlayerTryUseItemOnBlockPacket packet
    ) {
        return NetworkMain.channel.toVanillaPacket(
            new CtsRightClick(
                dimension, packet
            ),
            NetworkDirection.PLAY_TO_SERVER
        );
    }
    
    public static IPacket createCtsTeleport(
        DimensionType dimensionBefore,
        Vec3d posBefore,
        UUID portalEntityId
    ) {
        return NetworkMain.channel.toVanillaPacket(
            new CtsTeleport(
                dimensionBefore, posBefore, portalEntityId
            ),
            NetworkDirection.PLAY_TO_SERVER
        );
    }
    
    public static void init() {
        //nothing
    }
}
