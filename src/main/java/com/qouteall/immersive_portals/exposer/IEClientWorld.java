package com.qouteall.immersive_portals.exposer;

import net.minecraft.client.network.play.ClientPlayNetHandler;

public interface IEClientWorld {
    public ClientPlayNetHandler getNetHandler();
    
    public void setNetHandler(ClientPlayNetHandler handler);
}
