package com.qouteall.immersive_portals;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.*;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.GL_NO_ERROR;

public class Helper {
    
    private static final Logger LOGGER = LogManager.getContext(
        Helper.class.getClassLoader(), false
    ).getLogger("Portal");
    
    public static void assertWithSideEffects(boolean cond) {
        //assert cond;
        if (!cond) {
            Helper.err("ASSERTION FAILED");
        }
    }
    
    //copied from Project class
    public static void doTransformation(FloatBuffer m, float[] in, float[] out) {
        for (int i = 0; i < 4; i++) {
            out[i] =
                in[0] * m.get(m.position() + 0 * 4 + i)
                    + in[1] * m.get(m.position() + 1 * 4 + i)
                    + in[2] * m.get(m.position() + 2 * 4 + i)
                    + in[3] * m.get(m.position() + 3 * 4 + i);
            
        }
    }
    
    //NOTE the w value is omitted
    public static Vec3d doTransformation(FloatBuffer m, Vec3d in) {
        float[] input = {(float) in.x, (float) in.y, (float) in.z, 1};
        float[] output = new float[4];
        doTransformation(m, input, output);
        return new Vec3d(output[0], output[1], output[2]);
    }
    
    public static final float[] IDENTITY_MATRIX =
        new float[]{
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f};
    
    private static void loadIdentityMatrix(FloatBuffer m) {
        int oldPos = m.position();
        m.put(IDENTITY_MATRIX);
        m.position(oldPos);
    }
    
    public static FloatBuffer getModelViewMatrix() {
        return getMatrix(GL11.GL_MODELVIEW_MATRIX);
    }
    
    public static FloatBuffer getProjectionMatrix() {
        return getMatrix(GL11.GL_PROJECTION_MATRIX);
    }
    
    public static FloatBuffer getTextureMatrix() {
        return getMatrix(GL11.GL_TEXTURE_MATRIX);
    }
    
    public static FloatBuffer getMatrix(int matrixId) {
        FloatBuffer temp = BufferUtils.createFloatBuffer(16);
        
        GL11.glGetFloatv(matrixId, temp);
        
        return temp;
    }
    
    //get the intersect point of a line and a plane
    //a line: p = lineCenter + t * lineDirection
    //get the t of the colliding point
    //normal and lineDirection have to be normalized
    public static double getCollidingT(
        Vec3d planeCenter,
        Vec3d planeNormal,
        Vec3d lineCenter,
        Vec3d lineDirection
    ) {
        return (planeCenter.subtract(lineCenter).dotProduct(planeNormal))
            /
            (lineDirection.dotProduct(planeNormal));
    }
    
    public static boolean isInFrontOfPlane(
        Vec3d pos,
        Vec3d planePos,
        Vec3d planeNormal
    ) {
        return pos.subtract(planePos).dotProduct(planeNormal) > 0;
    }
    
    public static Vec3d fallPointOntoPlane(
        Vec3d point,
        Vec3d planePos,
        Vec3d planeNormal
    ) {
        double t = getCollidingT(planePos, planeNormal, point, planeNormal);
        return point.add(planeNormal.scale(t));
    }
    
    public static Vec3i getUnitFromAxis(Direction.Axis axis) {
        return Direction.getFacingFromAxisDirection(
            axis,
            Direction.AxisDirection.POSITIVE
        ).getDirectionVec();
    }
    
    public static int getCoordinate(Vec3i v, Direction.Axis axis) {
        return axis.getCoordinate(v.getX(), v.getY(), v.getZ());
    }
    
    public static double getCoordinate(Vec3d v, Direction.Axis axis) {
        return axis.getCoordinate(v.x, v.y, v.z);
    }
    
    public static <A, B> Tuple<B, A> swaped(Tuple<A, B> p) {
        return new Tuple<>(p.getB(), p.getA());
    }
    
    public static <T> T uniqueOfThree(T a, T b, T c) {
        if (a.equals(b)) {
            return c;
        }
        else if (b.equals(c)) {
            return a;
        }
        else {
            assert a.equals(c);
            return b;
        }
    }
    
    public static BlockPos max(BlockPos a, BlockPos b) {
        return new BlockPos(
            Math.max(a.getX(), b.getX()),
            Math.max(a.getY(), b.getY()),
            Math.max(a.getZ(), b.getZ())
        );
    }
    
    public static BlockPos min(BlockPos a, BlockPos b) {
        return new BlockPos(
            Math.min(a.getX(), b.getX()),
            Math.min(a.getY(), b.getY()),
            Math.min(a.getZ(), b.getZ())
        );
    }
    
    public static Tuple<Direction.Axis, Direction.Axis> getAnotherTwoAxis(Direction.Axis axis) {
        switch (axis) {
            case X:
                return new Tuple<>(Direction.Axis.Y, Direction.Axis.Z);
            case Y:
                return new Tuple<>(Direction.Axis.Z, Direction.Axis.X);
            case Z:
                return new Tuple<>(Direction.Axis.X, Direction.Axis.Y);
        }
        throw new IllegalArgumentException();
    }
    
    public static BlockPos scale(Vec3i v, int m) {
        return new BlockPos(v.getX() * m, v.getY() * m, v.getZ() * m);
    }
    
    public static BlockPos divide(Vec3i v, int d) {
        return new BlockPos(v.getX() / d, v.getY() / d, v.getZ() / d);
    }
    
    public static Direction[] getAnotherFourDirections(Direction.Axis axisOfNormal) {
        Tuple<Direction.Axis, Direction.Axis> anotherTwoAxis = getAnotherTwoAxis(
            axisOfNormal
        );
        return new Direction[]{
            Direction.getFacingFromAxisDirection(
                anotherTwoAxis.getA(), Direction.AxisDirection.POSITIVE
            ),
            Direction.getFacingFromAxisDirection(
                anotherTwoAxis.getB(), Direction.AxisDirection.POSITIVE
            ),
            Direction.getFacingFromAxisDirection(
                anotherTwoAxis.getA(), Direction.AxisDirection.NEGATIVE
            ),
            Direction.getFacingFromAxisDirection(
                anotherTwoAxis.getB(), Direction.AxisDirection.NEGATIVE
            )
        };
    }
    
    public static IEThreadedAnvilChunkStorage getIEStorage(DimensionType dimension) {
        return (IEThreadedAnvilChunkStorage) (
            (ServerChunkProvider) Helper.getServer().getWorld(dimension).getChunkProvider()
        ).chunkManager;
    }
    
    public static BatchTestResult batchTest(
        Vec3d[] testObjs,
        Predicate<Vec3d> predicate
    ) {
        assert testObjs.length == 8;
        boolean firstResult = predicate.test(testObjs[0]);
        for (int i = 1; i < testObjs.length; i++) {
            boolean thisResult = predicate.test(testObjs[i]);
            if (thisResult != firstResult) {
                return BatchTestResult.both;
            }
        }
        return firstResult ? BatchTestResult.all_true : BatchTestResult.all_false;
    }
    
    public static ArrayList<ServerPlayerEntity> getCopiedPlayerList() {
        return new ArrayList<>(getServer().getPlayerList().getPlayers());
    }
    
    public static String dimensionTypeToString(DimensionType this_) {
        return Registry.DIMENSION_TYPE.getKey(this_).toString();
    }
    
    public static class SimpleBox<T> {
        public T obj;
        
        public SimpleBox(T obj) {
            this.obj = obj;
        }
    }
    
    //@Nullable
    public static <T> T getLastSatisfying(Stream<T> stream, Predicate<T> predicate) {
        SimpleBox<T> box = new SimpleBox<T>(null);
        stream.filter(curr -> {
            if (predicate.test(curr)) {
                box.obj = curr;
                return false;
            }
            else {
                return true;
            }
        }).findFirst();
        return box.obj;
    }
    
    public interface CallableWithoutException<T> {
        public T run();
    }
    
    public static Vec3d lastTickPosOf(Entity entity) {
        return new Vec3d(entity.prevPosX, entity.prevPosY, entity.prevPosZ);
    }
    
    public static Vec3d interpolatePos(Entity entity, float partialTicks) {
        Vec3d currPos = entity.getPositionVec();
        Vec3d lastTickPos = lastTickPosOf(entity);
        return lastTickPos.add(currPos.subtract(lastTickPos).scale(partialTicks));
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
    
    public static WeakReference<MinecraftServer> refMinecraftServer;
    
    public static MinecraftServer getServer() {
        return refMinecraftServer.get();
    }
    
    public static Runnable noException(Callable func) {
        return () -> {
            try {
                func.call();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
    
    public static <MSG> void sendToServer(MSG message) {
        assert false;
    }
    
    public static <MSG> void sendToPlayer(ServerPlayerEntity player, MSG message) {
        assert false;
    }
    
    public static void checkGlError() {
        int errorCode = GL11.glGetError();
        if (errorCode != GL_NO_ERROR) {
            Helper.err("OpenGL Error" + errorCode);
            new Throwable().printStackTrace();
        }
    }
    
    public static ServerWorld getOverWorldOnServer() {
        return getServer().getWorld(DimensionType.OVERWORLD);
    }
    
    public static void doNotEatExceptionMessage(
        Runnable func
    ) {
        try {
            func.run();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    //sometimes it catches all types of exception
    //and the exception message will be hided
    //use this to avoid that
    public static <T> T doNotEatExceptionMessage(
        Supplier<T> func
    ) {
        try {
            return func.get();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    public static void serverLog(
        ServerPlayerEntity player,
        String text
    ) {
        //Helper.log(text);
        
        player.sendMessage(new StringTextComponent(text));
    }
    
    public static <T> String myToString(
        Stream<T> stream
    ) {
        StringBuilder stringBuilder = new StringBuilder();
        stream.forEach(obj -> {
            stringBuilder.append(obj.toString());
            stringBuilder.append('\n');
        });
        return stringBuilder.toString();
    }
    
    //NOTE this is not concatenation, it's composing
    public static <A, B> Stream<Tuple<A, B>> composeTwoStreamsWithEqualLength(
        Stream<A> a,
        Stream<B> b
    ) {
        Iterator<A> aIterator = a.iterator();
        Iterator<B> bIterator = b.iterator();
        Iterator<Tuple<A, B>> iterator = new Iterator<Tuple<A, B>>() {
            
            @Override
            public boolean hasNext() {
                assert aIterator.hasNext() == bIterator.hasNext();
                return aIterator.hasNext();
            }
            
            @Override
            public Tuple<A, B> next() {
                return new Tuple<>(aIterator.next(), bIterator.next());
            }
        };
        
        return Streams.stream(iterator);
    }
    
    public static void log(Object str) {
        LOGGER.info(str);
        //System.out.println(str);
    }
    
    public static void err(Object str) {
        LOGGER.error(str);
        //System.err.println(str);
    }
    
    public static Vec3d[] eightVerticesOf(AxisAlignedBB box) {
        return new Vec3d[]{
            new Vec3d(box.minX, box.minY, box.minZ),
            new Vec3d(box.minX, box.minY, box.maxZ),
            new Vec3d(box.minX, box.maxY, box.minZ),
            new Vec3d(box.minX, box.maxY, box.maxZ),
            new Vec3d(box.maxX, box.minY, box.minZ),
            new Vec3d(box.maxX, box.minY, box.maxZ),
            new Vec3d(box.maxX, box.maxY, box.minZ),
            new Vec3d(box.maxX, box.maxY, box.maxZ)
        };
    }
    
    public static void putVec3d(CompoundNBT compoundTag, String name, Vec3d vec3d) {
        compoundTag.putDouble(name + "X", vec3d.x);
        compoundTag.putDouble(name + "Y", vec3d.y);
        compoundTag.putDouble(name + "Z", vec3d.z);
    }
    
    public static Vec3d getVec3d(CompoundNBT compoundTag, String name) {
        return new Vec3d(
            compoundTag.getDouble(name + "X"),
            compoundTag.getDouble(name + "Y"),
            compoundTag.getDouble(name + "Z")
        );
    }
    
    public static void putVec3i(CompoundNBT compoundTag, String name, Vec3i vec3i) {
        compoundTag.putInt(name + "X", vec3i.getX());
        compoundTag.putInt(name + "Y", vec3i.getY());
        compoundTag.putInt(name + "Z", vec3i.getZ());
    }
    
    public static BlockPos getVec3i(CompoundNBT compoundTag, String name) {
        return new BlockPos(
            compoundTag.getInt(name + "X"),
            compoundTag.getInt(name + "Y"),
            compoundTag.getInt(name + "Z")
        );
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
    
    public static <T> void compareOldAndNew(
        Set<T> oldSet,
        Set<T> newSet,
        Consumer<T> forRemoved,
        Consumer<T> forAdded
    ) {
        oldSet.stream().filter(
            e -> !newSet.contains(e)
        ).forEach(
            forRemoved
        );
        newSet.stream().filter(
            e -> !oldSet.contains(e)
        ).forEach(
            forAdded
        );
    }
    
    public static long getServerGameTime() {
        return getOverWorldOnServer().getGameTime();
    }
    
    public static long secondToNano(double second) {
        return (long) (second * 1000000000L);
    }
    
    public enum BatchTestResult {
        all_true,
        all_false,
        both
    }
    
    public static IntegerAABBInclusive expandArea(
        IntegerAABBInclusive originalArea,
        Predicate<BlockPos> predicate,
        Direction direction
    ) {
        IntegerAABBInclusive currentBox = originalArea;
        for (int i = 1; i < 42; i++) {
            IntegerAABBInclusive expanded = currentBox.getExpanded(direction, 1);
            if (expanded.getSurfaceLayer(direction).stream().allMatch(predicate)) {
                currentBox = expanded;
            }
            else {
                return currentBox;
            }
        }
        return currentBox;
    }
    
    
    public static Vec3d getBoxSize(AxisAlignedBB box) {
        return new Vec3d(box.getXSize(), box.getYSize(), box.getZSize());
    }
    
    public static AxisAlignedBB getBoxSurface(AxisAlignedBB box, Direction direction) {
        double size = getCoordinate(getBoxSize(box), direction.getAxis());
        Vec3d shrinkVec = new Vec3d(direction.getDirectionVec()).scale(size);
        return box.contract(shrinkVec.x, shrinkVec.y, shrinkVec.z);
    }
    
    public static Tuple<Direction, Direction> getPerpendicularDirections(Direction facing) {
        Tuple<Direction.Axis, Direction.Axis> axises = getAnotherTwoAxis(facing.getAxis());
        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            axises = new Tuple<>(axises.getB(), axises.getA());
        }
        return new Tuple<>(
            Direction.getFacingFromAxis(Direction.AxisDirection.POSITIVE, axises.getA()),
            Direction.getFacingFromAxis(Direction.AxisDirection.POSITIVE, axises.getB())
        );
    }
    
    
    public static <A, B> B reduceWithDifferentType(
        B start,
        Stream<A> stream,
        BiFunction<B, A, B> func
    ) {
        SimpleBox<B> bBox = new SimpleBox<>(start);
        stream.forEach(a -> {
            bBox.obj = func.apply(bBox.obj, a);
        });
        return bBox.obj;
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
