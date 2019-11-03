package com.qouteall.immersive_portals.ducks;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.shader.Framebuffer;

public interface IEMinecraftClient {
    void setFrameBuffer(Framebuffer buffer);
    
    Screen getCurrentScreen();
}
