package com.qouteall.immersive_portals.render.context_management;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;

public class RenderInfo {
    public ClientWorld world;
    public Vector3d cameraPos;
    @Nullable
    public Matrix4f additionalTransformation;
    @Nullable
    public Portal portal;
    public int renderDistance;
    
    private static final Stack<RenderInfo> renderInfoStack = new Stack<>();
    
    public RenderInfo(
        ClientWorld world, Vector3d cameraPos,
        Matrix4f additionalTransformation, @Nullable Portal portal
    ) {
        this(
            world, cameraPos, additionalTransformation, portal,
            Minecraft.getInstance().gameSettings.renderDistanceChunks
        );
    }
    
    public RenderInfo(
        ClientWorld world, Vector3d cameraPos,
        @Nullable Matrix4f additionalTransformation,
        @Nullable Portal portal, int renderDistance
    ) {
        this.world = world;
        this.cameraPos = cameraPos;
        this.additionalTransformation = additionalTransformation;
        this.portal = portal;
        this.renderDistance = renderDistance;
    }
    
    public UUID getDescription() {
        return portal != null ? portal.getUniqueID() : null;
    }
    
    public static void pushRenderInfo(RenderInfo renderInfo) {
        renderInfoStack.push(renderInfo);
    }
    
    public static void popRenderInfo() {
        renderInfoStack.pop();
    }
    
    public static void adjustCameraPos(ActiveRenderInfo camera) {
        if (!renderInfoStack.isEmpty()) {
            RenderInfo currRenderInfo = renderInfoStack.peek();
            ((IECamera) camera).portal_setPos(currRenderInfo.cameraPos);
        }
    }
    
    public static void applyAdditionalTransformations(MatrixStack matrixStack) {
        for (RenderInfo renderInfo : renderInfoStack) {
            Matrix4f matrix = renderInfo.additionalTransformation;
            if (matrix != null) {
                matrixStack.getLast().getMatrix().mul(matrix);
                matrixStack.getLast().getNormal().mul(new Matrix3f(matrix));
            }
        }
    }
    
    /**
     * it's different from {@link PortalRendering#isRendering()}
     * when rendering cross portal third person view, this is true
     * but {@link PortalRendering#isRendering()} is false
     */
    public static boolean isRendering() {
        return !renderInfoStack.empty();
    }
    
    public static int getRenderingLayer() {
        return renderInfoStack.size();
    }
    
    // for example rendering portal B inside portal A will always have the same rendering description
    public static List<UUID> getRenderingDescription() {
        return renderInfoStack.stream()
            .map(RenderInfo::getDescription).collect(Collectors.toList());

//        UUID[] result = new UUID[renderInfoStack.size()];
//        for (int i = 0; i < result.length; i++) {
//            result[i] = renderInfoStack.get(i).getDescription();
//        }
//        return result;
    }
}
