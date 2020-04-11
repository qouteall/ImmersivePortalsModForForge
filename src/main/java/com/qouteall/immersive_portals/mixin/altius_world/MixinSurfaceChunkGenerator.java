package com.qouteall.immersive_portals.mixin.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationSettings;
import net.minecraft.world.gen.NoiseChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Random;

@Mixin(NoiseChunkGenerator.class)
public abstract class MixinSurfaceChunkGenerator<T extends GenerationSettings> extends ChunkGenerator<T> {
    public MixinSurfaceChunkGenerator(
        IWorld world,
        BiomeProvider biomeSource,
        T config
    ) {
        super(world, biomeSource, config);
        throw new RuntimeException();
    }
    
    @Inject(
        method = "Lnet/minecraft/world/gen/NoiseChunkGenerator;makeBedrock(Lnet/minecraft/world/chunk/IChunk;Ljava/util/Random;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onBuildBedrock(IChunk chunk, Random random, CallbackInfo ci) {
        if (AltiusInfo.isAltius()) {
            buildAltiusBedrock(chunk, random);
            ci.cancel();
        }
    }
    
    private void buildAltiusBedrock(IChunk chunk, Random random) {
        BlockState fillerBlock = Blocks.OBSIDIAN.getDefaultState();
        
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int i = chunk.getPos().getXStart();
        int j = chunk.getPos().getZStart();
        T chunkGeneratorConfig = this.getSettings();
        int minY = chunkGeneratorConfig.getBedrockFloorHeight();
        int maxY = chunkGeneratorConfig.getBedrockRoofHeight();
        Iterator iterator = BlockPos.getAllInBoxMutable(i, 0, j, i + 15, 0, j + 15).iterator();
        
        while (true) {
            BlockPos blockPos;
            int n;
            
            do {
                if (!iterator.hasNext()) {
                    return;
                }
                
                blockPos = (BlockPos) iterator.next();
                if (maxY > 0) {
                    for (n = maxY; n >= maxY - 4; --n) {
                        if (n >= maxY - random.nextInt(5)) {
                            chunk.setBlockState(mutable.setPos(blockPos.getX(), n, blockPos.getZ()),
                                fillerBlock, false
                            );
                        }
                    }
                }
            } while (minY >= 256);
            
            for (n = minY + 4; n >= minY; --n) {
                if (n <= minY + random.nextInt(5)) {
                    chunk.setBlockState(mutable.setPos(blockPos.getX(), n, blockPos.getZ()),
                        fillerBlock, false
                    );
                }
            }
        }
    }
    
}
