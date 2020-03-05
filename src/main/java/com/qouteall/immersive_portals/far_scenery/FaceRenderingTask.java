package com.qouteall.immersive_portals.far_scenery;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Streams;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import com.qouteall.immersive_portals.render.SecondaryFrameBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;

public class FaceRenderingTask {
    public static RenderScheduler scheduler = new RenderScheduler();
    
    //compose some small tasks into a big task
    //when running the composed task, firstly invoke preparation func
    //then invoke the small tasks
    //if a small task fails, invoke finish func and the composed task fails
    //when the composed task is invoked again it will rerun the unfinished small tasks
    public static MyTaskList.MyTask composeTask(
        Runnable taskPreparation,
        Runnable taskFinish,
        Iterator<MyTaskList.MyTask> subTasks,
        BooleanSupplier shouldCancelTask
    ) {
        PeekingIterator<MyTaskList.MyTask> subTaskIterator =
            Iterators.peekingIterator(subTasks);
        
        return () -> {
            if (shouldCancelTask.getAsBoolean()) {
                return true;
            }
            
            taskPreparation.run();
            
            try {
                for (; ; ) {
                    if (!subTaskIterator.hasNext()) {
                        return true;
                    }
                    MyTaskList.MyTask nextTask = subTaskIterator.peek();
                    boolean result = nextTask.runAndGetIsSucceeded();
                    if (result) {
                        subTaskIterator.next();
                    }
                    else {
                        return false;
                    }
                }
            }
            finally {
                taskFinish.run();
            }
        };
    }
    
    private static Minecraft mc = Minecraft.getInstance();
    
    public static MyTaskList.MyTask createFarSceneryRenderingTask(
        Vec3d cameraPos,
        DimensionType cameraDimension,
        double nearPlaneDistance,
        int farDistanceChunks,
        SecondaryFrameBuffer[] frameBuffersByFace
    ) {
        scheduler.onRenderLaunch();
        return composeTask(
            () -> {
                FSRenderingContext.isRenderingScenery = true;
                FSRenderingContext.cameraPos = cameraPos;
                FSRenderingContext.nearPlaneDistance = nearPlaneDistance;
                scheduler.onRenderPassStart();
            },
            () -> {
                FSRenderingContext.isRenderingScenery = false;
            },
            Arrays.stream(Direction.values())
                .map(direction -> createRenderFaceTask(
                    direction, frameBuffersByFace[direction.ordinal()],
                    cameraPos, nearPlaneDistance, farDistanceChunks,
                    scheduler
                )).iterator(),
            () -> mc.world.dimension.getType() != cameraDimension
        );
    }
    
    public static MyTaskList.MyTask createRenderFaceTask(
        Direction direction,
        SecondaryFrameBuffer frameBuffer,
        Vec3d cameraPos,
        double nearPlaneDistance,
        int farDistanceChunks,
        RenderScheduler scheduler
    ) {
        MatrixStack projectionMatrix = getPanoramaProjectionMatrix(farDistanceChunks * 16);
        MatrixStack modelViewMatrix = new MatrixStack();
        
        ActiveRenderInfo camera = createCamera(direction, cameraPos);
        
        modelViewMatrix.rotate(Vector3f.XP.rotationDegrees(camera.getPitch()));
        modelViewMatrix.rotate(Vector3f.YP.rotationDegrees(camera.getYaw() + 180.0F));
        
        ClippingHelperImpl frustum = new ClippingHelperImpl(
            modelViewMatrix.getLast().getMatrix(),
            projectionMatrix.getLast().getMatrix()
        );
        frustum.setCameraPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        
        frameBuffer.fb.bindFramebuffer(true);
        
        RenderSystem.clearColor(0, 1, 1, 0);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, true);
        
        mc.getFramebuffer().bindFramebuffer(true);
        
        List<ChunkRenderDispatcher.ChunkRender> sectionRenderList = SectionRenderListPropagator.getRenderSectionList(
            ((MyBuiltChunkStorage) ((IEWorldRenderer) mc.worldRenderer).getBuiltChunkStorage()),
            new BlockPos(cameraPos),
            farDistanceChunks,
            builtChunk -> frustum.isBoundingBoxInFrustum(builtChunk.boundingBox),
            direction.ordinal(),
            builtChunk -> shouldRenderInFarScenery(builtChunk)
        );
        
        return composeTask(
            () -> {
                frameBuffer.fb.bindFramebuffer(true);
                pushProjectionMatrix(projectionMatrix);
                FarSceneryRenderer.updateCullingEquation(nearPlaneDistance, direction);
            },
            () -> {
                mc.getFramebuffer().bindFramebuffer(true);
                popProjectionMatrix();
            },
            Arrays.stream(new RenderType[]{
                RenderType.getSolid(), RenderType.getCutoutMipped(),
                RenderType.getCutout(), RenderType.getTranslucent()
            }).map(renderLayer -> composeTask(
                () -> beginRenderLayer(renderLayer),
                () -> endRenderLayer(renderLayer),
                Streams.stream(
                    renderLayer == RenderType.getTranslucent() ?
                        new ReverseListIterator<>(sectionRenderList) : sectionRenderList.iterator()
                ).map(
                    builtChunk -> scheduler.limitTaskTime(() -> {
                        renderBuiltChunk(
                            builtChunk, renderLayer, cameraPos, modelViewMatrix
                        );
                        return true;
                    })
                ).iterator(),
                () -> false
            )).iterator(),
            () -> false
        );
        
    }
    
    private static void pushProjectionMatrix(MatrixStack matrixStack) {
        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.multMatrix(matrixStack.getLast().getMatrix());
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
    }
    
    private static void popProjectionMatrix() {
        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.popMatrix();
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
    }
    
    private static ActiveRenderInfo createCamera(Direction direction, Vec3d cameraPos) {
        ClientPlayerEntity player = mc.player;
        float oldYaw = player.rotationYaw;
        float oldPitch = player.rotationPitch;
        ActiveRenderInfo camera = new ActiveRenderInfo();
        FarSceneryRenderer.setPlayerRotation(direction, player);
        camera.update(
            player.world, player, false, false, 1
        );
        player.rotationYaw = oldYaw;
        player.rotationPitch = oldPitch;
        
        ((IECamera) camera).resetState(cameraPos, mc.world);
        
        return camera;
    }
    
    private static MatrixStack getPanoramaProjectionMatrix(float viewDistance) {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.getLast().getMatrix().setIdentity();
        
        matrixStack.getLast().getMatrix().mul(
            Matrix4f.perspective(
                90,
                1,
                0.05F,
                viewDistance
            )
        );
        return matrixStack;
    }
    
    private static VertexFormat getBlockVertexFormat() {
        return DefaultVertexFormats.BLOCK;
    }
    
    private static void beginRenderLayer(RenderType renderLayer) {
        renderLayer.setupRenderState();
    
        //TODO translucent sort
    }
    
    private static void endRenderLayer(RenderType renderLayer) {
        VertexBuffer.unbindBuffer();
        RenderSystem.clearCurrentColor();
        getBlockVertexFormat().clearBufferState();
        renderLayer.clearRenderState();
    }
    
    private static void renderBuiltChunk(
        ChunkRenderDispatcher.ChunkRender builtChunk,
        RenderType renderLayer,
        Vec3d cameraPos,
        MatrixStack matrixStack
    ) {
        if (builtChunk.needsUpdate()) {
            //builtChunk.scheduleRebuild(((IEWorldRenderer) mc.worldRenderer).getChunkBuilder());
            return;
        }
        if (builtChunk.getCompiledChunk().isLayerEmpty(renderLayer)) {
            return;
        }
        VertexBuffer vertexBuffer = builtChunk.getVertexBuffer(renderLayer);
        matrixStack.push();
        BlockPos blockPos = builtChunk.getPosition();
        matrixStack.translate(
            (double) blockPos.getX() - cameraPos.x,
            (double) blockPos.getY() - cameraPos.y,
            (double) blockPos.getZ() - cameraPos.z
        );
        vertexBuffer.bindBuffer();
        getBlockVertexFormat().setupBufferState(0L);
        vertexBuffer.draw(matrixStack.getLast().getMatrix(), 7);
        matrixStack.pop();
    }
    
    public static boolean shouldRenderInFarScenery(
        ChunkRenderDispatcher.ChunkRender builtChunk
    ) {
        Vec3d cameraPos = FSRenderingContext.cameraPos;
        double nearPlaneDistance = FSRenderingContext.nearPlaneDistance;
        AxisAlignedBB boundingBox = builtChunk.boundingBox;
        return Math.abs(boundingBox.minX - cameraPos.x) >= nearPlaneDistance ||
            Math.abs(boundingBox.maxX - cameraPos.x) >= nearPlaneDistance ||
            Math.abs(boundingBox.minY - cameraPos.y) >= nearPlaneDistance ||
            Math.abs(boundingBox.maxY - cameraPos.y) >= nearPlaneDistance ||
            Math.abs(boundingBox.minZ - cameraPos.z) >= nearPlaneDistance ||
            Math.abs(boundingBox.maxZ - cameraPos.z) >= nearPlaneDistance;
    }
    
    public static boolean shouldRenderInNearScenery(
        ChunkRenderDispatcher.ChunkRender builtChunk
    ) {
        Vec3d cameraPos = FSRenderingContext.cameraPos;
        double nearPlaneDistance = FSRenderingContext.nearPlaneDistance + 16;
        AxisAlignedBB boundingBox = builtChunk.boundingBox;
        return Math.abs(boundingBox.minX - cameraPos.x) <= nearPlaneDistance &&
            Math.abs(boundingBox.maxX - cameraPos.x) <= nearPlaneDistance &&
            Math.abs(boundingBox.minY - cameraPos.y) <= nearPlaneDistance &&
            Math.abs(boundingBox.maxY - cameraPos.y) <= nearPlaneDistance &&
            Math.abs(boundingBox.minZ - cameraPos.z) <= nearPlaneDistance &&
            Math.abs(boundingBox.maxZ - cameraPos.z) <= nearPlaneDistance;
    }
}
