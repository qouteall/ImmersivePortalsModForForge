package com.qouteall.immersive_portals.optifine_compatibility;

import net.minecraft.client.renderer.WorldRenderer;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class OFWorldRendererFix {
    private static Field f_renderInfosNormal;
    
    public static void init() {
        try {
            f_renderInfosNormal = WorldRenderer.class.getDeclaredField("renderInfosNormal");
            f_renderInfosNormal.setAccessible(true);
        }
        catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static void createNewRenderInfosNormal(WorldRenderer worldRenderer) {
        try {
            f_renderInfosNormal.set(worldRenderer, new ArrayList<>(512));
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
