package com.qouteall.immersive_portals.ducks;

import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import java.util.List;
import net.minecraft.client.network.play.ClientPlayNetHandler;

public interface IEClientWorld {
    ClientPlayNetHandler getNetHandler();
    
    void setNetHandler(ClientPlayNetHandler handler);
    
    List<GlobalTrackedPortal> getGlobalPortals();
    
    void setGlobalPortals(List<GlobalTrackedPortal> arg);
}
