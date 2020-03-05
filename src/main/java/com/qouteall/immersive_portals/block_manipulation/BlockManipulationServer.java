package com.qouteall.immersive_portals.block_manipulation;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.network.play.server.SChangeBlockPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

public class BlockManipulationServer {
    
    public static void processBreakBlock(
        DimensionType dimension,
        CPlayerDiggingPacket packet,
        ServerPlayerEntity player
    ) {
        if (shouldFinishMining(dimension, packet, player)) {
            if (canPlayerReach(dimension, player, packet.getPosition())) {
                doDestroyBlock(dimension, packet, player);
            }
            else {
                Helper.log("Rejected cross portal block breaking packet " + player);
            }
        }
    }
    
    private static boolean shouldFinishMining(
        DimensionType dimension,
        CPlayerDiggingPacket packet,
        ServerPlayerEntity player
    
    ) {
        if (packet.getAction() == CPlayerDiggingPacket.Action.START_DESTROY_BLOCK) {
            return canInstantMine(
                McHelper.getServer().getWorld(dimension),
                player,
                packet.getPosition()
            );
        }
        else {
            return packet.getAction() == CPlayerDiggingPacket.Action.STOP_DESTROY_BLOCK;
        }
    }
    
    private static boolean canPlayerReach(
        DimensionType dimension,
        ServerPlayerEntity player,
        BlockPos requestPos
    ) {
        Vec3d pos = new Vec3d(requestPos);
        Vec3d playerPos = player.getPositionVec();
        if (player.dimension == dimension) {
            return playerPos.squareDistanceTo(pos) < 100;
        }
        return McHelper.getServerPortalsNearby(
            player,
            20
        ).anyMatch(portal ->
            portal.dimensionTo == dimension &&
                portal.applyTransformationToPoint(playerPos).squareDistanceTo(pos) < 100
        );
    }
    
    private static void doDestroyBlock(
        DimensionType dimension,
        CPlayerDiggingPacket packet,
        ServerPlayerEntity player
    ) {
        ServerWorld destWorld = McHelper.getServer().getWorld(dimension);
        ServerWorld oldWorld = player.interactionManager.world;
        player.interactionManager.setWorld(destWorld);
        player.interactionManager.tryHarvestBlock(
            packet.getPosition()
        );
        player.interactionManager.setWorld(oldWorld);
    }
    
    private static boolean canInstantMine(
        ServerWorld world,
        ServerPlayerEntity player,
        BlockPos pos
    ) {
        if (player.isCreative()) {
            return true;
        }
        
        float progress = 1.0F;
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.isAir()) {
            blockState.onBlockClicked(world, pos, player);
            progress = blockState.getPlayerRelativeBlockHardness(player, world, pos);
        }
        return !blockState.isAir() && progress >= 1.0F;
    }
    
    public static Tuple<BlockRayTraceResult, DimensionType> getHitResultForPlacing(
        World world,
        BlockRayTraceResult blockHitResult
    ) {
        BlockRayTraceResult targetBlockHitResult = blockHitResult;
        DimensionType targetDimension = world.dimension.getType();
        if (blockHitResult.getPos().getY() == 255 && blockHitResult.getFace() == Direction.UP) {
            VerticalConnectingPortal connectingPortal = VerticalConnectingPortal.getConnectingPortal(
                world,
                VerticalConnectingPortal.ConnectorType.ceil
            );
            if (connectingPortal != null) {
                targetBlockHitResult = new BlockRayTraceResult(
                    Vec3d.ZERO,
                    Direction.DOWN,
                    new BlockPos(
                        blockHitResult.getPos().getX(),
                        0,
                        blockHitResult.getPos().getZ()
                    ),
                    blockHitResult.isInside()
                );
                targetDimension = connectingPortal.dimensionTo;
            }
        }
        
        if (blockHitResult.getPos().getY() == 0 && blockHitResult.getFace() == Direction.DOWN) {
            VerticalConnectingPortal connectingPortal = VerticalConnectingPortal.getConnectingPortal(
                world,
                VerticalConnectingPortal.ConnectorType.floor
            );
            if (connectingPortal != null) {
                targetBlockHitResult = new BlockRayTraceResult(
                    Vec3d.ZERO,
                    Direction.UP,
                    new BlockPos(
                        blockHitResult.getPos().getX(),
                        255,
                        blockHitResult.getPos().getZ()
                    ),
                    blockHitResult.isInside()
                );
                targetDimension = connectingPortal.dimensionTo;
            }
        }
        return new Tuple<>(targetBlockHitResult, targetDimension);
    }
    
    public static void processRightClickBlock(
        DimensionType dimension,
        CPlayerTryUseItemOnBlockPacket packet,
        ServerPlayerEntity player
    ) {
        Hand hand = packet.getHand();
        BlockRayTraceResult blockHitResult = packet.func_218794_c();
        
        ServerWorld world = McHelper.getServer().getWorld(dimension);
        
        doProcessRightClick(dimension, player, hand, blockHitResult);
    }
    
    public static void doProcessRightClick(
        DimensionType dimension,
        ServerPlayerEntity player,
        Hand hand,
        BlockRayTraceResult blockHitResult
    ) {
        ItemStack itemStack = player.getHeldItem(hand);
        
        MinecraftServer server = McHelper.getServer();
        ServerWorld targetWorld = server.getWorld(dimension);
        
        BlockPos blockPos = blockHitResult.getPos();
        Direction direction = blockHitResult.getFace();
        player.markPlayerActive();
        if (targetWorld.isBlockModifiable(player, blockPos)) {
            if (!canPlayerReach(dimension, player, blockPos)) {
                Helper.log("Reject cross portal block placing packet " + player);
                return;
            }
            
            World oldWorld = player.world;
            
            player.world = targetWorld;
            try {
                ActionResultType actionResult = player.interactionManager.func_219441_a(
                    player,
                    targetWorld,
                    itemStack,
                    hand,
                    blockHitResult
                );
                if (actionResult.isSuccess()) {
                    player.func_226292_a_(hand, true);
                }
            }
            finally {
                player.world = oldWorld;
            }
        }
        
        MyNetwork.sendRedirectedMessage(
            player,
            dimension,
            new SChangeBlockPacket(targetWorld, blockPos)
        );
        
        BlockPos offseted = blockPos.offset(direction);
        if (offseted.getY() >= 0 && offseted.getY() <= 256) {
            MyNetwork.sendRedirectedMessage(
                player,
                dimension,
                new SChangeBlockPacket(targetWorld, offseted)
            );
        }
    }
}
