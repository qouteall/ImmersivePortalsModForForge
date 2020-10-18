package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import com.qouteall.imm_ptl_peripheral.altius_world.AltiusGameRule;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(ChunkStatus.class)
public class MixinChunkStatus {
    //vanilla feature generation is not thread safe
    
    private static ReentrantLock featureGenLock;
    
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkGenerator;func_230351_a_(Lnet/minecraft/world/gen/WorldGenRegion;Lnet/minecraft/world/gen/feature/structure/StructureManager;)V"
        )
    )
    private static void redirectGenerateFeatures(
        ChunkGenerator chunkGenerator, WorldGenRegion region, StructureManager accessor
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            featureGenLock.lock();
        }
        try {
            chunkGenerator.func_230351_a_(region, accessor);
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s %d %d",
                ((ServerWorld) region.getChunkProvider().getWorld()).func_234923_W_(),
                region.getMainChunkX(),
                region.getMainChunkZ()
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            featureGenLock.unlock();
        }
    }
    
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkGenerator;func_242707_a(Lnet/minecraft/util/registry/DynamicRegistries;Lnet/minecraft/world/gen/feature/structure/StructureManager;Lnet/minecraft/world/chunk/IChunk;Lnet/minecraft/world/gen/feature/template/TemplateManager;J)V"
        )
    )
    private static void redirectSetStructureStarts(
        ChunkGenerator generator,
        DynamicRegistries dynamicRegistryManager, StructureManager structureAccessor, IChunk chunk, TemplateManager structureManager, long worldSeed
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            featureGenLock.lock();
        }
        try {
            generator.func_242707_a(dynamicRegistryManager, structureAccessor, chunk, structureManager, worldSeed);
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s",
                chunk
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            featureGenLock.unlock();
        }
    }
    
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkGenerator;func_235953_a_(Lnet/minecraft/world/ISeedReader;Lnet/minecraft/world/gen/feature/structure/StructureManager;Lnet/minecraft/world/chunk/IChunk;)V"
        )
    )
    private static void redirectAddStructureReference(
        ChunkGenerator chunkGenerator,
        ISeedReader structureWorldAccess, StructureManager accessor, IChunk chunk
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            featureGenLock.lock();
        }
        try {
            chunkGenerator.func_235953_a_(structureWorldAccess, accessor, chunk);
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s",
                chunk
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            featureGenLock.unlock();
        }
    }
    
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkGenerator;func_230350_a_(JLnet/minecraft/world/biome/BiomeManager;Lnet/minecraft/world/chunk/IChunk;Lnet/minecraft/world/gen/GenerationStage$Carving;)V"
        )
    )
    private static void redirectCarve(
        ChunkGenerator generator, long seed, BiomeManager access, IChunk chunk, GenerationStage.Carving carver
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            featureGenLock.lock();
        }
        try {
            generator.func_230350_a_(seed, access, chunk, carver);
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s",
                chunk
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            featureGenLock.unlock();
        }
    }
    
    private static boolean getShouldLock() {
        return AltiusGameRule.getIsDimensionStackCache();
    }
    
    static {
        featureGenLock = new ReentrantLock(true);
    }
}
