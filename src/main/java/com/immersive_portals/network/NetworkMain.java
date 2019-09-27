package com.immersive_portals.network;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class NetworkMain {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("immersive_portals", "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    public static void init(){
        INSTANCE.registerMessage(
            0,
            StcRedirected.class,
            StcRedirected::encode,
            StcRedirected::new,
            StcRedirected::handle
        );
    }
}
