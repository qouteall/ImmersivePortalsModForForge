package com.qouteall.hiding_in_the_bushes.mixin.alternate_dimension;

import net.minecraft.world.spawner.AbstractSpawner;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractSpawner.class)
public class MixinMobSpawnerLogic {
    //TODO spawn entities ignoring light in alternate dimension
}
