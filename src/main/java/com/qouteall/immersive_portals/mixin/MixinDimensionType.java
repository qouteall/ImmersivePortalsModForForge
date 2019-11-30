package com.qouteall.immersive_portals.mixin;

import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DimensionType.class)
public abstract class MixinDimensionType {
    
    //change "DimensionType{minecraft:nether}" to "minecraft:nether"
    
    @ModifyConstant(
        method = "toString",
        constant = @Constant(stringValue = "DimensionType{")
    )
    private String modify1(String whatever) {
        return "";
    }
    
    @ModifyConstant(
        method = "toString",
        constant = @Constant(stringValue = "}")
    )
    private String modify2(String whatever) {
        return "";
    }

    
}
