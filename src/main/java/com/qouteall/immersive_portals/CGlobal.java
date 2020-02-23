package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.render.*;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.world.dimension.DimensionType;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CGlobal {
    
    public static enum RenderMode {
        normal,
        compatibility,
        debug,
        none
    }
    
    public static PortalRenderer renderer;
    public static RendererUsingStencil rendererUsingStencil;
    public static RendererDummy rendererDummy = new RendererDummy();
    public static RendererUsingFrameBuffer rendererUsingFrameBuffer = new RendererUsingFrameBuffer();
    
    public static ClientWorldLoader clientWorldLoader;
    public static MyGameRenderer myGameRenderer;
    public static ClientTeleportationManager clientTeleportationManager;
    
    public static boolean doUseAdvancedFrustumCulling = true;
    public static int maxPortalLayer = 5;
    public static int maxIdleChunkRendererNum = 500;
    public static Object switchedFogRenderer;
    public static boolean useHackedChunkRenderDispatcher = true;
    public static boolean isClientRemoteTickingEnabled = true;
    private static boolean isOptifinePresent = false;
    public static boolean useFrontCulling = true;
    public static Map<DimensionType, Integer> renderInfoNumMap = new ConcurrentHashMap<>();
    
    public static boolean doDisableAlphaTestWhenRenderingFrameBuffer = true;
    
    public static boolean alwaysUpdateDisplayList = true;
    
    public static RenderMode renderMode = RenderMode.normal;
    
    public static ShaderManager shaderManager;
    
    public static boolean doCheckGlError = false;
    
    public static WeakReference<ClippingHelperImpl> currentFrustumCuller;
    
    public static boolean renderFewerInFastGraphic = true;
    
    public static boolean smoothUnload = true;
}
