package com.qouteall.immersive_portals.ducks;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.IDynamicRegistries;
import java.util.Map;

public interface IEClientPlayNetworkHandler {
    void setWorld(ClientWorld world);
    
    Map getPlayerListEntries();
    
    void setPlayerListEntries(Map value);
    
    void initScreenIfNecessary();
    
    void portal_setDimensionTracker(IDynamicRegistries arg);
}
