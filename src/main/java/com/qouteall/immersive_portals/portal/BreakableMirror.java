package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.my_util.IntBox;
import net.minecraft.block.Block;
import net.minecraft.block.GlassBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class BreakableMirror extends Mirror {
    
    public static EntityType<BreakableMirror> entityType;
    
    public IntBox wallArea;
    public boolean unbreakable = false;
    
    public BreakableMirror(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    protected void readAdditional(CompoundNBT tag) {
        super.readAdditional(tag);
        wallArea = new IntBox(
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
        if (tag.contains("unbreakable")) {
            unbreakable = tag.getBoolean("unbreakable");
        }
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
        
        tag.putBoolean("unbreakable", unbreakable);
    }
    
    @Override
    public void tick() {
        super.tick();
        if (!world.isRemote) {
            if (!unbreakable) {
                if (world.getGameTime() % 10 == getEntityId() % 10) {
                    checkWallIntegrity();
                }
            }
        }
    }
    
    @Override
    public boolean isPortalValid() {
        return super.isPortalValid() && wallArea != null;
    }
    
    private void checkWallIntegrity() {
        boolean wallValid = wallArea.fastStream().allMatch(
            blockPos ->
                isGlass(world, blockPos)
        );
        if (!wallValid) {
            removed = true;
        }
    }
    
    public static boolean isGlass(World world, BlockPos blockPos) {
        Block block = world.getBlockState(blockPos).getBlock();
        return block instanceof GlassBlock || block instanceof PaneBlock || block instanceof StainedGlassBlock;
    }
    
    private static boolean isGlassPane(World world, BlockPos blockPos) {
        Block block = world.getBlockState(blockPos).getBlock();
        return block instanceof PaneBlock;
    }
    
    public static BreakableMirror createMirror(
        ServerWorld world,
        BlockPos glassPos,
        Direction facing
    ) {
        if (!isGlass(world, glassPos)) {
            return null;
        }
        
        boolean isPane = isGlassPane(world, glassPos);

        if (facing.getAxis() == Direction.Axis.Y && isPane) {
            return null;
        }
        
        IntBox wallArea = Helper.expandRectangle(
            glassPos,
            blockPos -> isGlass(world, blockPos) && (isPane == isGlassPane(world, blockPos)),
            facing.getAxis()
        );
        
        BreakableMirror breakableMirror = BreakableMirror.entityType.create(world);
        double distanceToCenter = isPane ? (1.0 / 16) : 0.5;
        
        AxisAlignedBB wallBox = getWallBox(world, wallArea);
        
        Vector3d pos = Helper.getBoxSurface(wallBox, facing.getOpposite()).getCenter();
        pos = Helper.putCoordinate(
            //getWallBox is incorrect with corner glass pane so correct the coordinate on the normal axis
            pos, facing.getAxis(),
            Helper.getCoordinate(
                wallArea.getCenterVec().add(
                     Vector3d.func_237491_b_(facing.getDirectionVec()).scale(distanceToCenter)
                ),
                facing.getAxis()
            )
        );
        breakableMirror.setPosition(pos.x, pos.y, pos.z);
        breakableMirror.destination = pos;
        breakableMirror.dimensionTo = world.func_234923_W_();
        
        Tuple<Direction, Direction> dirs =
            Helper.getPerpendicularDirections(facing);
        
        Vector3d boxSize = Helper.getBoxSize(wallBox);
        double width = Helper.getCoordinate(boxSize, dirs.getA().getAxis());
        double height = Helper.getCoordinate(boxSize, dirs.getB().getAxis());
        
        breakableMirror.axisW =  Vector3d.func_237491_b_(dirs.getA().getDirectionVec());
        breakableMirror.axisH =  Vector3d.func_237491_b_(dirs.getB().getDirectionVec());
        breakableMirror.width = width;
        breakableMirror.height = height;
        
        breakableMirror.wallArea = wallArea;
        
        breakIntersectedMirror(breakableMirror);
        
        world.addEntity(breakableMirror);
        
        return breakableMirror;
    }
    
    private static void breakIntersectedMirror(BreakableMirror newMirror) {
        McHelper.getEntitiesNearby(
            newMirror,
            BreakableMirror.class,
            20
        ).filter(
            mirror1 -> mirror1.getNormal().dotProduct(newMirror.getNormal()) > 0.5
        ).filter(
            mirror1 -> IntBox.getIntersect(
                mirror1.wallArea, newMirror.wallArea
            ) != null
        ).filter(
            mirror -> mirror != newMirror
        ).forEach(
            Entity::remove
        );
    }
    
    //it's a little bit incorrect with corner glass pane
    private static AxisAlignedBB getWallBox(ServerWorld world, IntBox glassArea) {
        return glassArea.stream().map(blockPos ->
            world.getBlockState(blockPos).getCollisionShape(world, blockPos).getBoundingBox()
                .offset( Vector3d.func_237491_b_(blockPos))
        ).reduce(AxisAlignedBB::union).orElse(null);
    }
}
