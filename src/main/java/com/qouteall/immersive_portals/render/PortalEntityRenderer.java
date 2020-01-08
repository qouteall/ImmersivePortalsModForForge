package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class PortalEntityRenderer extends EntityRenderer<Portal> {
    public PortalEntityRenderer(EntityRendererManager entityRenderDispatcher_1) {
        super(entityRenderDispatcher_1);
    }
    
    @Override
    public boolean shouldRender(
        Portal entity_1,
        ICamera visibleRegion_1,
        double double_1,
        double double_2,
        double double_3
    ) {
        return true;
    }
    
    @Override
    public void doRender(
        Portal portal,
        double double_1,
        double double_2,
        double double_3,
        float float_1,
        float float_2
    ) {
        super.doRender(portal, double_1, double_2, double_3, float_1, float_2);
        CGlobal.renderer.renderPortalInEntityRenderer(portal);
        Helper.checkGlError();
    }
    
    @Override
    protected ResourceLocation getEntityTexture(Portal var1) {
        return null;
    }
}
