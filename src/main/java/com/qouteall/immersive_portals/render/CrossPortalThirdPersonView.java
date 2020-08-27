package com.qouteall.immersive_portals.render;

import com.mojang.datafixers.util.Pair;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.PehkuiInterface;
import com.qouteall.immersive_portals.commands.PortalCommand;
import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.RenderInfo;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;

public class CrossPortalThirdPersonView {
    public static final Minecraft client = Minecraft.getInstance();
    
    // if rendered, return true
    public static boolean renderCrossPortalThirdPersonView() {
        if (!isThirdPerson()) {
            return false;
        }
        
        Entity cameraEntity = client.renderViewEntity;
        
        ActiveRenderInfo resuableCamera = new ActiveRenderInfo();
        float cameraY = ((IECamera) RenderStates.originalCamera).getCameraY();
        ((IECamera) resuableCamera).setCameraY(cameraY, cameraY);
        resuableCamera.update(
            client.world, cameraEntity,
            true,
            isFrontView(),
            RenderStates.tickDelta
        );
        Vector3d normalCameraPos = resuableCamera.getProjectedView();
        
        resuableCamera.update(
            client.world, cameraEntity,
            false, false, RenderStates.tickDelta
        );
        Vector3d playerHeadPos = resuableCamera.getProjectedView();
        
        Pair<Portal, Vector3d> portalHit = PortalCommand.raytracePortals(
            client.world, playerHeadPos, normalCameraPos, true
        ).orElse(null);
        
        if (portalHit == null) {
            return false;
        }
        
        Portal portal = portalHit.getFirst();
        Vector3d hitPos = portalHit.getSecond();
    
        double distance = getThirdPersonMaxDistance();
        
        Vector3d thirdPersonPos = normalCameraPos.subtract(playerHeadPos).normalize()
            .scale(distance).add(playerHeadPos);
        
        if (!portal.isInteractable()) {
            return false;
        }
        
        Vector3d renderingCameraPos = getThirdPersonCameraPos(thirdPersonPos, portal, hitPos);
        ((IECamera) RenderStates.originalCamera).portal_setPos(renderingCameraPos);
        
        
        RenderInfo renderInfo = new RenderInfo(
            CGlobal.clientWorldLoader.getWorld(portal.dimensionTo),
            renderingCameraPos,
            PortalRenderer.getAdditionalCameraTransformation(portal),
            portal
        );
        
        CGlobal.renderer.invokeWorldRendering(renderInfo);
        
        return true;
    }
    
    private static boolean isFrontView() {
        return client.gameSettings.func_243230_g().func_243193_b();
    }
    
    private static boolean isThirdPerson() {
        return !client.gameSettings.func_243230_g().func_243192_a();
    }
    
    /**
     * {@link Camera#update(BlockView, Entity, boolean, boolean, float)}
     */
    private static Vector3d getThirdPersonCameraPos(Vector3d endPos, Portal portal, Vector3d startPos) {
        Vector3d rtStart = portal.transformPoint(startPos);
        Vector3d rtEnd = portal.transformPoint(endPos);
        BlockRayTraceResult blockHitResult = portal.getDestinationWorld().rayTraceBlocks(
            new RayTraceContext(
                rtStart,
                rtEnd,
                RayTraceContext.BlockMode.VISUAL,
                RayTraceContext.FluidMode.NONE,
                client.renderViewEntity
            )
        );
        
        if (blockHitResult == null) {
            return rtStart.add(rtEnd.subtract(rtStart).normalize().scale(
                getThirdPersonMaxDistance()
            ));
        }
        
        return blockHitResult.getHitVec();
    }
    
    private static double getThirdPersonMaxDistance() {
        return 4.0d * PehkuiInterface.getScale.apply(Minecraft.getInstance().player);
    }

//    private static Vec3d getThirdPersonCameraPos(Portal portalHit, Camera resuableCamera) {
//        return CHelper.withWorldSwitched(
//            client.cameraEntity,
//            portalHit,
//            () -> {
//                World destinationWorld = portalHit.getDestinationWorld();
//                resuableCamera.update(
//                    destinationWorld,
//                    client.cameraEntity,
//                    true,
//                    isInverseView(),
//                    RenderStates.tickDelta
//                );
//                return resuableCamera.getPos();
//            }
//        );
//    }
}
