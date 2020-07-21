package com.qouteall.immersive_portals;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import javax.annotation.Nullable;
import java.util.stream.Stream;

public class ClippedBlockView implements IBlockReader {
    public IBlockReader delegate;
    public Vector3d clipPos;
    public Vector3d contentDirection;
    public Entity raytraceDefaultEntity;
    
    public ClippedBlockView(IBlockReader delegate, Vector3d clipPos, Vector3d contentDirection, Entity raytraceDefaultEntity) {
        this.delegate = delegate;
        this.clipPos = clipPos;
        this.contentDirection = contentDirection;
        this.raytraceDefaultEntity = raytraceDefaultEntity;
    }
    
    public boolean isClipped(BlockPos pos) {
        return Vector3d.func_237489_a_(pos).subtract(clipPos).dotProduct(contentDirection) < 0;
    }
    
    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return delegate.getTileEntity(pos);
    }
    
    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (isClipped(pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return delegate.getBlockState(pos);
    }
    
    @Override
    public FluidState getFluidState(BlockPos pos) {
        if (isClipped(pos)) {
            return Fluids.EMPTY.getDefaultState();
        }
        
        return delegate.getFluidState(pos);
    }
    
    @Override
    public int getLightValue(BlockPos pos) {
        return delegate.getLightValue(pos);
    }
    
    @Override
    public int getMaxLightLevel() {
        return delegate.getMaxLightLevel();
    }
    
    @Override
    public int getHeight() {
        return delegate.getHeight();
    }
    
    @Override
    public Stream<BlockState> func_234853_a_(AxisAlignedBB box) {
        return delegate.func_234853_a_(box);
    }
    
    @Override
    public BlockRayTraceResult rayTraceBlocks(RayTraceContext context) {
        Vector3d delta = context.func_222250_a().subtract(context.func_222253_b());
        double t = Helper.getCollidingT(
            clipPos,
            contentDirection,
            context.func_222253_b(),
            delta
        );
        Vector3d startPos = context.func_222253_b().add(delta.scale(t));
    
        return rayTraceBlocks(new RayTraceContext(
            startPos,
            context.func_222250_a(),
            RayTraceContext.BlockMode.VISUAL, RayTraceContext.FluidMode.NONE,
            raytraceDefaultEntity
        ));
    }
    
    @Nullable
    @Override
    public BlockRayTraceResult rayTraceBlocks(Vector3d start, Vector3d end, BlockPos pos, VoxelShape shape, BlockState state) {
        return delegate.rayTraceBlocks(start, end, pos, shape, state);
    }
    
}
