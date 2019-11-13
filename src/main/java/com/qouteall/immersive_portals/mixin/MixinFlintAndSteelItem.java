package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.portal.BreakableMirror;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGenerator;
import com.qouteall.immersive_portals.portal.nether_portal.NewNetherPortalGenerator;
import net.minecraft.block.Blocks;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FlintAndSteelItem.class)
public class MixinFlintAndSteelItem {
    @Inject(method = "onItemUse", at = @At("HEAD"))
    private void onUseFlintAndSteel(
        ItemUseContext context,
        CallbackInfoReturnable<ActionResult> cir
    ) {
        IWorld world = context.getWorld();
        if (!world.isRemote()) {
            BlockPos blockPos_1 = context.getPos();
            BlockPos firePos = blockPos_1.offset(context.getFace());
            if (world.getBlockState(blockPos_1).getBlock() == Blocks.OBSIDIAN) {
                boolean isNetherPortalGenerated = SGlobal.doUseNewNetherPortal ?
                    NewNetherPortalGenerator.onFireLit(
                        ((ServerWorld) world), firePos
                    ) :
                    NetherPortalGenerator.onFireLit(
                        ((ServerWorld) world), firePos
                    ) != null;
            }
            else {
                BreakableMirror mirror = BreakableMirror.createMirror(
                    ((ServerWorld) world), blockPos_1, context.getFace()
                );
                if (mirror != null) {
                    context.getItem().damageItem(1, context.getPlayer(),
                        playerEntity_1x -> playerEntity_1x.sendBreakAnimation(context.getHand())
                    );
                }
            }
        }
    }
}
