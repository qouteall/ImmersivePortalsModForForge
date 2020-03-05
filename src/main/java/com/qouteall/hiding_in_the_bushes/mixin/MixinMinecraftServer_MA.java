package com.qouteall.hiding_in_the_bushes.mixin;

import com.qouteall.hiding_in_the_bushes.ModMainForge;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer_MA {
    static {
        ModMainForge.isServerMixinApplied = true;
    }
}
