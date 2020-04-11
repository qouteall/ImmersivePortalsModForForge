package com.qouteall.hiding_in_the_bushes.mixin.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.template.TemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(ChunkStatus.class)
public class MixinChunkStatus {
    //vanilla feature generation is not thread safe
    
    private static ReentrantLock featureGenLock;
    
    @Redirect(
        method = "lambda$static$9",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkGenerator;decorate(Lnet/minecraft/world/gen/WorldGenRegion;)V"
        )
    )
    private static void redirectGenerateFeatures(
        ChunkGenerator chunkGenerator,
        WorldGenRegion chunkRegion
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            featureGenLock.lock();
        }
        chunkGenerator.decorate(chunkRegion);
        if (shouldLock) {
            featureGenLock.unlock();
        }
    }
    
    @Redirect(
        method = "lambda$static$2",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkGenerator;generateStructures(Lnet/minecraft/world/biome/BiomeManager;Lnet/minecraft/world/chunk/IChunk;Lnet/minecraft/world/gen/ChunkGenerator;Lnet/minecraft/world/gen/feature/template/TemplateManager;)V"
        )
    )
    private static void redirectSetStructureStarts(
        ChunkGenerator generator,
        BiomeManager biomeAccess,
        IChunk chunk,
        ChunkGenerator<?> chunkGenerator,
        TemplateManager structureManager
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            featureGenLock.lock();
        }
        generator.generateStructures(
            biomeAccess, chunk, chunkGenerator, structureManager
        );
        if (shouldLock) {
            featureGenLock.unlock();
        }
    }
    
    @Redirect(
        method = "lambda$static$3",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkGenerator;generateStructureStarts(Lnet/minecraft/world/IWorld;Lnet/minecraft/world/chunk/IChunk;)V"
        )
    )
    private static void redirectAddStructureReference(
        ChunkGenerator chunkGenerator, IWorld world, IChunk chunk
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            featureGenLock.lock();
        }
        chunkGenerator.generateStructureStarts(world, chunk);
        if (shouldLock) {
            featureGenLock.unlock();
        }
    }
    
    private static boolean getShouldLock() {
        return AltiusInfo.isAltius();
    }
    
    static {
        featureGenLock = new ReentrantLock(true);
    }
}
