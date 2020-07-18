package com.qouteall.immersive_portals.mixin.portal_generation;

import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.portal.BreakableMirror;
import com.qouteall.immersive_portals.portal.CustomizablePortalGenerationOld;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public class MixinFlintAndSteelItem {
//    @Inject(
//        method = "canIgnite",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private static void onCanIgnite(
//        BlockState block,
//        WorldAccess world,
//        BlockPos pos,
//        CallbackInfoReturnable<Boolean> cir
//    ) {
//        for (Direction direction : Direction.values()) {
//            if (O_O.isObsidian(world, pos.offset(direction))) {
//                if (block.isAir()) {
//                    cir.setReturnValue(true);
//                    cir.cancel();
//                }
//            }
//        }
//    }
    
    @Inject(method = "Lnet/minecraft/item/FlintAndSteelItem;onItemUse(Lnet/minecraft/item/ItemUseContext;)Lnet/minecraft/util/ActionResultType;", at = @At("HEAD"), cancellable = true)
    private void onUseFlintAndSteel(
        ItemUseContext context,
        CallbackInfoReturnable<ActionResultType> cir
    ) {
        IWorld world = context.getWorld();
        if (!world.isRemote()) {
            BlockPos targetPos = context.getPos();
            Direction side = context.getFace();
            BlockPos firePos = targetPos.offset(side);
            BlockState targetBlockState = world.getBlockState(targetPos);
            Block targetBlock = targetBlockState.getBlock();
            if (BreakableMirror.isGlass(((World) world), targetPos)) {
                BreakableMirror mirror = BreakableMirror.createMirror(
                    ((ServerWorld) world), targetPos, side
                );
                cir.setReturnValue(ActionResultType.SUCCESS);
            }
            else if (targetBlock == ModMain.portalHelperBlock) {
                boolean result = NetherPortalGeneration.activatePortalHelper(
                    ((ServerWorld) world),
                    firePos
                );
            }
            else {
                CustomizablePortalGenerationOld.onFireLit(
                    ((ServerWorld) world),
                    firePos,
                    targetBlock
                );
            }
        }
    }
}
