package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class PortalEntityRenderer extends EntityRenderer<Portal> {
    public PortalEntityRenderer(EntityRendererManager entityRenderDispatcher_1) {
        super(entityRenderDispatcher_1);
    }
    
    @Override
    public void render(
        Portal portal,
        float float_1,
        float float_2,
        MatrixStack matrixStack_1,
        IRenderTypeBuffer vertexConsumerProvider_1,
        int int_1
    ) {
        super.render(portal, float_1, float_2, matrixStack_1, vertexConsumerProvider_1, int_1);
        CGlobal.renderer.renderPortalInEntityRenderer(portal);
    }
    
    @Override
    public ResourceLocation getEntityTexture(Portal var1) {
        return null;
    }
}
