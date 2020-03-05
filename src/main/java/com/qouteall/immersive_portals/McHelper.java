package com.qouteall.immersive_portals;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class McHelper {
    
    public static IEThreadedAnvilChunkStorage getIEStorage(DimensionType dimension) {
        return (IEThreadedAnvilChunkStorage) (
            (ServerChunkProvider) getServer().getWorld(dimension).getChunkProvider()
        ).chunkManager;
    }
    
    public static ArrayList<ServerPlayerEntity> getCopiedPlayerList() {
        return new ArrayList<>(getServer().getPlayerList().getPlayers());
    }
    
    public static List<ServerPlayerEntity> getRawPlayerList() {
        return getServer().getPlayerList().getPlayers();
    }
    
    public static Vec3d lastTickPosOf(Entity entity) {
        return new Vec3d(entity.prevPosX, entity.prevPosY, entity.prevPosZ);
    }
    
    public static MinecraftServer getServer() {
        return Helper.refMinecraftServer.get();
    }
    
    public static ServerWorld getOverWorldOnServer() {
        return getServer().getWorld(DimensionType.OVERWORLD);
    }
    
    public static void serverLog(
        ServerPlayerEntity player,
        String text
    ) {
        //Helper.log(text);
        
        player.sendMessage(new StringTextComponent(text));
    }
    
    public static AxisAlignedBB getChunkBoundingBox(ChunkPos chunkPos) {
        return new AxisAlignedBB(
            chunkPos.asBlockPos(),
            chunkPos.asBlockPos().add(16, 256, 16)
        );
    }
    
    public static long getServerGameTime() {
        return getOverWorldOnServer().getGameTime();
    }
    
    public static <T> void performSplitedFindingTaskOnServer(
        Iterator<T> iterator,
        Predicate<T> predicate,
        IntPredicate progressInformer,//return false to abort the task
        Consumer<T> onFound,
        Runnable onNotFound
    ) {
        final long timeValve = (1000000000L / 50);
        int[] countStorage = new int[1];
        countStorage[0] = 0;
        ModMain.serverTaskList.addTask(() -> {
            boolean shouldContinueRunning =
                progressInformer.test(countStorage[0]);
            if (!shouldContinueRunning) {
                return true;
            }
            long startTime = System.nanoTime();
            for (; ; ) {
                if (iterator.hasNext()) {
                    T next = iterator.next();
                    if (predicate.test(next)) {
                        onFound.accept(next);
                        return true;
                    }
                }
                else {
                    //finished searching
                    onNotFound.run();
                    return true;
                }
                countStorage[0] += 1;
    
                long currTime = System.nanoTime();
    
                if (currTime - startTime > timeValve) {
                    //suspend the task and retry it next tick
                    return false;
                }
            }
        });
    }
    
    public static <ENTITY extends Entity> Stream<ENTITY> getEntitiesNearby(
        World world,
        Vec3d center,
        Class<ENTITY> entityClass,
        double range
    ) {
        AxisAlignedBB box = new AxisAlignedBB(center, center).grow(range);
        return (Stream) world.getEntitiesWithinAABB(entityClass, box, e -> true).stream();
    }
    
    public static <ENTITY extends Entity> Stream<ENTITY> getEntitiesNearby(
        Entity center,
        Class<ENTITY> entityClass,
        double range
    ) {
        return getEntitiesNearby(
            center.world,
            center.getPositionVec(),
            entityClass,
            range
        );
    }
    
    public static void runWithTransformation(
        MatrixStack matrixStack,
        Runnable renderingFunc
    ) {
        transformationPush(matrixStack);
        renderingFunc.run();
        transformationPop();
    }
    
    public static void transformationPop() {
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.popMatrix();
    }
    
    public static void transformationPush(MatrixStack matrixStack) {
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.multMatrix(matrixStack.getLast().getMatrix());
    }
    
    public static List<GlobalTrackedPortal> getGlobalPortals(World world) {
        if (world.isRemote) {
            return CHelper.getClientGlobalPortal(world);
        }
        else {
            return GlobalPortalStorage.get(((ServerWorld) world)).data;
        }
    }
    
    public static Stream<Portal> getServerPortalsNearby(Entity center, double range) {
        List<GlobalTrackedPortal> globalPortals = GlobalPortalStorage.get(((ServerWorld) center.world)).data;
        Stream<Portal> nearbyPortals = McHelper.getEntitiesNearby(
            center,
            Portal.class,
            range
        );
        if (globalPortals == null) {
            return nearbyPortals;
        }
        else {
            return Streams.concat(
                globalPortals.stream().filter(
                    p -> p.getDistanceToNearestPointInPortal(center.getPositionVec()) < range * 2
                ),
                nearbyPortals
            );
        }
    }
    
    public static int getRenderDistanceOnServer() {
        return getIEStorage(DimensionType.OVERWORLD).getWatchDistance();
    }
    
    public static void setPosAndLastTickPos(
        Entity entity,
        Vec3d pos,
        Vec3d lastTickPos
    ) {
    
    
        //NOTE do not call entity.setPosition() because it may tick the entity
        entity.setRawPosition(pos.x, pos.y, pos.z);
        entity.lastTickPosX = lastTickPos.x;
        entity.lastTickPosY = lastTickPos.y;
        entity.lastTickPosZ = lastTickPos.z;
        entity.prevPosX = lastTickPos.x;
        entity.prevPosY = lastTickPos.y;
        entity.prevPosZ = lastTickPos.z;
    }
    
    public static double getVehicleY(Entity vehicle, Entity passenger) {
        return passenger.getPosY() - vehicle.getMountedYOffset() - passenger.getYOffset();
    }
    
    public static void adjustVehicle(Entity entity) {
        Entity vehicle = entity.getRidingEntity();
        if (vehicle == null) {
            return;
        }
    
        vehicle.setPosition(
            entity.getPosX(),
            getVehicleY(vehicle, entity),
            entity.getPosZ()
        );
    }
    
    public static void checkDimension(Entity entity) {
        if (entity.dimension != entity.world.dimension.getType()) {
            Helper.err(String.format(
                "Entity dimension field abnormal. Force corrected. %s %s %s",
                entity,
                entity.dimension,
                entity.world.dimension.getType()
            ));
            entity.dimension = entity.world.dimension.getType();
        }
    }
    
    public static Chunk getServerChunkIfPresent(
        DimensionType dimension,
        int x, int z
    ) {
        ChunkHolder chunkHolder_ = getIEStorage(dimension).getChunkHolder_(ChunkPos.asLong(x, z));
        if (chunkHolder_ == null) {
            return null;
        }
        return chunkHolder_.getChunkIfComplete();
    }
}
