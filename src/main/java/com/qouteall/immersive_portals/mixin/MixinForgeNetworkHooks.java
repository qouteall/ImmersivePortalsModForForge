package com.qouteall.immersive_portals.mixin;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NetworkHooks.class)
public class MixinForgeNetworkHooks {
    
    /**
     * @author qouteall
     * @reason Forge's dimension sync system is a mess.
     */
    @Overwrite
    static public void addCachedDimensionType(
        final DimensionType dimensionType,
        final ResourceLocation dimName
    ) {
    
    }
}
