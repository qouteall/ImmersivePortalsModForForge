package com.qouteall.immersive_portals.mixin.chunk_sync;

import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEChunkHolder;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.network.NetworkMain;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ChunkHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ChunkHolder.class)
public class MixinChunkHolder implements IEChunkHolder {
    
    @Shadow
    @Final
    private ChunkPos pos;
    
    @Shadow
    @Final
    private ChunkHolder.IPlayerProvider playerProvider;
    
    /**
     * @author qouteall
     * @reason whatever
     */
    @Overwrite
    private void sendToTracking(IPacket<?> packet_1, boolean boolean_1) {
        DimensionType dimension =
            ((IEThreadedAnvilChunkStorage) playerProvider).getWorld().dimension.getType();
    
        NewChunkTrackingGraph.getPlayersViewingChunk(
            dimension, pos.x, pos.z
        ).forEach(player ->
            NetworkMain.sendRedirected(
                player,
                dimension,
                packet_1
            )
        );
    
    }
    
}
