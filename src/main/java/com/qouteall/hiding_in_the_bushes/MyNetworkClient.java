package com.qouteall.hiding_in_the_bushes;

import com.qouteall.hiding_in_the_bushes.network.CtsPlayerAction;
import com.qouteall.hiding_in_the_bushes.network.CtsRightClick;
import com.qouteall.hiding_in_the_bushes.network.CtsTeleport;
import com.qouteall.hiding_in_the_bushes.network.NetworkMain;
import com.qouteall.hiding_in_the_bushes.network.StcRedirected;
import io.netty.buffer.Unpooled;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkDirection;

import java.io.IOException;
import java.util.UUID;

public class MyNetworkClient {
    public static IPacket createCtsPlayerAction(
        RegistryKey<World> dimension,
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
        RegistryKey<World> dimension,
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
        RegistryKey<World> dimensionBefore,
        Vector3d posBefore,
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
    
    public static void doProcessRedirectedMessage(
        ClientWorld world, IPacket packet
    ) {
        StcRedirected.doProcessRedirectedPacket(
            world.func_234923_W_(),
            packet
        );
    }
}
