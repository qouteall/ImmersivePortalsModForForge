package com.qouteall.hiding_in_the_bushes.network;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationServer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkEvent;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

public class CtsPlayerAction {
    DimensionType dimension;
    CPlayerDiggingPacket packet;
    
    public CtsPlayerAction(
        DimensionType dimension,
        CPlayerDiggingPacket packet
    ) {
        this.dimension = dimension;
        this.packet = packet;
    }
    
    public CtsPlayerAction(PacketBuffer buf) {
        dimension = DimensionType.getById(buf.readInt());
        packet = new CPlayerDiggingPacket();
        try {
            packet.readPacketData(buf);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void encode(PacketBuffer buf) {
        buf.writeInt(dimension.getId());
        try {
            packet.writePacketData(buf);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            BlockManipulationServer.processBreakBlock(
                dimension, packet,
                ((ServerPlayerEntity) context.get().getSender())
            );
        });
        context.get().setPacketHandled(true);
    }
}
