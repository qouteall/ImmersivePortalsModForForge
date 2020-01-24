package com.qouteall.immersive_portals.network;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.ProtocolType;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StcRedirected {
    public DimensionType dimension;
    public int packetId;
    public IPacket packet;
    
    public StcRedirected(
        DimensionType dimensionType,
        IPacket packet
    ) {
        this.dimension = dimensionType;
        Validate.notNull(dimensionType);
        this.packet = packet;
        try {
            packetId = ProtocolType.PLAY.getPacketId(PacketDirection.CLIENTBOUND, packet);
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    public StcRedirected(PacketBuffer buf) {
        int dimensionId = buf.readInt();
        packetId = buf.readInt();
        dimension = DimensionType.getById(dimensionId);
        packet = NetworkMain.createEmptyPacketByType(packetId);
        try {
            packet.readPacketData(buf);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    
        if (dimension == null) {
            Helper.err(String.format(
                "Invalid redirected packet %s %s \nRegistered dimensions %s",
                dimensionId, packet,
                Registry.DIMENSION_TYPE.stream().map(
                    dim -> dim.toString() + " " + dim.getId()
                ).collect(Collectors.joining("\n"))
            ));
        }
    }
    
    private static void doProcessRedirectedPacket(
        DimensionType dimension,
        IPacket packet
    ) {
        Minecraft mc = Minecraft.getInstance();
        
        ClientWorld packetWorld = CGlobal.clientWorldLoader.getOrCreateFakedWorld(dimension);
        
        assert packetWorld != null;
        
        assert packetWorld.getChunkProvider() instanceof MyClientChunkManager;
        
        ClientPlayNetHandler netHandler = ((IEClientWorld) packetWorld).getNetHandler();
        
        if ((netHandler).getWorld() != packetWorld) {
            ((IEClientPlayNetworkHandler) netHandler).setWorld(packetWorld);
            Helper.err("The world field of client net handler is wrong");
        }
        
        ClientWorld originalWorld = mc.world;
        //some packet handling may use mc.world so switch it
        mc.world = packetWorld;
        
        try {
            packet.processPacket(netHandler);
        }
        catch (Throwable e) {
            throw new IllegalStateException(
                "handling packet in " + dimension,
                e
            );
        }
        finally {
            mc.world = originalWorld;
        }
    }
    
    public void encode(PacketBuffer buf) {
        buf.writeInt(dimension.getId());
        buf.writeInt(packetId);
        
        try {
            packet.writePacketData(buf);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        if (dimension == null) {
            throw new IllegalStateException(
                "Redirected Packet without Dimension info " + packet
            );
        }
    
        context.get().enqueueWork(() -> doProcessRedirectedPacket(dimension, packet));
    
        context.get().setPacketHandled(true);
    }
}
