package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEMatrix4f;
import com.qouteall.immersive_portals.my_util.DQuaternion;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import com.qouteall.immersive_portals.render.context_management.WorldRenderInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TransformationManager {
    
    private static DQuaternion interpolationStart;
    private static DQuaternion lastCameraRotation;
    
    private static long interpolationStartTime = 0;
    private static long interpolationEndTime = 1;
    
    public static final Minecraft client = Minecraft.getInstance();
    
    public static boolean isIsometricView = false;
    public static float isometricViewLength = 50;
    
    public static void processTransformation(ActiveRenderInfo camera, MatrixStack matrixStack) {
        if (isAnimationRunning()) {
            // override vanilla camera transformation
            matrixStack.getLast().getMatrix().setIdentity();
            matrixStack.getLast().getNormal().setIdentity();
            
            DQuaternion cameraRotation = DQuaternion.getCameraRotation(camera.getPitch(), camera.getYaw());
            
            DQuaternion finalRotation = getFinalRotation(cameraRotation);
            
            matrixStack.rotate(finalRotation.toMcQuaternion());
        }
        
        WorldRenderInfo.applyAdditionalTransformations(matrixStack);
        
    }
    
    public static boolean isAnimationRunning() {
        if (interpolationStartTime == 0) {
            return false;
        }
        
        double progress = (RenderStates.renderStartNanoTime - interpolationStartTime) /
            ((double) interpolationEndTime - interpolationStartTime);
        
        return progress >= -0.1 && progress <= 1.1;
    }
    
    public static DQuaternion getFinalRotation(DQuaternion cameraRotation) {
        double progress = (RenderStates.renderStartNanoTime - interpolationStartTime) /
            ((double) interpolationEndTime - interpolationStartTime);
        
        if (progress < 0 || progress >= 1) {
            return cameraRotation;
        }
        
        progress = mapProgress(progress);
        
        DQuaternion cameraRotDelta = cameraRotation.hamiltonProduct(lastCameraRotation.getConjugated());
        interpolationStart = interpolationStart.hamiltonProduct(cameraRotDelta);
        
        lastCameraRotation = cameraRotation;
        
        return DQuaternion.interpolate(
            interpolationStart,
            cameraRotation,
            progress
        );
    }
    
    public static double mapProgress(double progress) {
//        return progress;
        return Math.sin(progress * (Math.PI / 2));
//        return Math.sqrt(1 - (1 - progress) * (1 - progress));
    }
    
    public static void onClientPlayerTeleported(
        Portal portal
    ) {
        if (portal.rotation != null) {
            ClientPlayerEntity player = client.player;
            
            DQuaternion currentCameraRotation = DQuaternion.getCameraRotation(player.rotationPitch, player.rotationYaw);
            DQuaternion currentCameraRotationInterpolated = getFinalRotation(currentCameraRotation);
            
            DQuaternion rotationThroughPortal =
                currentCameraRotationInterpolated.hamiltonProduct(
                    DQuaternion.fromMcQuaternion(portal.rotation).getConjugated()
                );
            
            Vector3d oldViewVector = player.getLook(RenderStates.tickDelta);
            Vector3d newViewVector;
            
            Tuple<Double, Double> pitchYaw = DQuaternion.getPitchYawFromRotation(rotationThroughPortal);
            
            player.rotationYaw = (float) (double) (pitchYaw.getB());
            player.rotationPitch = (float) (double) (pitchYaw.getA());
            
            if (player.rotationPitch > 90) {
                player.rotationPitch = 90 - (player.rotationPitch - 90);
            }
            else if (player.rotationPitch < -90) {
                player.rotationPitch = -90 + (-90 - player.rotationPitch);
            }
            
            player.prevRotationYaw = player.rotationYaw;
            player.prevRotationPitch = player.rotationPitch;
            player.renderArmYaw = player.rotationYaw;
            player.renderArmPitch = player.rotationPitch;
            player.prevRenderArmYaw = player.renderArmYaw;
            player.prevRenderArmPitch = player.renderArmPitch;
            
            DQuaternion newCameraRotation = DQuaternion.getCameraRotation(player.rotationPitch, player.rotationYaw);
            
            if (!DQuaternion.isClose(newCameraRotation, rotationThroughPortal, 0.001f)) {
                interpolationStart = rotationThroughPortal;
                lastCameraRotation = newCameraRotation;
                interpolationStartTime = RenderStates.renderStartNanoTime;
                interpolationEndTime = interpolationStartTime +
                    Helper.secondToNano(getAnimationDurationSeconds());
            }
            
            updateCamera(client);
        }
    }
    
    private static double getAnimationDurationSeconds() {
        return 1;
    }
    
    private static void updateCamera(Minecraft client) {
        ActiveRenderInfo camera = client.gameRenderer.getActiveRenderInfo();
        camera.update(
            client.world,
            client.player,
            !client.gameSettings.func_243230_g().func_243192_a(),
            client.gameSettings.func_243230_g().func_243193_b(),
            RenderStates.tickDelta
        );
    }
    
    public static Matrix4f getMirrorTransformation(Vector3d normal) {
        float x = (float) normal.x;
        float y = (float) normal.y;
        float z = (float) normal.z;
        float[] arr = new float[]{
            1 - 2 * x * x, 0 - 2 * x * y, 0 - 2 * x * z, 0,
            0 - 2 * y * x, 1 - 2 * y * y, 0 - 2 * y * z, 0,
            0 - 2 * z * x, 0 - 2 * z * y, 1 - 2 * z * z, 0,
            0, 0, 0, 1
        };
        Matrix4f matrix = new Matrix4f();
        ((IEMatrix4f) (Object) matrix).loadFromArray(arr);
        return matrix;
    }
    
    // https://docs.microsoft.com/en-us/windows/win32/opengl/glortho
    public static Matrix4f getIsometricProjection() {
        int w = client.getMainWindow().getFramebufferWidth();
        int h = client.getMainWindow().getFramebufferHeight();
        
        float wView = (isometricViewLength / h) * w;
        
        float near = -2000;
        float far = 2000;
        
        float left = -wView / 2;
        float right = wView / 2;
        
        float top = isometricViewLength / 2;
        float bottom = -isometricViewLength / 2;
        
        float[] arr = new float[]{
            2.0f / (right - left), 0, 0, -(right + left) / (right - left),
            0, 2.0f / (top - bottom), 0, -(top + bottom) / (top - bottom),
            0, 0, -2.0f / (far - near), -(far + near) / (far - near),
            0, 0, 0, 1
        };
        Matrix4f m1 = new Matrix4f();
        ((IEMatrix4f) (Object) m1).loadFromArray(arr);
        
        return m1;
    }
    
    @OnlyIn(Dist.CLIENT)
    public static class RemoteCallables {
        public static void enableIsometricView(float viewLength) {
            isometricViewLength = viewLength;
            isIsometricView = true;
            
            client.renderChunksMany = false;
        }
        
        public static void disableIsometricView() {
            isIsometricView = false;
            
            client.renderChunksMany = true;
        }
    }
    
    // isometric is equivalent to the camera being in infinitely far place
    public static Vector3d getIsometricAdjustedCameraPos() {
        ActiveRenderInfo camera = client.gameRenderer.getActiveRenderInfo();
        return getIsometricAdjustedCameraPos(camera);
    }
    
    public static Vector3d getIsometricAdjustedCameraPos(ActiveRenderInfo camera) {
        Vector3d cameraPos = camera.getProjectedView();
        
        if (!isIsometricView) {
            return cameraPos;
        }
        
        Quaternion rotation = camera.getRotation();
        Vector3f vec = new Vector3f(0, 0, client.gameSettings.renderDistanceChunks * -10);
        vec.transform(rotation);
        
        return cameraPos.add(vec.getX(), vec.getY(), vec.getZ());
    }
}
