package com.qouteall.hiding_in_the_bushes.mixin.client;

import net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ForgeBlockModelRenderer.class,remap = false)
public class MixinForgeBlockModelRenderer {

}
