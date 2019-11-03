package com.qouteall.immersive_portals.exposer;

import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.client.network.play.ClientPlayNetHandler;

import java.util.List;

public interface IEClientWorld {
    public ClientPlayNetHandler getNetHandler();
    
    public void setNetHandler(ClientPlayNetHandler handler);
    
    public List<GlobalTrackedPortal> getGlobalPortals();
    
    public void setGlobalPortals(List<GlobalTrackedPortal> arg);
}
