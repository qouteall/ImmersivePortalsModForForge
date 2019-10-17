package com.immersive_portals.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class NetworkMain {
    private static final String protocol_version = "1";
    public static final SimpleChannel channel = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("assets/immersive_portals", "main"),
        () -> protocol_version,
        protocol_version::equals,
        protocol_version::equals
    );
    
    public static void init() {
        channel.registerMessage(
            0,
            StcRedirected.class,
            StcRedirected::encode,
            StcRedirected::new,
            StcRedirected::handle
        );
        channel.registerMessage(
            1,
            StcDimensionConfirm.class,
            StcDimensionConfirm::encode,
            StcDimensionConfirm::new,
            StcDimensionConfirm::handle
        );
        channel.registerMessage(
            2,
            StcSpawnLoadingIndicator.class,
            StcSpawnLoadingIndicator::encode,
            StcSpawnLoadingIndicator::new,
            StcSpawnLoadingIndicator::handle
        );
        channel.registerMessage(
            3,
            CtsTeleport.class,
            CtsTeleport::encode,
            CtsTeleport::new,
            CtsTeleport::handle
        );
    }
    
    public static <T> void sendToServer(T t) {
        channel.sendToServer(t);
    }
    
    public static <T> void sendToPlayer(ServerPlayerEntity player, T t) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), t);
    }
    
    public static void sendRedirected(
        ServerPlayerEntity player,
        DimensionType dimension,
        IPacket t
    ) {
        sendToPlayer(player, new StcRedirected(dimension, t));
    }
    
    public static IPacket getRedirectedPacket(
        DimensionType dimension,
        IPacket t
    ) {
        return channel.toVanillaPacket(
            new StcRedirected(dimension, t), NetworkDirection.PLAY_TO_CLIENT
        );
    }
}
