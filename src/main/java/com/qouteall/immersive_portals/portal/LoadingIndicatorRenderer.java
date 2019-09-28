package com.qouteall.immersive_portals.portal;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class LoadingIndicatorRenderer extends EntityRenderer<LoadingIndicatorEntity> {
    protected LoadingIndicatorRenderer(EntityRendererManager entityRenderDispatcher_1) {
        super(entityRenderDispatcher_1);
    }
    
    @Override
    protected ResourceLocation getEntityTexture(LoadingIndicatorEntity var1) {
        return null;
    }
    
    @Override
    public void doRender(
        LoadingIndicatorEntity entity_1,
        double double_1,
        double double_2,
        double double_3,
        float float_1,
        float float_2
    ) {
        this.renderEntityName(entity_1, double_1, double_2, double_3, "Loading", 64);
    }
}
