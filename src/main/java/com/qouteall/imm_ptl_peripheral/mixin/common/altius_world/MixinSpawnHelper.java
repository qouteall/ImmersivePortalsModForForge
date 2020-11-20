package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import com.qouteall.imm_ptl_peripheral.altius_world.AltiusGameRule;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.spawner.WorldEntitySpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldEntitySpawner.class)
public class MixinSpawnHelper {
    
    //avoid spawning on top of nether in altius world
    //normally mob cannot spawn on bedrock but altius replaces it with obsidian
    @Redirect(
        method = "Lnet/minecraft/world/spawner/WorldEntitySpawner;getRandomHeight(Lnet/minecraft/world/World;Lnet/minecraft/world/chunk/Chunk;)Lnet/minecraft/util/math/BlockPos;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/Chunk;getTopBlockY(Lnet/minecraft/world/gen/Heightmap$Type;II)I"
        )
    )
    private static int redirectGetTopY(
        Chunk chunk,
        Heightmap.Type type,
        int x,
        int z
    ) {
        int height = chunk.getTopBlockY(type, x, z);
        int dimHeight = chunk.getWorld().func_234938_ad_();
        if (AltiusGameRule.getIsDimensionStack()) {
            if (chunk.getWorld().func_234923_W_() == World.field_234919_h_) {
                return Math.min(height, dimHeight - 3);
            }
        }
        return height;
    }
}
