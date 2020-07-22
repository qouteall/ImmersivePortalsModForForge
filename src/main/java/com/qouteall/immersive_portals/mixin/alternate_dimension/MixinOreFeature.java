package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.alternate_dimension.ErrorTerrainGenerator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.OreBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.OreFeature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.structure.StructureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Random;

@Mixin(OreFeature.class)
public class MixinOreFeature {
    @ModifyVariable(
        method = "Lnet/minecraft/world/gen/feature/OreFeature;func_230362_a_(Lnet/minecraft/world/ISeedReader;Lnet/minecraft/world/gen/feature/structure/StructureManager;Lnet/minecraft/world/gen/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/feature/OreFeatureConfig;)Z",
        at = @At("HEAD"),
        argsOnly = true
    )
    private OreFeatureConfig modifyOreFeatureConfig(
        OreFeatureConfig oreFeatureConfig,
        ISeedReader serverWorldAccess,
        StructureManager structureAccessor,
        ChunkGenerator chunkGenerator,
        Random random,
        BlockPos blockPos,
        OreFeatureConfig oreFeatureConfig1
    ) {
        if (chunkGenerator instanceof ErrorTerrainGenerator) {
            BlockState state = oreFeatureConfig.state;
            if (state.getBlock() instanceof OreBlock) {
                if (state.getBlock() != Blocks.COAL_ORE) {
                    return new OreFeatureConfig(
                        oreFeatureConfig.target,
                        oreFeatureConfig.state,
                        oreFeatureConfig.size * 3
                    );
                }
            }
        }
        return oreFeatureConfig;
    }
}
