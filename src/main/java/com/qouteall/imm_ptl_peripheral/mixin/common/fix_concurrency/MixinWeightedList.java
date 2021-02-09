package com.qouteall.imm_ptl_peripheral.mixin.common.fix_concurrency;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;
import net.minecraft.util.WeightedList;

@Mixin(WeightedList.class)
public abstract class MixinWeightedList {
    @Shadow
    public abstract WeightedList func_226314_a_(Random random);
    
    // it's not thread safe
    // dimension stack made this vanilla issue trigger more frequently
    @Overwrite
    public Object func_226318_b_(Random random) {
        for (; ; ) {
            try {
                return this.func_226314_a_(random).func_220655_b().findFirst().orElseThrow(RuntimeException::new);
            }
            catch (Throwable throwable) {
                // including ConcurrentModificationException
                throwable.printStackTrace();
            }
        }
    }
}
