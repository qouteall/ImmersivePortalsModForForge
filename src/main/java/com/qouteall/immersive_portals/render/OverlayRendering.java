package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.portal.PortalLike;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class OverlayRendering {
    private static final Random random = new Random();
    
    public static boolean test = false;
    
    private static final IRenderTypeBuffer.Impl vertexConsumerProvider =
        IRenderTypeBuffer.getImpl(new BufferBuilder(256));
    
    public static class PortalOverlayRenderLayer extends RenderType {
        
        public PortalOverlayRenderLayer() {
            super(
                "imm_ptl_portal_overlay",
                RenderType.func_239269_g_().getVertexFormat(),
                RenderType.func_239269_g_().getDrawMode(),
                RenderType.func_239269_g_().getBufferSize(),
                RenderType.func_239269_g_().isUseDelegate(),
                true,
                () -> {
                    RenderType.func_239269_g_().setupRenderState();
//                    RenderSystem.enableBlend();
//                    RenderSystem.color4f(0,0,1,1);
                },
                () -> {
                    RenderType.func_239269_g_().clearRenderState();
                }
            );
        }
    }
    
    public static final PortalOverlayRenderLayer portalOverlayRenderLayer = new PortalOverlayRenderLayer();
    
    public static boolean shouldRenderOverlay(PortalLike portal) {
        if (portal instanceof BreakablePortalEntity) {
            if (((BreakablePortalEntity) portal).overlayBlockState != null) {
                return true;
            }
        }
        return false;
    }
    
    public static void onPortalRendered(
        PortalLike portal,
        MatrixStack matrixStack
    ) {
        if (portal instanceof BreakablePortalEntity) {
            renderBreakablePortalOverlay(
                ((BreakablePortalEntity) portal),
                RenderStates.tickDelta,
                matrixStack,
                vertexConsumerProvider
            );
        }
    }
    
    public static List<BakedQuad> getQuads(IBakedModel model, BlockState blockState) {
        List<BakedQuad> result = new ArrayList<>();
        
        for (Direction direction : Direction.values()) {
            result.addAll(model.getQuads(blockState, direction, random));
        }
        
        result.addAll(model.getQuads(blockState, null, random));
        
        return result;
    }
    
    /**
     * {@link net.minecraft.client.render.entity.FallingBlockEntityRenderer}
     */
    private static void renderBreakablePortalOverlay(
        BreakablePortalEntity portal,
        float tickDelta,
        MatrixStack matrixStack,
        IRenderTypeBuffer.Impl vertexConsumerProvider
    ) {
        BlockState blockState = portal.overlayBlockState;
        
        Vector3d cameraPos = CHelper.getCurrentCameraPos();
        
        if (blockState == null) {
            return;
        }
        
        BlockRendererDispatcher blockRenderManager = Minecraft.getInstance().getBlockRendererDispatcher();
        
        BlockPortalShape blockPortalShape = portal.blockPortalShape;
        if (blockPortalShape == null) {
            return;
        }
        
        matrixStack.push();
        
        Vector3d pos = portal.getPositionVec();
        
        matrixStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
        
        IBakedModel model = blockRenderManager.getModelForState(blockState);
        PortalOverlayRenderLayer renderLayer = OverlayRendering.portalOverlayRenderLayer;
        IVertexBuilder buffer = vertexConsumerProvider.getBuffer(renderLayer);
        
        List<BakedQuad> quads = getQuads(model, blockState);
        
        random.setSeed(0);
        
        for (BlockPos blockPos : blockPortalShape.area) {
            matrixStack.push();
            matrixStack.translate(
                blockPos.getX() - pos.x, blockPos.getY() - pos.y, blockPos.getZ() - pos.z
            );
            
            for (BakedQuad quad : quads) {
                renderQuad(
                    buffer,
                    matrixStack.getLast(),
                    quad,
                    0.6f,
                    1.0f, 1.0f, 1.0f,
                    14680304,//packed light value
                    OverlayTexture.NO_OVERLAY,
                    true,
                    ((float) portal.overlayOpacity)
                );
            }

//            blockRenderManager.getModelRenderer().render(
//                portal.world,
//                model,
//                blockState,
//                blockPos,
//                matrixStack,
//                buffer,
//                false,
//                random,
//                blockState.getRenderingSeed(blockPos),
//                OverlayTexture.DEFAULT_UV
//            );
            
            matrixStack.pop();
        }
        
        matrixStack.pop();
        
        vertexConsumerProvider.finish(renderLayer);
        
    }
    
    /**
     * vanilla copy
     * vanilla block model rendering does not support customizing alpha
     * and glColor() also doesn't work
     * {@link net.minecraft.client.render.VertexConsumer#quad(net.minecraft.client.util.math.MatrixStack.Entry, net.minecraft.client.render.model.BakedQuad, float[], float, float, float, int[], int, boolean)}
     */
    public static void renderQuad(
        IVertexBuilder vertexConsumer,
        MatrixStack.Entry matrixEntry, BakedQuad quad,
        float brightness, float red, float green, float blue,
        int lights, int overlay, boolean useQuadColorData,
        float alpha
    ) {
        int[] is = quad.getVertexData();
        Vector3i vec3i = quad.getFace().getDirectionVec();
        Vector3f vector3f = new Vector3f((float) vec3i.getX(), (float) vec3i.getY(), (float) vec3i.getZ());
        Matrix4f matrix4f = matrixEntry.getMatrix();
        vector3f.transform(matrixEntry.getNormal());
        
        int j = is.length / 8;
        MemoryStack memoryStack = MemoryStack.stackPush();
        Throwable var17 = null;
        
        try {
            ByteBuffer byteBuffer = memoryStack.malloc(DefaultVertexFormats.BLOCK.getSize());
            IntBuffer intBuffer = byteBuffer.asIntBuffer();
            
            for (int k = 0; k < j; ++k) {
                intBuffer.clear();
                intBuffer.put(is, k * 8, 8);
                float f = byteBuffer.getFloat(0);
                float g = byteBuffer.getFloat(4);
                float h = byteBuffer.getFloat(8);
                float r;
                float s;
                float t;
                float v;
                float w;
                if (useQuadColorData) {
                    float l = (float) (byteBuffer.get(12) & 255) / 255.0F;
                    v = (float) (byteBuffer.get(13) & 255) / 255.0F;
                    w = (float) (byteBuffer.get(14) & 255) / 255.0F;
                    r = l * brightness * red;
                    s = v * brightness * green;
                    t = w * brightness * blue;
                }
                else {
                    r = brightness * red;
                    s = brightness * green;
                    t = brightness * blue;
                }
                
                int u = lights;
                v = byteBuffer.getFloat(16);
                w = byteBuffer.getFloat(20);
                Vector4f vector4f = new Vector4f(f, g, h, 1.0F);
                vector4f.transform(matrix4f);
                vertexConsumer.addVertex(vector4f.getX(), vector4f.getY(), vector4f.getZ(), r, s, t, alpha, v, w, overlay, u, vector3f.getX(), vector3f.getY(), vector3f.getZ());
            }
        }
        catch (Throwable var38) {
            var17 = var38;
            throw var38;
        }
        finally {
            if (memoryStack != null) {
                if (var17 != null) {
                    try {
                        memoryStack.close();
                    }
                    catch (Throwable var37) {
                        var17.addSuppressed(var37);
                    }
                }
                else {
                    memoryStack.close();
                }
            }
            
        }
    }
}
