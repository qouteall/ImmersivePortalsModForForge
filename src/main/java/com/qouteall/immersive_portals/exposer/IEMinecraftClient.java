package com.qouteall.immersive_portals.exposer;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.shader.Framebuffer;

public interface IEMinecraftClient {
    void setFrameBuffer(Framebuffer buffer);
    
    Screen getCurrentScreen();
}
