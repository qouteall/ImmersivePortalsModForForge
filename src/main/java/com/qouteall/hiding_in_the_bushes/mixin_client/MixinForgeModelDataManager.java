package com.qouteall.hiding_in_the_bushes.mixin_client;

import com.google.common.base.Preconditions;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

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
    
    /**
     * @author qouteall
     * @reason avoid crash (this may cause the model system to not work)
     */
    @Overwrite
    private static void cleanCaches(World world) {
        Preconditions.checkNotNull(world, "World must not be null");
        
        if (world != currentWorld.get()) {
            currentWorld = new WeakReference<>(world);
            needModelDataRefresh.clear();
            modelDataCache.clear();
        }
    }
}
