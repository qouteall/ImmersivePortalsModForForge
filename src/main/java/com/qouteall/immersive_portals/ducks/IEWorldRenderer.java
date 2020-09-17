package com.qouteall.immersive_portals.ducks;

import com.mojang.blaze3d.matrix.MatrixStack;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderTypeBuffers;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;

public interface IEWorldRenderer {
    EntityRendererManager getEntityRenderDispatcher();
    
    ViewFrustum getBuiltChunkStorage();
    
    ObjectList getVisibleChunks();
    
    void setVisibleChunks(ObjectList l);
    
    ChunkRenderDispatcher getChunkBuilder();
    
    void myRenderEntity(
        Entity entity,
        double cameraX,
        double cameraY,
        double cameraZ,
        float tickDelta,
        MatrixStack matrixStack,
        IRenderTypeBuffer vertexConsumerProvider
    );
    
    ShaderGroup portal_getTransparencyShader();
    
    void portal_setTransparencyShader(ShaderGroup arg);
    
    RenderTypeBuffers getBufferBuilderStorage();
    
    void setBufferBuilderStorage(RenderTypeBuffers arg);
    
    int portal_getRenderDistance();
    
    void portal_setRenderDistance(int arg);
}
