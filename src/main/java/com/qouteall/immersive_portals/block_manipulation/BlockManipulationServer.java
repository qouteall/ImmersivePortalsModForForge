package com.qouteall.immersive_portals.block_manipulation;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.Portal;
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
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import java.util.List;

public class BlockManipulationServer {
    
    public static void processBreakBlock(
        RegistryKey<World> dimension,
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
        RegistryKey<World> dimension,
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
        RegistryKey<World> dimension,
        ServerPlayerEntity player,
        BlockPos requestPos
    ) {
        Vector3d pos = Vector3d.func_237489_a_(requestPos);
        Vector3d playerPos = player.getPositionVec();
        double multiplier = HandReachTweak.getActualHandReachMultiplier(player);
        double distanceSquare = 6 * 6 * multiplier * multiplier;
        if (player.world.func_234923_W_() == dimension) {
            if (playerPos.squareDistanceTo(pos) < distanceSquare) {
                return true;
            }
        }
        return McHelper.getNearbyPortals(
            player,
            20
        ).anyMatch(portal ->
            portal.dimensionTo == dimension &&
                portal.transformPoint(playerPos).squareDistanceTo(pos) <
                    distanceSquare * portal.getScale() * portal.getScale()
        );
    }
    
    private static void doDestroyBlock(
        RegistryKey<World> dimension,
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
    
    public static Tuple<BlockRayTraceResult, RegistryKey<World>> getHitResultForPlacing(
        World world,
        BlockRayTraceResult blockHitResult
    ) {
        Direction side = blockHitResult.getFace();
        Vector3d sideVec = Vector3d.func_237491_b_(side.getDirectionVec());
        Vector3d hitCenter = Vector3d.func_237489_a_(blockHitResult.getPos());
        
        List<Portal> globalPortals = McHelper.getGlobalPortals(world);
        
        Portal portal = globalPortals.stream().filter(p ->
            p.getContentDirection().dotProduct(sideVec) > 0.9
                && p.isPointInPortalProjection(hitCenter)
                && p.getDistanceToPlane(hitCenter) < 0.6
        ).findFirst().orElse(null);
        
        if (portal == null) {
            return new Tuple<>(blockHitResult, world.func_234923_W_());
        }
        
        Vector3d newCenter = portal.transformPoint(hitCenter.add(sideVec.scale(0.501)));
        BlockPos placingBlockPos = new BlockPos(newCenter);
        
        BlockRayTraceResult newHitResult = new BlockRayTraceResult(
            Vector3d.ZERO,
            side.getOpposite(),
            placingBlockPos,
            blockHitResult.isInside()
        );
        
        return new Tuple<>(newHitResult, portal.dimensionTo);
    }
    
    public static void processRightClickBlock(
        RegistryKey<World> dimension,
        CPlayerTryUseItemOnBlockPacket packet,
        ServerPlayerEntity player
    ) {
        Hand hand = packet.getHand();
        BlockRayTraceResult blockHitResult = packet.func_218794_c();
        
        ServerWorld world = McHelper.getServer().getWorld(dimension);
        
        doProcessRightClick(dimension, player, hand, blockHitResult);
    }
    
    public static void doProcessRightClick(
        RegistryKey<World> dimension,
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
                    player.swing(hand, true);
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
