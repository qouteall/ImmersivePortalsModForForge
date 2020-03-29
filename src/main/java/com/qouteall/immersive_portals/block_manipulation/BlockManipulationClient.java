package com.qouteall.immersive_portals.block_manipulation;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.commands.MyCommandServer;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.dimension.DimensionType;

public class BlockManipulationClient {
    public static DimensionType remotePointedDim;
    public static RayTraceResult remoteHitResult;
    public static boolean isContextSwitched = false;
    
    public static boolean isPointingToRemoteBlock() {
        return remotePointedDim != null;
    }
    
    public static void onPointedBlockUpdated(float partialTicks) {
        remotePointedDim = null;
        remoteHitResult = null;
        
        Minecraft mc = Minecraft.getInstance();
        Vec3d cameraPos = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
        
        float reachDistance = mc.playerController.getBlockReachDistance();
        
        MyCommandServer.getPlayerPointingPortalRaw(
            mc.player, partialTicks, reachDistance, true
        ).ifPresent(pair -> {
            double distanceToPortalPointing = pair.getSecond().distanceTo(cameraPos);
            if (distanceToPortalPointing < getCurrentTargetDistance() + 0.2) {
                updateTargetedBlockThroughPortal(
                    cameraPos,
                    mc.player.getLook(partialTicks),
                    mc.player.dimension,
                    distanceToPortalPointing,
                    reachDistance,
                    pair.getFirst()
                );
            }
        });
    }
    
    private static double getCurrentTargetDistance() {
        Minecraft mc = Minecraft.getInstance();
        Vec3d cameraPos = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
        
        if (mc.objectMouseOver == null) {
            return 23333;
        }
        
        //pointing to placeholder block does not count
        if (mc.objectMouseOver instanceof BlockRayTraceResult) {
            BlockRayTraceResult hitResult = (BlockRayTraceResult) mc.objectMouseOver;
            BlockPos hitPos = hitResult.getPos();
            if (mc.world.getBlockState(hitPos).getBlock() == PortalPlaceholderBlock.instance) {
                return 23333;
            }
        }
        
        return cameraPos.distanceTo(mc.objectMouseOver.getHitVec());
    }
    
    private static void updateTargetedBlockThroughPortal(
        Vec3d cameraPos,
        Vec3d viewVector,
        DimensionType playerDimension,
        double beginDistance,
        double endDistance,
        Portal portal
    ) {
        Minecraft mc = Minecraft.getInstance();
        
        Vec3d from = portal.transformPoint(
            cameraPos.add(viewVector.scale(beginDistance))
        );
        Vec3d to = portal.transformPoint(
            cameraPos.add(viewVector.scale(endDistance))
        );
        
        RayTraceContext context = new RayTraceContext(
            from,
            to,
            RayTraceContext.BlockMode.OUTLINE,
            RayTraceContext.FluidMode.NONE,
            mc.player
        );
        
        ClientWorld world = CGlobal.clientWorldLoader.getWorld(
            portal.dimensionTo
        );
        
        remoteHitResult = IBlockReader.func_217300_a(
            context,
            (rayTraceContext, blockPos) -> {
                BlockState blockState = world.getBlockState(blockPos);
                
                //don't stop at placeholder block
                if (blockState.getBlock() == PortalPlaceholderBlock.instance) {
                    return null;
                }
                
                IFluidState fluidState = world.getFluidState(blockPos);
                Vec3d vec3d = rayTraceContext.func_222253_b();
                Vec3d vec3d2 = rayTraceContext.func_222250_a();
                VoxelShape solidShape = rayTraceContext.getBlockShape(blockState, world, blockPos);
                BlockRayTraceResult blockHitResult = world.rayTraceBlocks(
                    vec3d, vec3d2, blockPos, solidShape, blockState
                );
                VoxelShape fluidShape = rayTraceContext.getFluidShape(fluidState, world, blockPos);
                BlockRayTraceResult blockHitResult2 = fluidShape.rayTrace(vec3d, vec3d2, blockPos);
                double d = blockHitResult == null ? Double.MAX_VALUE :
                    rayTraceContext.func_222253_b().squareDistanceTo(blockHitResult.getHitVec());
                double e = blockHitResult2 == null ? Double.MAX_VALUE :
                    rayTraceContext.func_222253_b().squareDistanceTo(blockHitResult2.getHitVec());
                return d <= e ? blockHitResult : blockHitResult2;
            },
            (rayTraceContext) -> {
                Vec3d vec3d = rayTraceContext.func_222253_b().subtract(rayTraceContext.func_222250_a());
                return BlockRayTraceResult.createMiss(
                    rayTraceContext.func_222250_a(),
                    Direction.getFacingFromVector(vec3d.x, vec3d.y, vec3d.z),
                    new BlockPos(rayTraceContext.func_222250_a())
                );
            }
        );
    
        if (remoteHitResult.getHitVec().y < 0.1) {
            remoteHitResult = new BlockRayTraceResult(
                remoteHitResult.getHitVec(),
                Direction.DOWN,
                ((BlockRayTraceResult) remoteHitResult).getPos(),
                ((BlockRayTraceResult) remoteHitResult).isInside()
            );
        }
    
        if (remoteHitResult != null) {
            if (!world.getBlockState(((BlockRayTraceResult) remoteHitResult).getPos()).isAir()) {
                mc.objectMouseOver = null;
                remotePointedDim = portal.dimensionTo;
            }
        }
    }
    
    public static void myHandleBlockBreaking(boolean isKeyPressed) {
        Minecraft mc = Minecraft.getInstance();
        
        if (!mc.player.isHandActive()) {
            if (isKeyPressed && isPointingToRemoteBlock()) {
                BlockRayTraceResult blockHitResult = (BlockRayTraceResult) remoteHitResult;
                BlockPos blockPos = blockHitResult.getPos();
                ClientWorld remoteWorld =
                    CGlobal.clientWorldLoader.getWorld(remotePointedDim);
                if (!remoteWorld.getBlockState(blockPos).isAir()) {
                    Direction direction = blockHitResult.getFace();
                    if (myUpdateBlockBreakingProgress(mc, blockPos, direction)) {
                        mc.particles.addBlockHitEffects(blockPos, direction);
                        mc.player.swingArm(Hand.MAIN_HAND);
                    }
                }
                
            }
            else {
                mc.playerController.resetBlockRemoving();
            }
        }
    }
    
    //hacky switch
    public static boolean myUpdateBlockBreakingProgress(
        Minecraft mc,
        BlockPos blockPos,
        Direction direction
    ) {
        ClientWorld oldWorld = mc.world;
        mc.world = CGlobal.clientWorldLoader.getWorld(remotePointedDim);
        isContextSwitched = true;
    
        try {
            return mc.playerController.onPlayerDamageBlock(blockPos, direction);
        }
        finally {
            mc.world = oldWorld;
            isContextSwitched = false;
        }
    
    }
    
    public static void myAttackBlock() {
        Minecraft mc = Minecraft.getInstance();
        
        ClientWorld targetWorld =
            CGlobal.clientWorldLoader.getWorld(remotePointedDim);
        BlockPos blockPos = ((BlockRayTraceResult) remoteHitResult).getPos();
    
        if (targetWorld.isAirBlock(blockPos)) {
            return;
        }
    
        ClientWorld oldWorld = mc.world;
    
        mc.world = targetWorld;
        isContextSwitched = true;
    
        try {
            mc.playerController.clickBlock(
                blockPos,
                ((BlockRayTraceResult) remoteHitResult).getFace()
            );
        }
        finally {
            mc.world = oldWorld;
            isContextSwitched = false;
        }
    
        mc.player.swingArm(Hand.MAIN_HAND);
    }
    
    //too lazy to rewrite the whole interaction system so hack there and here
    public static void myItemUse(Hand hand) {
        Minecraft mc = Minecraft.getInstance();
        ClientWorld targetWorld =
            CGlobal.clientWorldLoader.getWorld(remotePointedDim);
        
        ItemStack itemStack = mc.player.getHeldItem(hand);
        BlockRayTraceResult blockHitResult = (BlockRayTraceResult) remoteHitResult;
        
        Tuple<BlockRayTraceResult, DimensionType> result =
            BlockManipulationServer.getHitResultForPlacing(targetWorld, blockHitResult);
        blockHitResult = result.getA();
        targetWorld = CGlobal.clientWorldLoader.getWorld(result.getB());
        remoteHitResult = blockHitResult;
        remotePointedDim = result.getB();
        
        int i = itemStack.getCount();
        ActionResultType actionResult2 = myInteractBlock(hand, mc, targetWorld, blockHitResult);
        if (actionResult2.isSuccessOrConsume()) {
            if (actionResult2.isSuccess()) {
                mc.player.swingArm(hand);
                if (!itemStack.isEmpty() && (itemStack.getCount() != i || mc.playerController.isInCreativeMode())) {
                    mc.gameRenderer.itemRenderer.resetEquippedProgress(hand);
                }
            }
            
            return;
        }
        
        if (actionResult2 == ActionResultType.FAIL) {
            return;
        }
        
        if (!itemStack.isEmpty()) {
            ActionResultType actionResult3 = mc.playerController.processRightClick(
                mc.player,
                targetWorld,
                hand
            );
            if (actionResult3.isSuccessOrConsume()) {
                if (actionResult3.isSuccess()) {
                    mc.player.swingArm(hand);
                }
                
                mc.gameRenderer.itemRenderer.resetEquippedProgress(hand);
                return;
            }
        }
    }
    
    private static ActionResultType myInteractBlock(
        Hand hand,
        Minecraft mc,
        ClientWorld targetWorld,
        BlockRayTraceResult blockHitResult
    ) {
        ClientWorld oldWorld = mc.world;
        
        try {
            mc.player.world = targetWorld;
            mc.world = targetWorld;
            isContextSwitched = true;
            
            return mc.playerController.func_217292_a(
                mc.player, targetWorld, hand, blockHitResult
            );
        }
        finally {
            mc.player.world = oldWorld;
            mc.world = oldWorld;
            isContextSwitched = false;
        }
    }
    
}
