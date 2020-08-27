package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.ducks.IEGeneratorOptions;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.Dimension;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DimensionGeneratorSettings.class)
public class MixinGeneratorOptions implements IEGeneratorOptions {
    
    @Shadow
    @Final
    @Mutable
    private SimpleRegistry<Dimension> field_236208_h_;
    
    @Override
    public void setDimOptionRegistry(SimpleRegistry<Dimension> reg) {
        field_236208_h_ = reg;
    }
    
}
