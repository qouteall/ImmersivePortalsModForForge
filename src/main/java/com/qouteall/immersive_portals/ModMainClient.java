package com.qouteall.immersive_portals;

import com.immersive_portals.network.NetworkMain;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.*;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.RendererUsingStencil;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererManager;

public class ModMainClient {
    
    public static void initRenderers(EntityRendererManager manager) {
        manager.register(
            Portal.class,
            new PortalEntityRenderer(manager)
        );
        manager.register(
            NetherPortalEntity.class,
            new PortalEntityRenderer(manager)
        );
        manager.register(
            EndPortalEntity.class,
            new PortalEntityRenderer(manager)
        );
        manager.register(
            Mirror.class,
            new PortalEntityRenderer(manager)
        );
        manager.register(
            BreakableMirror.class,
            new PortalEntityRenderer(manager)
        );
        
        manager.register(
            LoadingIndicatorEntity.class,
            new LoadingIndicatorRenderer(manager)
        );
    }
    
    public static void switchToCorrectRenderer() {
        if (CGlobal.renderer.isRendering()) {
            //do not switch when rendering
            return;
        }
        switchRenderer(CGlobal.rendererUsingStencil);
//        if (OFHelper.getIsUsingShader()) {
//            if (CGlobal.isRenderDebugMode) {
//                switchRenderer(OFGlobal.rendererDebugWithShader);
//            }
//            else {
//                switchRenderer(OFGlobal.rendererMixed);
//            }
//        }
//        else {
//            switchRenderer(CGlobal.rendererUsingStencil);
//        }
    }
    
    private static void switchRenderer(PortalRenderer renderer) {
        if (CGlobal.renderer != renderer) {
            Helper.log("switched to renderer " + renderer.getClass());
            CGlobal.renderer = renderer;
        }
    }
    
    public static void onInitializeClient() {
        Helper.log("initializing client");
    
        NetworkMain.init();
        
        Minecraft.getInstance().execute(() -> {
            CGlobal.rendererUsingStencil = new RendererUsingStencil();
        
            CGlobal.renderer = CGlobal.rendererUsingStencil;
            CGlobal.clientWorldLoader = new ClientWorldLoader();
            CGlobal.myGameRenderer = new MyGameRenderer();
            CGlobal.clientTeleportationManager = new ClientTeleportationManager();
        });
//
//        CGlobal.isOptifinePresent = FabricLoader.INSTANCE.isModLoaded("optifabric");
//
//        Helper.log(CGlobal.isOptifinePresent ? "Optifine is present" : "Optifine is not present");
//
//        if (CGlobal.isOptifinePresent) {
//            OFHelper.init();
//        }
//
    }
}
