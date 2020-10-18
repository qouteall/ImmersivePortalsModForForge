package com.qouteall.immersive_portals.mixin.common;

import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEServerWorld;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.IServerWorldInfo;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld implements IEServerWorld {
    
    @Shadow
    public abstract DimensionSavedDataManager getSavedData();
    
    @Shadow
    public abstract ServerChunkProvider getChunkProvider();
    
    @Shadow @Final private IServerWorldInfo field_241103_E_;
    private static LongSortedSet dummy;
    
    static {
        dummy = new LongLinkedOpenHashSet();
        dummy.add(23333);
    }
    
    //in vanilla if a dimension has no player and no forced chunks then it will not tick
//    @Redirect(
//        method = "tick",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/server/world/ServerWorld;getForcedChunks()Lit/unimi/dsi/fastutil/longs/LongSet;"
//        )
//    )
//    private LongSet redirectGetForcedChunks(ServerWorld world) {
//        if (NewChunkTrackingGraph.shouldLoadDimension(world.getRegistryKey())) {
//            return dummy;
//        }
//        else {
//            return world.getForcedChunks();
//        }
//    }
    
    //in vanilla if a dimension has no player and no forced chunks then it will not tick
    @Redirect(
        method = "Lnet/minecraft/world/server/ServerWorld;tick(Ljava/util/function/BooleanSupplier;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z"
        )
    )
    private boolean redirectIsEmpty(List list) {
        final ServerWorld this_ = (ServerWorld) (Object) this;
        if (NewChunkTrackingGraph.shouldLoadDimension(this_.func_234923_W_())) {
            return false;
        }
        return list.isEmpty();
    }
    
    // for debug
    @Inject(method = "Lnet/minecraft/world/server/ServerWorld;toString()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void onToString(CallbackInfoReturnable<String> cir) {
        final ServerWorld this_ = (ServerWorld) (Object) this;
        cir.setReturnValue("ServerWorld " + this_.func_234923_W_().func_240901_a_() +
            " " + field_241103_E_.getWorldName());
    }
}
