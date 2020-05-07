package com.qouteall.hiding_in_the_bushes.mixin_client;

import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRender;
import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRenderDispatcher;
import com.qouteall.hiding_in_the_bushes.fix_model_data.IEChunkRenderTask;
import com.qouteall.hiding_in_the_bushes.fix_model_data.ModelDataHacker;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderCache;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$ChunkRender$RebuildTask")
public class MixinRebuildTask {
//    @Shadow(remap = false, aliases = "this$1")
//    private ChunkRenderDispatcher.ChunkRender this$1;
//
//    @Inject(
//        method = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$ChunkRender$RebuildTask;<init>(Lnet/minecraft/util/math/ChunkPos;DLnet/minecraft/client/renderer/chunk/ChunkRenderCache;)V",
//        at = @At("RETURN")
//    )
//    private void onInitEnded(
//        ChunkPos pos,
//        double distanceSqIn,
//        ChunkRenderCache renderCacheIn,
//        CallbackInfo ci
//    ) {
//        World world = ((IEChunkRenderDispatcher) ((IEChunkRender) this$1).getParent()).myGetWorld();
//
//        ((IEChunkRenderTask) this).portal_setDimension(world.dimension.getType());
//
////        ClientWorld playerWorld = Minecraft.getInstance().world;
////        if (CGlobal.renderer.isRendering()) {
////            playerWorld = CGlobal.clientWorldLoader.getWorld(MyRenderHelper.originalPlayerDimension);
////        }
////
////        if (world != playerWorld) {
////            Helper.log("Use new model data");
////            ((IEChunkRenderTask) this).setModelData(
////                ModelDataHacker.getChunkModelData(world, pos)
////            );
////        }
//    }
}