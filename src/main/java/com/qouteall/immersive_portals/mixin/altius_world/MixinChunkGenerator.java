package com.qouteall.immersive_portals.mixin.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusGeneratorType;
import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.WorldGenRegion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {
    @Shadow
    @Final
    protected IWorld world;
    private static ReentrantLock featureGenLock;
    
    //Vanilla feature generation is not thread safe
    @Inject(
        method = "Lnet/minecraft/world/gen/ChunkGenerator;decorate(Lnet/minecraft/world/gen/WorldGenRegion;)V",
        at = @At("HEAD")
    )
    private void onStartGeneratingFeatures(WorldGenRegion region, CallbackInfo ci) {
        if (shouldLock()) {
            featureGenLock.lock();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/world/gen/ChunkGenerator;decorate(Lnet/minecraft/world/gen/WorldGenRegion;)V",
        at = @At("RETURN")
    )
    private void onEndGeneratingFeatures(WorldGenRegion region, CallbackInfo ci) {
        if (shouldLock()) {
            featureGenLock.unlock();
        }
    }
    
    private boolean shouldLock() {
        return AltiusInfo.isAltius();
    }
    
    static {
        featureGenLock = new ReentrantLock(true);
    }
    
}
