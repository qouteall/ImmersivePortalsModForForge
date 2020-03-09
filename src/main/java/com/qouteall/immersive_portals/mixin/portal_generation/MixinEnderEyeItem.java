package com.qouteall.immersive_portals.mixin.portal_generation;

import com.qouteall.immersive_portals.portal.EndPortalEntity;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderEyeItem.class)
public class MixinEnderEyeItem {
    @Inject(method = "Lnet/minecraft/item/EnderEyeItem;onItemUse(Lnet/minecraft/item/ItemUseContext;)Lnet/minecraft/util/ActionResultType;", at = @At("HEAD"), cancellable = true)
    private void onUseOnBlock(
        ItemUseContext itemUsageContext_1,
        CallbackInfoReturnable<ActionResultType> cir
    ) {
        cir.setReturnValue(myUseOnBlock(itemUsageContext_1));
        cir.cancel();
    }
    
    private ActionResultType myUseOnBlock(ItemUseContext itemUsageContext) {
        World world = itemUsageContext.getWorld();
        BlockPos blockPos = itemUsageContext.getPos();
        BlockState blockState = world.getBlockState(blockPos);
        if (blockState.getBlock() == Blocks.END_PORTAL_FRAME &&
            !blockState.get(EndPortalFrameBlock.EYE)) {
            if (world.isRemote) {
                return ActionResultType.SUCCESS;
            }
            else {
                BlockState blockState_2 = (BlockState) blockState.with(
                    EndPortalFrameBlock.EYE,
                    true
                );
                Block.nudgeEntitiesWithNewState(blockState, blockState_2, world, blockPos);
                world.setBlockState(blockPos, blockState_2, 2);
                world.updateComparatorOutputLevel(blockPos, Blocks.END_PORTAL_FRAME);
                itemUsageContext.getItem().shrink(1);
                world.playEvent(1503, blockPos, 0);
                BlockPattern.PatternHelper pattern =
                    EndPortalFrameBlock.getOrCreatePortalShape().match(world, blockPos);
                if (pattern != null) {
                    BlockPos blockPos_2 = pattern.getFrontTopLeft().add(-3, 0, -3);
                    
                    for (int dx = 0; dx < 3; ++dx) {
                        for (int dz = 0; dz < 3; ++dz) {
                            world.setBlockState(
                                blockPos_2.add(dx, 0, dz),
                                PortalPlaceholderBlock.instance.getDefaultState().with(
                                    PortalPlaceholderBlock.AXIS, Direction.Axis.Y
                                ),
                                2
                            );
                        }
                    }
                    
                    world.playBroadcastSound(1038, blockPos_2.add(1, 0, 1), 0);
                    
                    EndPortalEntity.onEndPortalComplete(((ServerWorld) world), pattern);
                }
                
                return ActionResultType.SUCCESS;
            }
        }
        else {
            return ActionResultType.PASS;
        }
    }
}
