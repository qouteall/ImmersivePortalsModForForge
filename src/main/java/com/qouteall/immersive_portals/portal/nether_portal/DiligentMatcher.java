package com.qouteall.immersive_portals.portal.nether_portal;

import com.google.common.math.IntMath;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntBox;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Orientation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiligentMatcher {
    public static class IntMatrix3 {
        public final Vector3i x;
        public final Vector3i y;
        public final Vector3i z;
        
        public IntMatrix3(Vector3i x, Vector3i y, Vector3i z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public IntMatrix3(Orientation t) {
            Direction d1 = t.func_235530_a_(Direction.getFacingFromAxisDirection(Direction.Axis.X, Direction.AxisDirection.POSITIVE));
            Direction d2 = t.func_235530_a_(Direction.getFacingFromAxisDirection(Direction.Axis.Y, Direction.AxisDirection.POSITIVE));
            Direction d3 = t.func_235530_a_(Direction.getFacingFromAxisDirection(Direction.Axis.Z, Direction.AxisDirection.POSITIVE));
            x = d1.getDirectionVec();
            y = d2.getDirectionVec();
            z = d3.getDirectionVec();
        }
        
        public boolean isRotationTransformation() {
            double r = Vector3d.func_237491_b_(x).crossProduct(Vector3d.func_237491_b_(y)).dotProduct(Vector3d.func_237491_b_(z));
            
            return r > 0;
        }
        
        // p * m  p is horizontal vector
        public BlockPos transform(Vector3i p) {
            return Helper.scale(x, p.getX())
                .add(Helper.scale(y, p.getY()))
                .add(Helper.scale(z, p.getZ()));
        }
        
        public IntMatrix3 multiply(IntMatrix3 m) {
            return new IntMatrix3(
                m.transform(x),
                m.transform(y),
                m.transform(z)
            );
        }
        
        public Direction transformDirection(Direction direction) {
            BlockPos vec = transform(direction.getDirectionVec());
            return Direction.byLong(vec.getX(), vec.getY(), vec.getZ());
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IntMatrix3 that = (IntMatrix3) o;
            return x.equals(that.x) &&
                y.equals(that.y) &&
                z.equals(that.z);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
        
        public static IntMatrix3 getIdentity() {
            return new IntMatrix3(
                new BlockPos(1, 0, 0),
                new BlockPos(0, 1, 0),
                new BlockPos(0, 0, 1)
            );
        }
    }
    
    public static int dirDotProduct(Direction dir, Direction b) {
        if (dir.getAxis() != b.getAxis()) {
            return 0;
        }
        if (dir.getAxisDirection() == b.getAxisDirection()) {
            return 1;
        }
        else {
            return -1;
        }
    }
    
    // counter clockwise
    public static Direction rotateAlong(Direction dir, Direction axis) {
        Tuple<Direction, Direction> ds = Helper.getPerpendicularDirections(axis);
        Direction d1 = ds.getA();
        Direction d2 = ds.getB();
        
        int c1 = dirDotProduct(dir, d1);
        int c2 = dirDotProduct(dir, d2);
        int ca = dirDotProduct(dir, axis);
        
        int nc1 = -c2;
        int nc2 = c1;
        
        BlockPos finalVec = Helper.scale(d1.getDirectionVec(), nc1)
            .add(Helper.scale(d2.getDirectionVec(), nc2))
            .add(Helper.scale(axis.getDirectionVec(), ca));
        return Direction.byLong(finalVec.getX(), finalVec.getY(), finalVec.getZ());
    }
    
    public static IntMatrix3 getRotation90(Direction direction) {
        return new IntMatrix3(
            rotateAlong(Direction.getFacingFromAxisDirection(Direction.Axis.X, Direction.AxisDirection.POSITIVE), direction).getDirectionVec(),
            rotateAlong(Direction.getFacingFromAxisDirection(Direction.Axis.Y, Direction.AxisDirection.POSITIVE), direction).getDirectionVec(),
            rotateAlong(Direction.getFacingFromAxisDirection(Direction.Axis.Z, Direction.AxisDirection.POSITIVE), direction).getDirectionVec()
        );
    }
    
    // sorted from simpler rotations to complex rotations
    public static final List<IntMatrix3> rotationTransformations = Util.make(() -> {
        List<IntMatrix3> basicRotations = Arrays.stream(Direction.values())
            .map(DiligentMatcher::getRotation90)
            .collect(Collectors.toList());
        
        IntMatrix3 identity = IntMatrix3.getIdentity();
        
        ArrayList<IntMatrix3> rotationList = new ArrayList<>();
        Set<IntMatrix3> rotationSet = new HashSet<>();
        
        rotationList.add(identity);
        rotationSet.add(identity);
        
        for (int i = 0; i < 3; i++) {
            ArrayList<IntMatrix3> newlyAdded = new ArrayList<>(rotationList);
            for (IntMatrix3 rot : rotationList) {
                for (IntMatrix3 basicRotation : basicRotations) {
                    IntMatrix3 newRot = rot.multiply(basicRotation);
                    if (!rotationSet.contains(newRot)) {
                        rotationSet.add(newRot);
                        newlyAdded.add(newRot);
                    }
                }
            }
            
            rotationList.addAll(newlyAdded);
        }
        
        Validate.isTrue(rotationList.size() == 24);
        
        return rotationList;
    });
    
    public static class TransformedShape {
        public final BlockPortalShape originalShape;
        public final BlockPortalShape transformedShape;
        public final IntMatrix3 rotation;
        public final double scale;
        
        public TransformedShape(BlockPortalShape originalShape, BlockPortalShape transformedShape, IntMatrix3 rotation, double scale) {
            this.originalShape = originalShape;
            this.transformedShape = transformedShape;
            this.rotation = rotation;
            this.scale = scale;
        }
    }
    
    public static List<TransformedShape> getMatchableShapeVariants(
        BlockPortalShape original,
        int maxShapeLen
    ) {
        List<TransformedShape> result = new ArrayList<>();
        HashSet<BlockPortalShape> shapeSet = new HashSet<>();
        
        int divFactor = getShapeShrinkFactor(original);
        
        BlockPortalShape shrinked = shrinkShapeBy(original, divFactor);
        
        BlockPos shrinkedShapeSize = shrinked.innerAreaBox.getSize();
        int shrinkedShapeLen = Math.max(shrinkedShapeSize.getX(), Math.max(shrinkedShapeSize.getY(), shrinkedShapeSize.getZ()));
        int maxMultiplyFactor = (int) Math.ceil(((double) maxShapeLen) / shrinkedShapeLen);
        
        for (IntMatrix3 rotation : rotationTransformations) {
            BlockPortalShape rotatedShape = rotateShape(shrinked, rotation);
            BlockPortalShape newShape = regularizeShape(rotatedShape);
            boolean isNew = shapeSet.add(newShape);
            if (isNew) {
                result.add(new TransformedShape(
                    original, newShape, rotation, 1.0 / divFactor
                ));
                
                for (int mul = 2; mul <= maxMultiplyFactor; mul++) {
                    BlockPortalShape expanded = regularizeShape(expandShape(rotatedShape, mul));
                    isNew = shapeSet.add(expanded);
                    if (isNew) {
                        result.add(new TransformedShape(
                            original, expanded,
                            rotation, ((double) mul) / divFactor
                        ));
                    }
                }
            }
        }
        
        return result;
    }
    
    public static BlockPortalShape regularizeShape(BlockPortalShape rotatedShape) {
        return rotatedShape.getShapeWithMovedAnchor(BlockPos.ZERO);
    }
    
    public static BlockPortalShape rotateShape(BlockPortalShape shape, IntMatrix3 t) {
        Set<BlockPos> newArea = shape.area.stream().map(
            b -> t.transform(b)
        ).collect(Collectors.toSet());
        Direction.Axis newAxis = t.transformDirection(
            Direction.getFacingFromAxisDirection(shape.axis, Direction.AxisDirection.POSITIVE)
        ).getAxis();
        return new BlockPortalShape(newArea, newAxis);
    }
    
    public static int getShapeShrinkFactor(BlockPortalShape shape) {
        HashSet<BlockPos> area = new HashSet<>(shape.area);
        
        ArrayList<IntBox> boxList = decomposeShape(shape, area);
        
        IntArrayList sideLenList = new IntArrayList();
        Tuple<Direction.Axis, Direction.Axis> axs = Helper.getAnotherTwoAxis(shape.axis);
        for (IntBox box : boxList) {
            BlockPos boxSize = box.getSize();
            int a = Helper.getCoordinate(boxSize, axs.getA());
            int b = Helper.getCoordinate(boxSize, axs.getB());
            sideLenList.add(a);
            sideLenList.add(b);
        }
        
        return sideLenList.stream().reduce((a, b) -> IntMath.gcd(a, b)).get();
    }
    
    public static BlockPortalShape shrinkShapeBy(BlockPortalShape shape, int div) {
        Validate.isTrue(div != 0);
        
        if (div == 1) {
            return shape;
        }
        
        Set<BlockPos> newArea = shape.area.stream().map(
            b -> Helper.divide(b, div)
        ).collect(Collectors.toSet());
        
        return new BlockPortalShape(newArea, shape.axis);
    }
    
    public static ArrayList<IntBox> decomposeShape(BlockPortalShape shape, HashSet<BlockPos> area) {
        ArrayList<IntBox> boxList = new ArrayList<>();
        while (!area.isEmpty()) {
            IntBox box = splitBoxFromArea(area, shape.axis);
            if (box == null) {
                break;
            }
            boxList.add(box);
        }
        return boxList;
    }
    
    public static BlockPortalShape expandShape(
        BlockPortalShape shape,
        int multiplyFactor
    ) {
        Tuple<Direction.Axis, Direction.Axis> axs = Helper.getAnotherTwoAxis(shape.axis);
        Vector3i v1 = Direction.getFacingFromAxisDirection(axs.getA(), Direction.AxisDirection.POSITIVE).getDirectionVec();
        Vector3i v2 = Direction.getFacingFromAxisDirection(axs.getB(), Direction.AxisDirection.POSITIVE).getDirectionVec();
        
        return new BlockPortalShape(
            shape.area.stream().flatMap(
                basePos -> IntStream.range(0, multiplyFactor).boxed().flatMap(dx ->
                    IntStream.range(0, multiplyFactor).mapToObj(dy ->
                        basePos.add(Helper.scale(v1, dx).add(Helper.scale(v2, dy)))
                    )
                )
            ).collect(Collectors.toSet()),
            shape.axis
        );
    }
    
    private static IntBox splitBoxFromArea(
        Set<BlockPos> area,
        Direction.Axis axis
    ) {
        Iterator<BlockPos> iterator = area.iterator();
        if (!iterator.hasNext()) {
            return null;
        }
        BlockPos firstElement = iterator.next();
        
        IntBox expanded = Helper.expandRectangle(
            firstElement,
            area::contains,
            axis
        );
        
        expanded.stream().forEach(b -> area.remove(b));
        
        return expanded;
    }
}
