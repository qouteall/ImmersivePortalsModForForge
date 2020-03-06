package com.qouteall.hiding_in_the_bushes.mixin.block_manipulation;

import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;

@Mixin(Item.class)
public class MixinItem {
    private static WeakReference<PlayerEntity> argPlayer;
    
    @Inject(
        method = "Lnet/minecraft/item/Item;rayTrace(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/RayTraceContext$FluidMode;)Lnet/minecraft/util/math/RayTraceResult;",
        at = @At("HEAD")
    )
    private static void onRayTrace(
        World world,
        PlayerEntity player,
        RayTraceContext.FluidMode fluidHandling,
        CallbackInfoReturnable<RayTraceResult> cir
    ) {
        argPlayer = new WeakReference<>(player);
    }
    
    @Redirect(
        method = "rayTrace",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ai/attributes/IAttributeInstance;getValue()D"
        )
    )
    private static double modifyHandReach(IAttributeInstance iAttributeInstance) {
        double multiplier = HandReachTweak.getActualHandReachMultiplier(argPlayer.get());
        return iAttributeInstance.getValue() * multiplier;
    }
}
