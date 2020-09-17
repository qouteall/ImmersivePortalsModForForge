package com.qouteall.hiding_in_the_bushes.network;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.network.CommonNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.ProtocolType;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StcRedirected {
    public RegistryKey<World> dimension;
    public int packetId;
    public IPacket packet;
    
    public StcRedirected(
        RegistryKey<World> dimensionType,
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
        dimension = DimId.readWorldId(buf, true);
        packetId = buf.readInt();
        packet = NetworkMain.createEmptyPacketByType(packetId);
        try {
            packet.readPacketData(buf);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    private static int reportedErrors = 0;
    
    public static void doProcessRedirectedPacket(
        RegistryKey<World> dimension,
        IPacket packet
    ) {
        CommonNetwork.processRedirectedPacket(dimension, packet);
    }
    
    public void encode(PacketBuffer buf) {
        DimId.writeWorldId(buf, dimension, false);
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
