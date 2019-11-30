package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class McHelper {
    public static CompoundNBT writeEntityWithId(Entity entity) {
        return entity.serializeNBT();
    }
    
    @Nullable
    public static Entity readEntity(CompoundNBT tag, World world) {
        return EntityType.byKey(tag.getString("id")).map(
            entityType -> {
                Entity e = entityType.create(world);
                e.read(tag);
                return e;
            }
        ).orElse(null);
    }
    
    public static IEThreadedAnvilChunkStorage getIEStorage(DimensionType dimension) {
        return (IEThreadedAnvilChunkStorage) (
            (ServerChunkProvider) getServer().getWorld(dimension).getChunkProvider()
        ).chunkManager;
    }
    
    public static ArrayList<ServerPlayerEntity> getCopiedPlayerList() {
        return new ArrayList<>(getServer().getPlayerList().getPlayers());
    }
    
    public static void setPosAndLastTickPos(
        Entity entity,
        Vec3d pos,
        Vec3d lastTickPos
    ) {
        
        
        //NOTE do not call entity.setPosition() because it may tick the entity
        
        entity.posX = pos.x;
        entity.posY = pos.y;
        entity.posZ = pos.z;
        entity.lastTickPosX = lastTickPos.x;
        entity.lastTickPosY = lastTickPos.y;
        entity.lastTickPosZ = lastTickPos.z;
        entity.prevPosX = lastTickPos.x;
        entity.prevPosY = lastTickPos.y;
        entity.prevPosZ = lastTickPos.z;
    }
    
    public static MinecraftServer getServer() {
        return Helper.refMinecraftServer.get();
    }
    
    public static <MSG> void sendToServer(MSG message) {
        assert false;
    }
    
    public static <MSG> void sendToPlayer(ServerPlayerEntity player, MSG message) {
        assert false;
    }
    
    public static void serverLog(
        ServerPlayerEntity player,
        String text
    ) {
        //Helper.log(text);
        
        player.sendMessage(new StringTextComponent(text));
    }
    
    public static <ENTITY extends Entity> Stream<ENTITY> getEntitiesNearby(
        World world,
        Vec3d center,
        Class<ENTITY> entityClass,
        double range
    ) {
        AxisAlignedBB box = new AxisAlignedBB(center, center).grow(range);
        return (Stream) world.getEntitiesWithinAABB(entityClass, box).stream();
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
    
    public static AxisAlignedBB getChunkBoundingBox(ChunkPos chunkPos) {
        return new AxisAlignedBB(
            chunkPos.asBlockPos(),
            chunkPos.asBlockPos().add(16, 256, 16)
        );
    }
    
    public static long getServerGameTime() {
        return Helper.getOverWorldOnServer().getGameTime();
    }
    
    public static <T> void performSplitedFindingTaskOnServer(
        Iterator<T> iterator,
        Predicate<T> predicate,
        IntPredicate progressInformer,//return false to abort the task
        Consumer<T> onFound,
        Runnable onNotFound
    ) {
        final long timeValve = (1000000000L / 40);
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
}
