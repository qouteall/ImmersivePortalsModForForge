package com.qouteall.immersive_portals.mixin.client;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.client.multiplayer.ClientChunkProvider;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Supplier;

@Mixin(value = ClientWorld.class)
public abstract class MixinClientWorld implements IEClientWorld {
    @Shadow
    @Final
    @Mutable
    private ClientPlayNetHandler connection;
    
    @Mutable
    @Shadow
    @Final
    private ClientChunkProvider field_239129_E_;
    
    @Shadow
    public abstract Entity getEntityByID(int id);
    
    private List<GlobalTrackedPortal> globalTrackedPortals;
    
    @Override
    public ClientPlayNetHandler getNetHandler() {
        return connection;
    }
    
    @Override
    public void setNetHandler(ClientPlayNetHandler handler) {
        connection = handler;
    }
    
    @Override
    public List<GlobalTrackedPortal> getGlobalPortals() {
        return globalTrackedPortals;
    }
    
    @Override
    public void setGlobalPortals(List<GlobalTrackedPortal> arg) {
        globalTrackedPortals = arg;
    }

//    @Redirect(
//        method = "<init>",
//        at = @At(
//            value = "NEW",
//            target = "net/minecraft/client/world/ClientChunkManager"
//        )
//    )
//    private ClientChunkManager replaceChunkManager(ClientWorld world, int loadDistance) {
//        return O_O.createMyClientChunkManager(world, loadDistance);
//    }
    
    //use my client chunk manager
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    void onConstructed(
        ClientPlayNetHandler clientPlayNetworkHandler, ClientWorld.ClientWorldInfo properties,
        RegistryKey<World> registryKey, DimensionType dimensionType, int i,
        Supplier<IProfiler> supplier, WorldRenderer worldRenderer, boolean bl,
        long l, CallbackInfo ci
    ) {
        ClientWorld clientWorld = (ClientWorld) (Object) this;
        ClientChunkProvider myClientChunkManager =
            O_O.createMyClientChunkManager(clientWorld, i);
        field_239129_E_ = myClientChunkManager;
    }
    
    // avoid entity duplicate when an entity travels
    @Inject(
        method = "Lnet/minecraft/client/world/ClientWorld;addEntityImpl(ILnet/minecraft/entity/Entity;)V",
        at = @At("TAIL")
    )
    private void onOnEntityAdded(int entityId, Entity entityIn, CallbackInfo ci) {
        for (ClientWorld world : CGlobal.clientWorldLoader.clientWorldMap.values()) {
            if (world != (Object) this) {
                world.removeEntityFromWorld(entityId);
            }
        }
    }
    
    /**
     * If the player goes into a portal when the other side chunk is not yet loaded
     * freeze the player so the player won't drop
     * {@link ClientPlayerEntity#tick()}
     */
    @Inject(method = "Lnet/minecraft/client/world/ClientWorld;isBlockLoaded(Lnet/minecraft/util/math/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void onIsChunkLoaded(int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
        Chunk chunk = field_239129_E_.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null || chunk instanceof EmptyChunk) {
            cir.setReturnValue(false);
//            Helper.log("chunk not loaded");
//            new Throwable().printStackTrace();
        }
    }
    
    // for debug
    @Inject(method = "Lnet/minecraft/client/world/ClientWorld;toString()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void onToString(CallbackInfoReturnable<String> cir) {
        ClientWorld this_ = (ClientWorld) (Object) this;
        cir.setReturnValue("ClientWorld " + this_.func_234923_W_().func_240901_a_());
    }
}
