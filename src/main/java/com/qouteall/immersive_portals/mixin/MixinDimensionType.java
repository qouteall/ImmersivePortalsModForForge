package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Helper;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(DimensionType.class)
public abstract class MixinDimensionType {
    
    /**
     * @author qouteall
     * @reason turn DimensionType{minecraft:overworld} to minecraft:overworld
     */
    @Overwrite
    public String toString() {
        return Helper.dimensionTypeToString((DimensionType) (Object) this);
    }
    
}
