package com.qouteall.immersive_portals.alternate_dimension;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.render.ShaderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.opengl.GL11;

import java.util.List;

@Deprecated
public class AlternateSkyRenderer {
    public static void renderAlternateSky(MatrixStack matrixStack, float f) {
        ClientWorld world = Minecraft.getInstance().world;
        
        if (shouldRenderBlackLid(world)) {
            renderBlackLid(matrixStack);
        }
    }
    
    private static boolean shouldRenderBlackLid(ClientWorld world) {
        List<GlobalTrackedPortal> globalPortals = ((IEClientWorld) world).getGlobalPortals();
        if (globalPortals == null) {
            return false;
        }
        return globalPortals.stream().anyMatch(portal ->
            portal.getPosY() > 128 && portal.dimensionTo == DimensionType.THE_END
        );
    }
    
    private static void renderBlackLid(MatrixStack matrixStack) {
        RenderSystem.disableTexture();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableFog();
        RenderSystem.shadeModel(7425);
        
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        
        bufferBuilder.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        
        float colorR = 18 / 255.0f;
        float colorG = 13 / 255.0f;
        float colorB = 26 / 255.0f;
        float colorA = 0;
        
        bufferBuilder.pos(100, 0, 0).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(0, -100, 0).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(0, 0, 100).color(colorR, colorG, colorB, colorA).endVertex();
        
        bufferBuilder.pos(0, 0, 100).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(0, -100, 0).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(-100, 0, 0).color(colorR, colorG, colorB, colorA).endVertex();
        
        bufferBuilder.pos(-100, 0, 0).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(0, -100, 0).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(0, 0, -100).color(colorR, colorG, colorB, colorA).endVertex();
        
        bufferBuilder.pos(0, 0, -100).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(0, -100, 0).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(100, 0, 0).color(colorR, colorG, colorB, colorA).endVertex();
        
        bufferBuilder.pos(100, 0, 0).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(0, 100, 0).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(0, 0, 100).color(colorR, colorG, colorB, colorA).endVertex();
        
        bufferBuilder.pos(0, 0, 100).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(0, 100, 0).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(-100, 0, 0).color(colorR, colorG, colorB, colorA).endVertex();
        
        bufferBuilder.pos(-100, 0, 0).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(0, 100, 0).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(0, 0, -100).color(colorR, colorG, colorB, colorA).endVertex();
        
        bufferBuilder.pos(0, 0, -100).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(0, 100, 0).color(colorR, colorG, colorB, colorA).endVertex();
        bufferBuilder.pos(100, 0, 0).color(colorR, colorG, colorB, colorA).endVertex();
        
        bufferBuilder.finishDrawing();
        
        McHelper.runWithTransformation(matrixStack, () -> {
            if (CGlobal.shaderManager == null) {
                CGlobal.shaderManager = new ShaderManager();
            }
            
            double y = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView().y;
            
            float origin = y > 256 ? (float) ((256.0 - y) / 256.0) : 0;
            CGlobal.shaderManager.loadGradientSkyShader(origin);
            WorldVertexBufferUploader.draw(bufferBuilder);
            CGlobal.shaderManager.unloadShader();
        });
        
        //reset gl states
        RenderType.getBlockRenderTypes().get(0).setupRenderState();
        RenderType.getBlockRenderTypes().get(0).clearRenderState();
        RenderSystem.depthMask(true);
    }
}
