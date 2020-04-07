package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEMatrix4f;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Quaternion;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TransformationManager {
    public static Quaternion inertialRotation;
    private static long interpolationStartTime = 0;
    private static long interpolationEndTime = 1;
    public static Quaternion portalRotation;
    
    public static void processTransformation(ActiveRenderInfo camera, MatrixStack matrixStack) {
        matrixStack.getLast().getMatrix().setIdentity();
        matrixStack.getLast().getNormal().setIdentity();
        
        Quaternion cameraRotation = getCameraRotation(camera.getPitch(), camera.getYaw());
        Quaternion finalRotation = getFinalRotation(cameraRotation);
        
        matrixStack.rotate(finalRotation);
        
        CGlobal.renderer.applyAdditionalTransformations(matrixStack);
        
        //applyMirrorTransformation(camera, matrixStack);
        
    }
    
    public static boolean isAnimationRunning() {
        double progress = (MyRenderHelper.renderStartNanoTime - interpolationStartTime) /
            ((double) interpolationEndTime - interpolationStartTime);
        
        return progress >= -0.1 && progress <= 1.1;
    }
    
    public static Quaternion getFinalRotation(Quaternion cameraRotation) {
        double progress = (MyRenderHelper.renderStartNanoTime - interpolationStartTime) /
            ((double) interpolationEndTime - interpolationStartTime);
        
        if (progress < 0 || progress >= 1) {
            return cameraRotation;
        }

//        if (inertialRotation != null) {
//
//            if (Helper.isClose(inertialRotation, cameraRotation, 0.000001f)) {
//                inertialRotation = null;
//                return cameraRotation;
//            }
//
//            inertialRotation = Helper.interpolateQuaternion(
//                inertialRotation, cameraRotation, 0.04f
//            );
//            return inertialRotation;
//        }
//        else {
//            return cameraRotation;
//        }
        
        progress = mapProgress(progress);
        
        return Helper.interpolateQuaternion(
            Helper.ortholize(inertialRotation),
            Helper.ortholize(cameraRotation.copy()),
            (float) progress
        );
    }
    
    private static double mapProgress(double progress) {
//        return progress;
        return Math.sin(progress * (Math.PI / 2));
//        return Math.sqrt(1 - (1 - progress) * (1 - progress));
    }
    
    public static Quaternion getCameraRotation(float pitch, float yaw) {
        Quaternion cameraRotation = Vector3f.XP.rotationDegrees(pitch);
        cameraRotation.multiply(
            Vector3f.YP.rotationDegrees(yaw + 180.0F)
        );
        return cameraRotation;
    }
    
    /**
     * Entity#getRotationVector(float, float)
     */
    private static float getYawFromViewVector(Vec3d viewVector) {
        double lx = viewVector.x;
        double lz = viewVector.z;
        double len = Math.sqrt(lx * lx + lz * lz);
        lx /= len;
        lz /= len;
        
        if (lz >= 0) {
            return (float) -Math.asin(lx) / 0.017453292F;
        }
        else {
            if (lx > 0) {
                return (float) -Math.acos(lz) / 0.017453292F;
            }
            else {
                return (float) Math.acos(lz) / 0.017453292F;
            }
        }
    }
    
    private static float getPitchFromViewVector(Vec3d viewVector) {
        return (float) -Math.asin(viewVector.y) / 0.017453292F;
    }
    
    public static void onClientPlayerTeleported(
        Portal portal
    ) {
        if (portal.rotation != null) {
            Minecraft client = Minecraft.getInstance();
            ClientPlayerEntity player = client.player;
            
            Quaternion currentCameraRotation =
                getFinalRotation(getCameraRotation(player.rotationPitch, player.rotationYaw));
            
            Quaternion visualRotation =
                currentCameraRotation.copy();
            Quaternion b = portal.rotation.copy();
            b.conjugate();
            visualRotation.multiply(b);
            
            Vec3d oldViewVector = player.getLook(MyRenderHelper.partialTicks);
            Vec3d newViewVector = portal.transformLocalVec(oldViewVector);
            
            player.rotationYaw = getYawFromViewVector(newViewVector);
            player.prevRotationYaw = player.rotationYaw;
            player.rotationPitch = getPitchFromViewVector(newViewVector);
            player.prevRotationPitch = player.rotationPitch;
            
            player.renderArmYaw = player.rotationYaw;
            player.renderArmPitch = player.rotationPitch;
            
            player.prevRenderArmYaw = player.renderArmYaw;
            player.prevRenderArmPitch = player.renderArmPitch;
            
            Quaternion newCameraRotation = getCameraRotation(player.rotationPitch, player.rotationYaw);
            
            if (!Helper.isClose(newCameraRotation, visualRotation, 0.001f)) {
                inertialRotation = visualRotation;
                interpolationStartTime = MyRenderHelper.renderStartNanoTime;
                interpolationEndTime = interpolationStartTime +
                    Helper.secondToNano(1);
            }
            
            updateCamera(client);
        }
    }
    
    private static void updateCamera(Minecraft client) {
        ActiveRenderInfo camera = client.gameRenderer.getActiveRenderInfo();
        camera.update(
            client.world,
            client.player,
            client.gameSettings.thirdPersonView > 0,
            client.gameSettings.thirdPersonView == 2,
            MyRenderHelper.partialTicks
        );
    }
    
    public static Matrix4f getMirrorTransformation(Vec3d normal) {
        float x = (float) normal.x;
        float y = (float) normal.y;
        float z = (float) normal.z;
        float[] arr =
            new float[]{
                1 - 2 * x * x, 0 - 2 * x * y, 0 - 2 * x * z, 0,
                0 - 2 * y * x, 1 - 2 * y * y, 0 - 2 * y * z, 0,
                0 - 2 * z * x, 0 - 2 * z * y, 1 - 2 * z * z, 0,
                0, 0, 0, 1
            };
        Matrix4f matrix = new Matrix4f();
        ((IEMatrix4f) (Object) matrix).loadFromArray(arr);
        return matrix;
    }
}
