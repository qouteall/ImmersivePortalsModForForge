package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.optifine_compatibility.ShaderCullingManager;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public class CrossPortalEntityRenderer {
    private static final Minecraft client = Minecraft.getInstance();
    
    //there is no weak hash set
    private static final WeakHashMap<Entity, Object> collidedEntities = new WeakHashMap<>();
    
    public static boolean isRendering = false;
    
    public static void init() {
        ModMain.postClientTickSignal.connect(CrossPortalEntityRenderer::onClientTick);
    }
    
    public static void cleanUp() {
        collidedEntities.clear();
    }
    
    private static void onClientTick() {
        collidedEntities.entrySet().removeIf(entry ->
            entry.getKey().removed ||
                ((IEEntity) entry.getKey()).getCollidingPortal() == null
        );
    }
    
    public static void onEntityTickClient(Entity entity) {
        if (entity instanceof Portal) {
            return;
        }
        
        Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
        if (collidingPortal != null) {
            collidedEntities.put(entity, null);
        }
    }
    
    public static void onBeginRenderingEntities(MatrixStack matrixStack) {
        if (PortalRendering.isRendering()) {
            PixelCuller.updateCullingPlaneInner(
                matrixStack, PortalRendering.getRenderingPortal(), false
            );
            PixelCuller.startCulling();
        }
    }
    
    public static void onEndRenderingEntities(MatrixStack matrixStack) {
        PixelCuller.endCulling();
        
        renderEntityProjections(matrixStack);
    }
    
    public static void beforeRenderingEntity(Entity entity, MatrixStack matrixStack) {
        if (!Global.correctCrossPortalEntityRendering) {
            return;
        }
        if (!PortalRendering.isRendering()) {
            if (collidedEntities.containsKey(entity)) {
                Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
                if (collidingPortal == null) {
                    //Helper.err("Colliding Portal Record Invalid " + entity);
                    return;
                }
                
                //draw already built triangles
                client.getRenderTypeBuffers().getBufferSource().finish();
                
                PixelCuller.updateCullingPlaneOuter(
                    matrixStack,
                    collidingPortal
                );
                PixelCuller.startCulling();
                if (OFInterface.isShaders.getAsBoolean()) {
                    ShaderCullingManager.update();
                }
            }
        }
    }
    
    public static void afterRenderingEntity(Entity entity) {
        if (!Global.correctCrossPortalEntityRendering) {
            return;
        }
        if (!PortalRendering.isRendering()) {
            if (collidedEntities.containsKey(entity)) {
                //draw it with culling in a separate draw call
                client.getRenderTypeBuffers().getBufferSource().finish();
                PixelCuller.endCulling();
            }
        }
    }
    
    //if an entity is in overworld but halfway through a nether portal
    //then it has a projection in nether
    private static void renderEntityProjections(MatrixStack matrixStack) {
        if (!Global.correctCrossPortalEntityRendering) {
            return;
        }
        collidedEntities.keySet().forEach(entity -> {
            Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
            if (collidingPortal == null) {
                //Helper.err("Colliding Portal Record Invalid " + entity);
                return;
            }
            if (collidingPortal instanceof Mirror) {
                //no need to render entity projection for mirrors
                return;
            }
            if (collidingPortal.rotation != null) {
                //currently cannot render entity projection through a rotating portal
                return;
            }
            if (collidingPortal.hasScaling()) {
                return;
            }
            RegistryKey<World> projectionDimension = collidingPortal.dimensionTo;
            if (client.world.func_234923_W_() == projectionDimension) {
                renderProjectedEntity(entity, collidingPortal, matrixStack);
            }
        });
    }
    
    public static boolean hasIntersection(
        Vector3d outerPlanePos, Vector3d outerPlaneNormal,
        Vector3d entityPos, Vector3d collidingPortalNormal
    ) {
        return entityPos.subtract(outerPlanePos).dotProduct(outerPlaneNormal) > 0.01 &&
            outerPlanePos.subtract(entityPos).dotProduct(collidingPortalNormal) > 0.01;
    }
    
    private static void renderProjectedEntity(
        Entity entity,
        Portal collidingPortal,
        MatrixStack matrixStack
    ) {
        if (PortalRendering.isRendering()) {
            Portal renderingPortal = PortalRendering.getRenderingPortal();
            //correctly rendering it needs two culling planes
            //use some rough check to work around
            
            if (!Portal.isFlippedPortal(renderingPortal, collidingPortal)) {
                Vector3d cameraPos = client.gameRenderer.getActiveRenderInfo().getProjectedView();
                
                boolean isHidden = cameraPos.subtract(collidingPortal.destination)
                    .dotProduct(collidingPortal.getContentDirection()) < 0;
                if (renderingPortal == collidingPortal || !isHidden) {
                    renderEntityRegardingPlayer(entity, collidingPortal, matrixStack);
                }
            }
        }
        else {
            PixelCuller.updateCullingPlaneInner(matrixStack, collidingPortal, false);
            PixelCuller.startCulling();
            renderEntityRegardingPlayer(entity, collidingPortal, matrixStack);
            PixelCuller.endCulling();
        }
    }
    
    private static void renderEntityRegardingPlayer(
        Entity entity,
        Portal transformingPortal,
        MatrixStack matrixStack
    ) {
        if (entity instanceof ClientPlayerEntity) {
            MyGameRenderer.renderPlayerItself(() -> {
                renderEntity(entity, transformingPortal, matrixStack);
            });
        }
        else {
            renderEntity(entity, transformingPortal, matrixStack);
        }
    }
    
    private static void renderEntity(
        Entity entity,
        Portal transformingPortal,
        MatrixStack matrixStack
    ) {
        Vector3d cameraPos = client.gameRenderer.getActiveRenderInfo().getProjectedView();
        
        ClientWorld newWorld = CGlobal.clientWorldLoader.getWorld(
            transformingPortal.dimensionTo
        );
        
        Vector3d oldEyePos = McHelper.getEyePos(entity);
        Vector3d oldLastTickEyePos = McHelper.getLastTickEyePos(entity);
        World oldWorld = entity.world;
        
        Vector3d newEyePos = transformingPortal.transformPoint(oldEyePos);
        
        if (PortalRendering.isRendering()) {
            Portal renderingPortal = PortalRendering.getRenderingPortal();
            if (!renderingPortal.isInside(newEyePos, -3)) {
                return;
            }
        }
        
        if (entity instanceof ClientPlayerEntity) {
            if (!Global.renderYourselfInPortal) {
                return;
            }
            
            //avoid rendering player too near and block view
            double dis = newEyePos.distanceTo(cameraPos);
            double valve = 0.5 + McHelper.lastTickPosOf(entity).distanceTo(entity.getPositionVec());
            if (dis < valve) {
                return;
            }
            else {
                //Helper.log("wow " + dis + " " + valve);
            }
        }
        
        McHelper.setEyePos(
            entity,
            newEyePos,
            transformingPortal.transformPoint(oldLastTickEyePos)
        );
        
        entity.world = newWorld;
        
        isRendering = true;
        OFInterface.updateEntityTypeForShader.accept(entity);
        IRenderTypeBuffer.Impl consumers = client.getRenderTypeBuffers().getBufferSource();
        ((IEWorldRenderer) client.worldRenderer).myRenderEntity(
            entity,
            cameraPos.x, cameraPos.y, cameraPos.z,
            RenderStates.tickDelta, matrixStack,
            consumers
        );
        //immediately invoke draw call
        consumers.finish();
        isRendering = false;
        
        McHelper.setEyePos(
            entity, oldEyePos, oldLastTickEyePos
        );
        entity.world = oldWorld;
    }
    
    public static boolean shouldRenderPlayerItself() {
        if (!Global.renderYourselfInPortal) {
            return false;
        }
        if (!PortalRendering.isRendering()) {
            return false;
        }
        if (client.renderViewEntity.world.func_234923_W_() == RenderStates.originalPlayerDimension) {
            return true;
        }
        return false;
    }
    
    public static boolean shouldRenderEntityNow(Entity entity) {
        if (OFInterface.isShadowPass.getAsBoolean()) {
            return true;
        }
        if (PortalRendering.isRendering()) {
            if (entity instanceof ClientPlayerEntity) {
                return shouldRenderPlayerItself();
            }
            Portal renderingPortal = PortalRendering.getRenderingPortal();
            Portal collidingPortal = ((IEEntity) entity).getCollidingPortal();
            if (collidingPortal != null) {
                if (!Portal.isReversePortal(collidingPortal, renderingPortal)) {
                    Vector3d cameraPos = PortalRenderer.client.gameRenderer.getActiveRenderInfo().getProjectedView();
                    
                    boolean isHidden = cameraPos.subtract(collidingPortal.getPositionVec())
                        .dotProduct(collidingPortal.getNormal()) < 0;
                    if (isHidden) {
                        return false;
                    }
                }
            }
            
            return renderingPortal.isInside(
                entity.getEyePosition(RenderStates.tickDelta), -0.01
            );
        }
        return true;
    }
}
