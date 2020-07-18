package com.qouteall.immersive_portals.mixin.portal_generation;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(NetherPortalBlock.class)
public class MixinNetherPortalBlock {
    
    @Inject(
        method = "Lnet/minecraft/block/NetherPortalBlock;trySpawnPortal(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onCreatePortal(
        IWorld world,
        BlockPos pos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (world instanceof ServerWorld) {
            boolean isNearObsidian = Arrays.stream(Direction.values())
                .anyMatch(direction -> O_O.isObsidian(world.getBlockState(pos.offset(direction))));
            
            if (!isNearObsidian) {
                cir.setReturnValue(false);
                cir.cancel();
                return;
            }
            
            boolean result = NetherPortalGeneration.onFireLitOnObsidian(
                ((ServerWorld) world),
                pos
            );
            
            cir.setReturnValue(result);
            cir.cancel();
        }
        
        
    }
}
