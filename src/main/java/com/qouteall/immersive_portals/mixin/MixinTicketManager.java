package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEChunkTicketManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TicketManager.class, remap = false)
public class MixinTicketManager implements IEChunkTicketManager {
    @Shadow
    private long currentTime;
    
    @Shadow
    @Final
    private Long2ObjectMap<ObjectSet<ServerPlayerEntity>> playersByChunkPos;
    
    //avoid NPE
    @Inject(method = "removePlayer", at = @At("HEAD"))
    private void onHandleChunkLeave(
        SectionPos chunkSectionPos_1,
        ServerPlayerEntity serverPlayerEntity_1,
        CallbackInfo ci
    ) {
        long long_1 = chunkSectionPos_1.asChunkPos().asLong();
        playersByChunkPos.putIfAbsent(long_1, new ObjectOpenHashSet<>());
    }
}
