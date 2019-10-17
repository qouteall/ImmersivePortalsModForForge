package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import com.qouteall.immersive_portals.exposer.IEClientWorld;
import com.qouteall.immersive_portals.exposer.IEWorld;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class MixinClientWorld implements IEClientWorld {
    @Shadow
    @Final
    @Mutable
    private ClientPlayNetHandler netHandler;
    
    @Override
    public ClientPlayNetHandler getNetHandler() {
        return netHandler;
    }
    
    @Override
    public void setNetHandler(ClientPlayNetHandler handler) {
        netHandler = handler;
    }
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    void onConstructed(
        ClientPlayNetHandler clientPlayNetworkHandler_1,
        WorldSettings levelInfo_1,
        DimensionType dimensionType_1,
        int int_1,
        IProfiler profiler_1,
        WorldRenderer worldRenderer_1,
        CallbackInfo ci
    ) {
        ClientWorld clientWorld = (ClientWorld) (Object) this;
        MyClientChunkManager chunkManager = new MyClientChunkManager(clientWorld, int_1);
        ((IEWorld) this).setChunkManager(chunkManager);
    }
    
    //avoid entity duplicate when an entity travels
    @Inject(
        method = "addEntityImpl",
        at = @At("TAIL")
    )
    private void onOnEntityAdded(int entityId, Entity entityIn, CallbackInfo ci) {
        CGlobal.clientWorldLoader.clientWorldMap.values().stream()
            .filter(world -> world != (Object) this)
            .forEach(world -> world.removeEntityFromWorld(entityId));
    }
}
