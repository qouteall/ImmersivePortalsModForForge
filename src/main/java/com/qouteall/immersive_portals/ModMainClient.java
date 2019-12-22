package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.optifine_compatibility.OFGlobal;
import com.qouteall.immersive_portals.optifine_compatibility.OFInterfaceInitializer;
import com.qouteall.immersive_portals.portal.*;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.NewNetherPortalEntity;
import com.qouteall.immersive_portals.render.*;
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
            NewNetherPortalEntity.class,
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
        if (OFInterface.isShaders.getAsBoolean()) {
            if (CGlobal.isRenderDebugMode) {
                switchRenderer(OFGlobal.rendererDebugWithShader);
            }
            else {
                switchRenderer(OFGlobal.rendererMixed);
            }
        }
        else {
            switchRenderer(CGlobal.rendererUsingStencil);
        }
    }
    
    private static void switchRenderer(PortalRenderer renderer) {
        if (CGlobal.renderer != renderer) {
            Helper.log("switched to renderer " + renderer.getClass());
            CGlobal.renderer = renderer;
        }
    }
    
    public static boolean getIsOptifinePresent() {
        try {
            //do not load other optifine classes that loads vanilla classes
            //that would load the class before mixin
            Class.forName("optifine.ZipResourceProvider");
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public static void onInitializeClient() {
        Helper.log("initializing client");
    
        Minecraft.getInstance().execute(() -> {
            CGlobal.rendererUsingStencil = new RendererUsingStencil();
        
            CGlobal.renderer = CGlobal.rendererUsingStencil;
            CGlobal.clientWorldLoader = new ClientWorldLoader();
            CGlobal.myGameRenderer = new MyGameRenderer();
            CGlobal.clientTeleportationManager = new ClientTeleportationManager();
        
            OFInterface.isOptifinePresent = getIsOptifinePresent();
            Helper.log(OFInterface.isOptifinePresent ? "Optifine is present" : "Optifine is not present");
        
            if (OFInterface.isOptifinePresent) {
                OFInterfaceInitializer.init();
                OFInterface.initShaderCullingManager.run();
            }
        });
    }
}
