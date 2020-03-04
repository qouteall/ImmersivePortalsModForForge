package com.qouteall.hiding_in_the_bushes.mixin;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = NetworkHooks.class, remap = false)
public class MixinForgeNetworkHooks {
    
    /**
     * @author qouteall
     * @reason The dimension sync will be manage by this mod
     */
    @Overwrite
    public static DimensionType getDummyDimType(final int dimension) {
        return DimensionType.getById(dimension);
    }
    
    /**
     * @author qouteall
     * @reason The dimension sync will be manage by this mod
     */
    @Overwrite
    static public void addCachedDimensionType(
        final DimensionType dimensionType,
        final ResourceLocation dimName
    ) {
    
    }
}
