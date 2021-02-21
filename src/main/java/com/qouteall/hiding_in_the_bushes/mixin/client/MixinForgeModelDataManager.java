package com.qouteall.hiding_in_the_bushes.mixin.client;

import com.google.common.base.Preconditions;
import com.qouteall.forge_model_data_fix.ForgeModelDataManagerPerWorld;
import com.qouteall.hiding_in_the_bushes.ModMainForge;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    
    
    private static ConcurrentHashMap<RegistryKey<World>, ForgeModelDataManagerPerWorld>
        portal_modelDataManagerMap;
    
    static {
        portal_modelDataManagerMap = new ConcurrentHashMap<>();
        
        ModMain.clientTaskList.addTask(() -> {
            if (ModMainForge.enableModelDataFix) {
                ModMain.clientCleanupSignal.connect(MixinForgeModelDataManager::portal_cleanup);
                
                MyClientChunkManager.clientChunkUnloadSignal.connect((chunk) -> {
                    portal_getManager(chunk.getWorld()).onChunkUnload(chunk);
                });
                
                Helper.log("IP Forge Model Data Fix Initialized");
            }
            else {
                Helper.log("IP Forge Model Data Fix is Disabled");
            }
            
            return true;
        });
        
    }
    
    private static void portal_cleanup() {
        portal_modelDataManagerMap.clear();
    }
    
    private static ForgeModelDataManagerPerWorld portal_getManager(World world) {
        RegistryKey<World> key = world.func_234923_W_();
        return portal_modelDataManagerMap.computeIfAbsent(key, k -> new ForgeModelDataManagerPerWorld());
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    private static void cleanCaches(World world) {
        if (!ModMainForge.enableModelDataFix) {
            if (world != currentWorld.get()) {
                currentWorld = new WeakReference<>(world);
                needModelDataRefresh.clear();
                modelDataCache.clear();
            }
        }
    }
    
    @Inject(method = "requestModelDataRefresh", at = @At("HEAD"), cancellable = true)
    private static void onRequestModelDataRefresh(TileEntity te, CallbackInfo ci) {
        if (!ModMainForge.enableModelDataFix) {
            return;
        }
        
        Validate.notNull(te);
        portal_getManager(te.getWorld()).requestModelDataRefresh(te);
        
        ci.cancel();
    }
    
    @Inject(method = "refreshModelData", at = @At("HEAD"), cancellable = true)
    private static void onRefreshModelData(World world, ChunkPos chunk, CallbackInfo ci) {
        if (!ModMainForge.enableModelDataFix) {
            return;
        }
        
        ci.cancel();
    }
    
    @Inject(method = "onChunkUnload", at = @At("HEAD"), cancellable = true)
    private static void onOnChunkUnload(ChunkEvent.Unload event, CallbackInfo ci) {
        if (!ModMainForge.enableModelDataFix) {
            return;
        }
        
        ci.cancel();
    }
    
    @Inject(
        method = "getModelData(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraftforge/client/model/data/IModelData;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onGetModelData1(World world, BlockPos pos, CallbackInfoReturnable<IModelData> cir) {
        if (!ModMainForge.enableModelDataFix) {
            return;
        }
        
        cir.setReturnValue(portal_getManager(world).getModelData(world, pos));
    }
    
    @Inject(
        method = "getModelData(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;)Ljava/util/Map;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onGetModelData2(World world, ChunkPos pos, CallbackInfoReturnable<Map<BlockPos, IModelData>> cir) {
        if (!ModMainForge.enableModelDataFix) {
            return;
        }
        
        cir.setReturnValue(portal_getManager(world).getModelData(world, pos));
    }
}
