package com.qouteall.hiding_in_the_bushes.mixin_client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qouteall.hiding_in_the_bushes.fix_model_data.ModelDataHacker;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer;
import net.minecraftforge.client.model.pipeline.VertexLighterFlat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Random;

@Mixin(ForgeBlockModelRenderer.class)
public class MixinForgeBlockModelRenderer {
    @ModifyArg(
        method = "render",
        at = @At("HEAD")
    )
    private static IModelData modifyModelData(
        VertexLighterFlat lighter,
        ILightReader world,
        IBakedModel model,
        BlockState state,
        BlockPos pos,
        MatrixStack matrixStack,
        boolean checkSides,
        Random rand,
        long seed,
        IModelData modelData
    ) {
        if (modelData != null) {
            return modelData;
        }
        
        return ModelDataHacker.fetchMissingModelData(world, pos);
    }
}
