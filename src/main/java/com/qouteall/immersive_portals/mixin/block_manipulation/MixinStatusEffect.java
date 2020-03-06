package com.qouteall.immersive_portals.mixin.block_manipulation;

import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Effect.class)
public class MixinStatusEffect {
    @Invoker("<init>")
    private static Effect construct(EffectType type, int color) {
        return null;
    }
    
    static {
        HandReachTweak.statusEffectConstructor = (a, b) -> construct(a, b);
    }
}
