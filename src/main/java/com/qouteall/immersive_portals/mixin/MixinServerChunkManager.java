package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEServerChunkManager;
import net.minecraft.world.chunk.ServerChunkProvider;
import net.minecraft.world.chunk.TicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerChunkProvider.class)
public abstract class MixinServerChunkManager implements IEServerChunkManager {
    
    @Override
    @Accessor("ticketManager")
    public abstract TicketManager getTicketManager();

//    @Shadow
//    @Final
//    private ChunkTicketManager ticketManager;
//
//    @Override
//    public ChunkTicketManager getTicketManager() {
//        return ticketManager;
//    }
}
