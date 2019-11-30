package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class BreakableMirror extends Mirror {
    
    public static EntityType<BreakableMirror> entityType;
    
    public IntegerAABBInclusive wallArea;
    
    public BreakableMirror(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    protected void readAdditional(CompoundNBT tag) {
        super.readAdditional(tag);
        wallArea = new IntegerAABBInclusive(
            new BlockPos(
                tag.getInt("boxXL"),
                tag.getInt("boxYL"),
                tag.getInt("boxZL")
            ),
            new BlockPos(
                tag.getInt("boxXH"),
                tag.getInt("boxYH"),
                tag.getInt("boxZH")
            )
        );
    }
    
    @Override
    protected void writeAdditional(CompoundNBT tag) {
        super.writeAdditional(tag);
        tag.putInt("boxXL", wallArea.l.getX());
        tag.putInt("boxYL", wallArea.l.getY());
        tag.putInt("boxZL", wallArea.l.getZ());
        tag.putInt("boxXH", wallArea.h.getX());
        tag.putInt("boxYH", wallArea.h.getY());
        tag.putInt("boxZH", wallArea.h.getZ());
    }
    
    @Override
    public void tick() {
        super.tick();
        if (!world.isRemote) {
            if (world.getGameTime() % 50 == getEntityId() % 50) {
                checkWallIntegrity();
            }
        }
    }
    
    @Override
    public boolean isPortalValid() {
        if (world.isRemote) {
            return super.isPortalValid();
        }
        else {
            return super.isPortalValid() && wallArea != null;
        }
    }
    
    private void checkWallIntegrity() {
        boolean wallValid = wallArea.streamOfMutable().allMatch(
            blockPos ->
                isGlass(world, blockPos)
        );
        if (!wallValid) {
            removed = true;
        }
    }
    
    private static boolean isGlass(World world, BlockPos blockPos) {
        return world.getBlockState(blockPos).getBlock() == Blocks.GLASS;
    }
    
    public static BreakableMirror createMirror(
        ServerWorld world,
        BlockPos glassPos,
        Direction facing
    ) {
        if (!isGlass(world, glassPos)) {
            return null;
        }
        
        IntegerAABBInclusive wallArea = new IntegerAABBInclusive(glassPos, glassPos);
        
        for (Direction direction : Helper.getAnotherFourDirections(facing.getAxis())) {
            wallArea = Helper.expandArea(
                wallArea,
                blockPos -> isGlass(world, blockPos),
                direction
            );
        }
        
        BreakableMirror breakableMirror = BreakableMirror.entityType.create(world);
        Vec3d pos = new Vec3d(
            (double) (wallArea.l.getX() + wallArea.h.getX()) / 2,
            (double) (wallArea.l.getY() + wallArea.h.getY()) / 2,
            (double) (wallArea.l.getZ() + wallArea.h.getZ()) / 2
        ).add(
            0.5, 0.5, 0.5
        ).add(
            new Vec3d(facing.getDirectionVec()).scale(0.5)
        );
        breakableMirror.setPosition(
            pos.x, pos.y, pos.z
        );
        breakableMirror.destination = pos;
        breakableMirror.dimensionTo = world.dimension.getType();
        
        Tuple<Direction.Axis, Direction.Axis> axises = Helper.getAnotherTwoAxis(facing.getAxis());
        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            axises = new Tuple<>(axises.getB(), axises.getA());
        }
    
        Direction.Axis wAxis = axises.getA();
        Direction.Axis hAxis = axises.getB();
        float width = Helper.getCoordinate(wallArea.getSize(), wAxis);
        float height = Helper.getCoordinate(wallArea.getSize(), hAxis);
        
        breakableMirror.axisW = new Vec3d(
            Direction.getFacingFromAxisDirection(
                wAxis,
                Direction.AxisDirection.POSITIVE
            ).getDirectionVec()
        );
        breakableMirror.axisH = new Vec3d(
            Direction.getFacingFromAxisDirection(
                hAxis,
                Direction.AxisDirection.POSITIVE
            ).getDirectionVec()
        );
    
        breakableMirror.width = width;
        breakableMirror.height = height;
        
        breakableMirror.wallArea = wallArea;
    
        world.addEntity(breakableMirror);
        
        return breakableMirror;
    }
}
