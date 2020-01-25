package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.optifine_compatibility.OFGlobal;
import com.qouteall.immersive_portals.optifine_compatibility.OFInterfaceInitializer;
import com.qouteall.immersive_portals.portal.*;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalEntity;
import com.qouteall.immersive_portals.portal.nether_portal.NewNetherPortalEntity;
import com.qouteall.immersive_portals.render.*;
import com.qouteall.immersive_portals.teleportation.ClientTeleportationManager;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.EntityType;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Field;
import java.util.Arrays;

public class ModMainClient {
    
    
    public static void initRenderers(EntityRendererManager manager) {
        Arrays.stream(new EntityType<?>[]{
            Portal.entityType,
            NetherPortalEntity.entityType,
            NewNetherPortalEntity.entityType,
            EndPortalEntity.entityType,
            Mirror.entityType,
            BreakableMirror.entityType,
            GlobalTrackedPortal.entityType,
            BorderPortal.entityType,
            VerticalConnectingPortal.entityType
        }).peek(
            Validate::notNull
        ).forEach(
            entityType -> manager.register(
                entityType,
                (EntityRenderer) new PortalEntityRenderer(manager)
            )
        );
    
        manager.register(
            LoadingIndicatorEntity.entityType,
            new LoadingIndicatorRenderer(manager)
        );
    }
    
    public static void switchToCorrectRenderer() {
        if (CGlobal.renderer.isRendering()) {
            //do not switch when rendering
            return;
        }
        if (OFInterface.isShaders.getAsBoolean()) {
            switch (CGlobal.renderMode) {
                case normal:
                    switchRenderer(OFGlobal.rendererMixed);
                    break;
                case compatibility:
                    switchRenderer(OFGlobal.rendererDeferred);
                    break;
                case debug:
                    switchRenderer(OFGlobal.rendererDebugWithShader);
                    break;
                case none:
                    switchRenderer(CGlobal.rendererDummy);
                    break;
            }
        }
        else {
            switch (CGlobal.renderMode) {
                case normal:
                    switchRenderer(CGlobal.rendererUsingStencil);
                    break;
                case compatibility:
                    switchRenderer(CGlobal.rendererUsingFrameBuffer);
                    break;
                case debug:
                    //TODO add debug renderer for non shader mode
                    break;
                case none:
                    switchRenderer(CGlobal.rendererDummy);
                    break;
            }
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
    
    private static Field gameSettings_ofRenderRegions;
    
    public static void turnOffRenderRegionOption() {
        if (!OFInterface.isOptifinePresent) {
            return;
        }
        
        if (gameSettings_ofRenderRegions == null) {
            try {
                gameSettings_ofRenderRegions =
                    GameSettings.class.getDeclaredField("ofRenderRegions");
            }
            catch (NoSuchFieldException e) {
                throw new IllegalStateException(e);
            }
        }
        
        try {
            gameSettings_ofRenderRegions.set(
                Minecraft.getInstance().gameSettings,
                false
            );
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
