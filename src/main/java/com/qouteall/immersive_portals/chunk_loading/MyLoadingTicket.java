package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEChunkTicketManager;
import com.qouteall.immersive_portals.ducks.IEServerChunkManager;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import java.util.Comparator;
import java.util.WeakHashMap;

public class MyLoadingTicket {
    public static final TicketType<ChunkPos> ticketType =
        TicketType.create("imm_ptl", Comparator.comparingLong(ChunkPos::asLong));
    
    private static TicketManager getTicketManager(ServerWorld world) {
        return ((IEServerChunkManager) world.getChunkProvider()).getTicketManager();
    }
    
    public static final WeakHashMap<ServerWorld, LongSortedSet>
        loadedChunkRecord = new WeakHashMap<>();
    
    private static boolean hasOtherChunkTicket(ServerWorld world, ChunkPos chunkPos) {
        SortedArraySet<Ticket<?>> chunkTickets =
            ((IEChunkTicketManager) getTicketManager(world))
                .portal_getTicketSet(chunkPos.asLong());
        return chunkTickets.stream().anyMatch(t -> t.getType() != ticketType);
    }
    
    public static void addTicketIfNotLoaded(ServerWorld world, ChunkPos chunkPos) {
        boolean isNewlyAdded = getRecord(world).add(chunkPos.asLong());
        if (isNewlyAdded) {
            getTicketManager(world).register(
                ticketType, chunkPos, getLoadingRadius(), chunkPos
            );
        }
    }
    
    public static void removeTicket(ServerWorld world, ChunkPos chunkPos) {
        boolean isNewlyRemoved = getRecord(world).remove(chunkPos.asLong());
        
        if (isNewlyRemoved) {
            getTicketManager(world).release(
                ticketType, chunkPos, getLoadingRadius(), chunkPos
            );
        }
    }
    
    public static int getLoadingRadius() {
        if (Global.activeLoading) {
            return 2;
        }
        else {
            return 1;
        }
    }
    
    public static LongSortedSet getRecord(ServerWorld world) {
        return loadedChunkRecord.computeIfAbsent(
            world, k -> new LongLinkedOpenHashSet()
        );
    }
}
