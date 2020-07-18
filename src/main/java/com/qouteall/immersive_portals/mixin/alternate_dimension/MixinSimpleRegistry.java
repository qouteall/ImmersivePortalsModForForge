package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.ducks.IESimpleRegistry;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(SimpleRegistry.class)
public class MixinSimpleRegistry implements IESimpleRegistry {
    
    @Shadow
    @Final
    private Set<RegistryKey<?>> field_239650_bc_;
    
    @Override
    public void markUnloaded(RegistryKey<?> key) {
        field_239650_bc_.remove(key);
    }
}
