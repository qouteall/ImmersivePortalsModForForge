package com.qouteall.immersive_portals.mixin.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusGeneratorType;
import net.minecraft.world.WorldType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldType.class)
public class MixinLevelGeneratorType {
    
    @Invoker("<init>")
    private static WorldType construct(
        int id, String name, String storedName, int version
    ) {
        throw new RuntimeException();
    }
    
    static {
//        AltiusGeneratorType.constructor = MixinLevelGeneratorType::construct;
    }
}
