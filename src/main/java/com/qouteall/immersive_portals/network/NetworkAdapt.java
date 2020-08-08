package com.qouteall.immersive_portals.network;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NetworkAdapt {
    private static boolean serverHasIP = true;
    
    public static void setServerHasIP(boolean cond) {
        if (serverHasIP) {
            if (!cond) {
                warnServerMissingIP();
            }
        }
        
        serverHasIP = cond;
    }
    
    public static boolean doesServerHasIP() {
        return serverHasIP;
    }
    
    private static void warnServerMissingIP() {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().ingameGUI.func_238450_a_(
                ChatType.SYSTEM,
                new StringTextComponent(
                    "You logged into a server that doesn't have Immersive Portals mod." +
                        " Issues may arise. It's recommended to uninstall IP before joining a vanilla server"
                ),
                Util.field_240973_b_
            );
        });
    }
}
