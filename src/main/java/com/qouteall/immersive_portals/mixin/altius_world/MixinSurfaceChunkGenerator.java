package com.qouteall.immersive_portals.mixin.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.DimensionSettings;
import net.minecraft.world.gen.NoiseChunkGenerator;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Random;

@Mixin(NoiseChunkGenerator.class)
public abstract class MixinSurfaceChunkGenerator extends ChunkGenerator {
    
    
    @Shadow @Final protected DimensionSettings field_236080_h_;
    
    @Shadow @Final private int field_236085_x_;
    
    public MixinSurfaceChunkGenerator(BiomeProvider biomeSource, DimensionStructuresSettings arg) {
        super(biomeSource, arg);
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
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int i = chunk.getPos().getXStart();
        int j = chunk.getPos().getZStart();
        int k = this.field_236080_h_.func_236118_f_();
        int l = this.field_236085_x_ - 1 - this.field_236080_h_.func_236117_e_();
        boolean bl = l + 4 >= 0 && l < this.field_236085_x_;
        boolean bl2 = k + 4 >= 0 && k < this.field_236085_x_;
        if (bl || bl2) {
            Iterator var11 = BlockPos.getAllInBoxMutable(i, 0, j, i + 15, 0, j + 15).iterator();
        
            while(true) {
                BlockPos blockPos;
                int o;
                do {
                    if (!var11.hasNext()) {
                        return;
                    }
                
                    blockPos = (BlockPos)var11.next();
                    if (bl) {
                        for(o = 0; o < 5; ++o) {
                            if (o <= random.nextInt(5)) {
                                chunk.setBlockState(mutable.setPos(blockPos.getX(), l - o, blockPos.getZ()), Blocks.OBSIDIAN.getDefaultState(), false);
                            }
                        }
                    }
                } while(!bl2);
            
                for(o = 4; o >= 0; --o) {
                    if (o <= random.nextInt(5)) {
                        chunk.setBlockState(mutable.setPos(blockPos.getX(), k + o, blockPos.getZ()), Blocks.OBSIDIAN.getDefaultState(), false);
                    }
                }
            }
        }
    }
    
}
