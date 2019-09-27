package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.my_util.ICustomStcPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class MyNetwork {
    public static final ResourceLocation id_ctsTeleport =
        new ResourceLocation("immersive_portals", "teleport");
    public static final ResourceLocation id_stcCustom =
        new ResourceLocation("immersive_portals", "stc_custom");
    public static final ResourceLocation id_stcSpawnEntity =
        new ResourceLocation("immersive_portals", "spawn_entity");
    public static final ResourceLocation id_stcDimensionConfirm =
        new ResourceLocation("immersive_portals", "dim_confirm");
    public static final ResourceLocation id_stcRedirected =
        new ResourceLocation("immersive_portals", "redirected");
    public static final ResourceLocation id_stcSpawnLoadingIndicator =
        new ResourceLocation("immersive_portals", "indicator");
    
    static void processCtsTeleport(PacketContext context, PacketBuffer buf) {
        DimensionType dimensionBefore = DimensionType.byRawId(buf.readInt());
        Vec3d posBefore = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        int portalEntityId = buf.readInt();
        
        ModMain.serverTaskList.addTask(() -> {
            SGlobal.serverTeleportationManager.onPlayerTeleportedInClient(
                (ServerPlayerEntity) context.getPlayer(),
                dimensionBefore,
                posBefore,
                portalEntityId
            );
            
            return true;
        });
    }
    
    public static void init() {
        ServerSidePacketRegistry.INSTANCE.register(
            id_ctsTeleport,
            MyNetwork::processCtsTeleport
        );
    }
    
    public static SCustomPayloadPlayPacket createRedirectedMessage(
        DimensionType dimension,
        IPacket packet
    ) {
        int messageType = 0;
        try {
            messageType = ProtocolType.PLAY.getPacketId(PacketDirection.CLIENTBOUND, packet);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeInt(dimension.getId());
        buf.writeInt(messageType);
        
        try {
            packet.write(buf);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        
        return new SCustomPayloadPlayPacket(id_stcRedirected, buf);
    }
    
    public static IPacket createEmptyPacketByType(
        int messageType
    ) {
        try {
            return ProtocolType.PLAY.getPacketHandler(PacketDirection.CLIENTBOUND, messageType);
        }
        catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public static void sendRedirectedMessage(
        ServerPlayerEntity player,
        DimensionType dimension,
        IPacket packet
    ) {
        player.connection.sendPacket(createRedirectedMessage(dimension, packet));
    }
    
    public static SCustomPayloadPlayPacket createStcDimensionConfirm(
        DimensionType dimensionType,
        Vec3d pos
    ) {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeInt(dimensionType.getId());
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        return new SCustomPayloadPlayPacket(id_stcDimensionConfirm, buf);
    }
    
    //you can input a lambda expression and it will be invoked remotely
    //but java serialization is not stable
    @Deprecated
    public static SCustomPayloadPlayPacket createCustomPacketStc(
        ICustomStcPacket serializable
    ) {
        //it copies the data twice but as the packet is small it's of no problem
        
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream stream = null;
        try {
            stream = new ObjectOutputStream(byteArrayOutputStream);
            stream.writeObject(serializable);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(byteArrayOutputStream.toByteArray());
        
        PacketBuffer buf = new PacketBuffer(buffer);
        
        return new SCustomPayloadPlayPacket(id_stcCustom, buf);
    }
    
    //NOTE my packet is redirected but I cannot get the packet handler info here
    public static SCustomPayloadPlayPacket createStcSpawnEntity(
        Entity entity
    ) {
        EntityType entityType = entity.getType();
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeString(EntityType.getId(entityType).toString());
        buf.writeInt(entity.getEntityId());
        buf.writeInt(entity.dimension.getId());
        CompoundNBT tag = new CompoundNBT();
        entity.toTag(tag);
        buf.writeCompoundTag(tag);
        return new SCustomPayloadPlayPacket(id_stcSpawnEntity, buf);
    }
    
    public static SCustomPayloadPlayPacket createSpawnLoadingIndicator(
        DimensionType dimensionType,
        Vec3d pos
    ) {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeInt(dimensionType.getId());
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        return new SCustomPayloadPlayPacket(id_stcSpawnLoadingIndicator, buf);
    }
}
