package com.qouteall.immersive_portals.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.time.Duration;
import net.minecraft.profiler.Profiler;

@Mixin(Profiler.class)
public class MixinProfilerSystem {
    @Mutable
    @Shadow
    @Final
    private static long WARN_TIME_THRESHOLD;
    
    static {
        WARN_TIME_THRESHOLD = Duration.ofMillis(60).toNanos();
    }
}
