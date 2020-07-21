package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Supplier;

@Mixin(ClientWorld.class)
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
    
    //use my client chunk manager
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    void onConstructed(
        ClientPlayNetHandler clientPlayNetworkHandler,
        ClientWorld.ClientWorldInfo properties,
        RegistryKey<World> registryKey,
        RegistryKey<DimensionType> registryKey2,
        DimensionType dimensionType,
        int chunkLoadDistance,
        Supplier<IProfiler> supplier,
        WorldRenderer worldRenderer,
        boolean bl,
        long l,
        CallbackInfo ci
    ) {
        ClientWorld clientWorld = (ClientWorld) (Object) this;
        MyClientChunkManager myClientChunkManager =
            new MyClientChunkManager(clientWorld, chunkLoadDistance);
        field_239129_E_ = myClientChunkManager;
    }
    
    //avoid entity duplicate when an entity travels
    @Inject(
        method = "Lnet/minecraft/client/world/ClientWorld;addEntityImpl(ILnet/minecraft/entity/Entity;)V",
        at = @At("TAIL")
    )
    private void onOnEntityAdded(int entityId, Entity entityIn, CallbackInfo ci) {
        CGlobal.clientWorldLoader.clientWorldMap.values().stream()
            .filter(world -> world != (Object) this)
            .forEach(world -> world.removeEntityFromWorld(entityId));
    }
    
}
