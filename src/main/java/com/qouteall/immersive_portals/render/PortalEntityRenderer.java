package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PortalEntityRenderer extends EntityRenderer<Portal> {
    
    public PortalEntityRenderer(EntityRendererManager entityRenderDispatcher_1) {
        super(entityRenderDispatcher_1);
    }
    
    @Override
    public void render(
        Portal portal,
        float yaw,
        float tickDelta,
        MatrixStack matrixStack,
        IRenderTypeBuffer vertexConsumerProvider,
        int light
    ) {
        
        CGlobal.renderer.renderPortalInEntityRenderer(portal);
        
//        if (portal instanceof BreakablePortalEntity) {
//            BreakablePortalEntity breakablePortalEntity = (BreakablePortalEntity) portal;
//            OverlayRendering.renderBreakablePortalOverlay(
//                breakablePortalEntity, tickDelta, matrixStack, vertexConsumerProvider, light
//            );
//        }
//
        super.render(portal, yaw, tickDelta, matrixStack, vertexConsumerProvider, light);
    }
    
    @Override
    public ResourceLocation getEntityTexture(Portal portal) {
//        if (portal instanceof BreakablePortalEntity) {
//            if (((BreakablePortalEntity) portal).overlayBlockState != null) {
//                return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
//            }
//        }
        return null;
    }
    
}
