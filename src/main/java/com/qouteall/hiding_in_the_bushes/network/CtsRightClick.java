package com.qouteall.hiding_in_the_bushes.network;

import com.qouteall.immersive_portals.block_manipulation.BlockManipulationServer;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

import java.io.IOException;
import java.util.function.Supplier;

public class CtsRightClick {
    RegistryKey<World> dimension;
    CPlayerTryUseItemOnBlockPacket packet;
    
    public CtsRightClick(
        RegistryKey<World> dimension,
        CPlayerTryUseItemOnBlockPacket packet
    ) {
        this.dimension = dimension;
        this.packet = packet;
    }
    
    public CtsRightClick(PacketBuffer buf) {
        dimension = DimId.readWorldId(buf, false);
        packet = new CPlayerTryUseItemOnBlockPacket();
        try {
            packet.readPacketData(buf);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void encode(PacketBuffer buf) {
        DimId.writeWorldId(buf, dimension, true);
        try {
            packet.writePacketData(buf);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            BlockManipulationServer.processRightClickBlock(
                dimension, packet,
                ((ServerPlayerEntity) context.get().getSender())
            );
        });
        context.get().setPacketHandled(true);
    }
    
}
