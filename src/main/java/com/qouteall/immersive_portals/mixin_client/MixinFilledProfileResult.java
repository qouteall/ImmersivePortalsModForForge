package com.qouteall.immersive_portals.mixin_client;

import net.minecraft.profiler.FilledProfileResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

@Mixin(value = FilledProfileResult.class, remap = false)
public class MixinFilledProfileResult {
    @Shadow
    @Final
    private Map<String, Long> timesMap;
    @Shadow
    @Final
    private Map<String, Long> field_223508_c;
    
    //fix vanilla bug
    @ModifyVariable(
        method = "getDataPoints",
        argsOnly = true,
        at = @At("HEAD")
    )
    private String modifyKey(String key) {
        return key.replace('.', '\u001e');
    }
}
