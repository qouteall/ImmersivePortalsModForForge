package com.qouteall.immersive_portals.mixin;

import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DimensionType.class)
public abstract class MixinDimensionType {
    
    @Mutable
    @Shadow
    @Final
    private int id;
    
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
