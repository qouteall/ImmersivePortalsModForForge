package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import com.qouteall.imm_ptl_peripheral.altius_world.AltiusInfo;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChunkStatus.class)
public class MixinChunkStatus {
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkGenerator;generateSurface(Lnet/minecraft/world/gen/WorldGenRegion;Lnet/minecraft/world/chunk/IChunk;)V"
        )
    )
    private static void redirectGenerateSurface(
        ChunkGenerator chunkGenerator,
        WorldGenRegion region, IChunk chunk
    ) {
        chunkGenerator.generateSurface(region, chunk);
        AltiusInfo.replaceBedrock(region.getWorld(), chunk);
    }
    
}
