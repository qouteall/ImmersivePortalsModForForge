package com.qouteall.immersive_portals.render.context_management;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.ducks.IECamera;
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

public class RenderingHierarchy {
    public final ClientWorld world;
    public final Vector3d cameraPos;
    @Nullable
    public final Matrix4f additionalTransformation;
    @Nullable
    public final UUID description;
    public final int renderDistance;
    
    private static final Stack<RenderingHierarchy> renderInfoStack = new Stack<>();
    
    public RenderingHierarchy(
        ClientWorld world, Vector3d cameraPos,
        Matrix4f additionalTransformation, @Nullable UUID description
    ) {
        this(
            world, cameraPos, additionalTransformation, description,
            Minecraft.getInstance().gameSettings.renderDistanceChunks
        );
    }
    
    public RenderingHierarchy(
        ClientWorld world, Vector3d cameraPos,
        @Nullable Matrix4f additionalTransformation,
        @Nullable UUID description, int renderDistance
    ) {
        this.world = world;
        this.cameraPos = cameraPos;
        this.additionalTransformation = additionalTransformation;
        this.description = description;
        this.renderDistance = renderDistance;
    }
    
    public static void pushRenderInfo(RenderingHierarchy renderingHierarchy) {
        renderInfoStack.push(renderingHierarchy);
    }
    
    public static void popRenderInfo() {
        renderInfoStack.pop();
    }
    
    public static void adjustCameraPos(ActiveRenderInfo camera) {
        if (!renderInfoStack.isEmpty()) {
            RenderingHierarchy currRenderingHierarchy = renderInfoStack.peek();
            ((IECamera) camera).portal_setPos(currRenderingHierarchy.cameraPos);
        }
    }
    
    public static void applyAdditionalTransformations(MatrixStack matrixStack) {
        for (RenderingHierarchy renderingHierarchy : renderInfoStack) {
            Matrix4f matrix = renderingHierarchy.additionalTransformation;
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
            .map(renderInfo -> renderInfo.description).collect(Collectors.toList());
    }
}
