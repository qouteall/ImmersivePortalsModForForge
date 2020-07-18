package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.ducks.IEWorld;
import net.minecraft.world.World;
import net.minecraft.world.storage.ISpawnWorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public class MixinWorld implements IEWorld {
    
    @Shadow
    @Final
    protected ISpawnWorldInfo worldInfo;
    
    @Override
    public ISpawnWorldInfo myGetProperties() {
        return worldInfo;
    }
}
