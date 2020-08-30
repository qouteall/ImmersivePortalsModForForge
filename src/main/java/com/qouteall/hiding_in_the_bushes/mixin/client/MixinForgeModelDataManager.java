package com.qouteall.hiding_in_the_bushes.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;

@Mixin(value = ModelDataManager.class, remap = false)
public class MixinForgeModelDataManager {
    @Shadow
    private static WeakReference<World> currentWorld;

    @Shadow
    @Final
    private static Map<ChunkPos, Set<BlockPos>> needModelDataRefresh;

    @Shadow
    @Final
    private static Map<ChunkPos, Map<BlockPos, IModelData>> modelDataCache;

    @Inject(method = "cleanCaches", at = @At("HEAD"), cancellable = true)
    private static void onCleanCaches(World world, CallbackInfo ci) {
        if (world != Minecraft.getInstance().world) {
            //the other patch will supply the model data
            needModelDataRefresh.clear();
            modelDataCache.clear();
            ci.cancel();
        }
    }
}
