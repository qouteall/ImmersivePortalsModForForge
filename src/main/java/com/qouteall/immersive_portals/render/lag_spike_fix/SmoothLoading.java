package com.qouteall.immersive_portals.render.lag_spike_fix;

import com.qouteall.hiding_in_the_bushes.MyNetworkClient;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.IClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SChunkDataPacket;
import net.minecraft.network.play.server.SUnloadChunkPacket;
import net.minecraft.network.play.server.SUpdateLightPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

// intercept the load/unload packets and apply them bit by bit later
// to reduce lag spike
// has issues currently, disabled by default
@OnlyIn(Dist.CLIENT)
public class SmoothLoading {
    
    public static final Minecraft client = Minecraft.getInstance();
    
    public static class WorldInfo {
        public List<IPacket<IClientPlayNetHandler>> packets = new ArrayList<>();
        
        public WorldInfo() {
        }
        
        public void removeChunkLoadingPackets(int cx, int cz) {
            this.packets.removeIf(p -> {
                if (p instanceof SChunkDataPacket) {
                    SChunkDataPacket pt = (SChunkDataPacket) p;
                    if (pt.getChunkX() == cx && pt.getChunkZ() == cz) {
                        return true;
                    }
                }
                else if (p instanceof SUpdateLightPacket) {
                    SUpdateLightPacket pt = (SUpdateLightPacket) p;
                    if (pt.getChunkX() == cx && pt.getChunkZ() == cz) {
                        return true;
                    }
                }
                return false;
            });
        }
        
        public void removeChunkUnloadPackets(int cx, int cz) {
            packets.removeIf(p -> {
                if (p instanceof SUnloadChunkPacket) {
                    SUnloadChunkPacket pt = (SUnloadChunkPacket) p;
                    return pt.getX() == cx && pt.getZ() == cz;
                }
                return false;
            });
        }
        
        public void sortByDistanceToBarycenter() {
            long xSum = 0;
            long zSum = 0;
            
            for (IPacket<IClientPlayNetHandler> packet : packets) {
                ChunkPos chunkPos = getChunkPosOf(packet);
                xSum += chunkPos.x;
                zSum += chunkPos.z;
            }
            
            int centerX = (int) (xSum / packets.size());
            int centerZ = (int) (zSum / packets.size());
            
            packets.sort(Comparator.comparingDouble((IPacket<IClientPlayNetHandler> packet) -> {
                ChunkPos chunkPos = getChunkPosOf(packet);
                return -Helper.getChebyshevDistance(
                    chunkPos.x, chunkPos.z,
                    centerX, centerZ
                );
            }));
        }
    }
    
    private static final WeakHashMap<ClientWorld, WorldInfo> data = new WeakHashMap<>();
    
    private static int coolDown = getInterval();
    
    public static void init() {
        ModMain.postClientTickSignal.connect(SmoothLoading::tick);
    }
    
    private static int getSplitRatio() {
        return 150;
    }
    
    private static WorldInfo getWorldInfo(ClientWorld world) {
        return data.computeIfAbsent(
            world, k -> new WorldInfo()
        );
    }
    
    private static boolean isNearPlayer(ClientWorld world, int chunkX, int chunkZ) {
        if (world != client.player.world) {
            return false;
        }
        
        return Helper.getChebyshevDistance(
            chunkX, chunkZ,
            client.player.chunkCoordX, client.player.chunkCoordZ
        ) <= (client.gameSettings.renderDistanceChunks + 5);
    }
    
    // return true indicates that the packet is intercepted
    public static boolean filterPacket(ClientWorld world, IPacket<IClientPlayNetHandler> packet) {
        if (!Global.smoothLoading) {
            return false;
        }
        
        if (packet instanceof SChunkDataPacket) {
            SChunkDataPacket p = (SChunkDataPacket) packet;
            if (!isNearPlayer(world, p.getChunkX(), p.getChunkZ())) {
                interceptPacket(world, packet);
                return true;
            }
        }
        else if (packet instanceof SUpdateLightPacket) {
            SUpdateLightPacket p = (SUpdateLightPacket) packet;
            if (!isNearPlayer(world, p.getChunkX(), p.getChunkZ())) {
                interceptPacket(world, packet);
                return true;
            }
        }
        else if (packet instanceof SUnloadChunkPacket) {
            SUnloadChunkPacket p = (SUnloadChunkPacket) packet;
            if (!isNearPlayer(world, p.getX(), p.getZ())) {
                interceptPacket(world, packet);
                return true;
            }
        }
        
        return false;
    }
    
    private static void interceptPacket(
        ClientWorld world,
        IPacket<IClientPlayNetHandler> packet
    ) {
        WorldInfo worldInfo = getWorldInfo(world);
        
        if (packet instanceof SUnloadChunkPacket) {
            SUnloadChunkPacket packetT = (SUnloadChunkPacket) packet;
            worldInfo.removeChunkLoadingPackets(packetT.getX(), packetT.getZ());
        }
        else if (packet instanceof SChunkDataPacket) {
            SChunkDataPacket packetT = (SChunkDataPacket) packet;
            worldInfo.removeChunkUnloadPackets(packetT.getChunkX(), packetT.getChunkZ());
        }
        else if (packet instanceof SUpdateLightPacket) {
            SUpdateLightPacket packetT = (SUpdateLightPacket) packet;
            worldInfo.removeChunkUnloadPackets(packetT.getChunkX(), packetT.getChunkZ());
        }
        
        worldInfo.packets.add(packet);
    }
    
    private static void applyPacket(
        ClientWorld world,
        IPacket<IClientPlayNetHandler> packet
    ) {
        MyNetworkClient.doProcessRedirectedMessage(world, packet);
    }
    
    private static void tick() {
        if (client.world == null) {
            return;
        }
        if (client.player == null) {
            return;
        }
        
        coolDown--;
        
        if (coolDown <= 0) {
            coolDown = getInterval();
            flushInterceptedPackets();
        }
    }
    
    public static void cleanUp() {
        data.clear();
    }
    
    private static void flushInterceptedPackets() {
        client.getProfiler().startSection("flush_intercepted_packets");
        data.entrySet().stream().max(
            Comparator.comparingInt(entry -> entry.getValue().packets.size())
        ).ifPresent(entry -> {
            flushPacketsForWorld(entry.getKey(), entry.getValue());
        });
        client.getProfiler().endSection();
    }
    
    private static void flushPacketsForWorld(ClientWorld world, WorldInfo info) {
        if (info.packets.isEmpty()) {
            return;
        }
        
        info.sortByDistanceToBarycenter();
        
        int packets = (int) (info.packets.size() / ((double) getSplitRatio()));
        
        if (packets == 0) {
            packets = 1;
        }
        
        for (int i = 0; i < packets; i++) {
            IPacket<IClientPlayNetHandler> lastPacket = info.packets.get(info.packets.size() - 1);
            info.packets.remove(info.packets.size() - 1);
            applyPacket(world, lastPacket);
        }
    }
    
    private static ChunkPos getChunkPosOf(IPacket<IClientPlayNetHandler> packet) {
        if (packet instanceof SChunkDataPacket) {
            return new ChunkPos(
                ((SChunkDataPacket) packet).getChunkX(),
                ((SChunkDataPacket) packet).getChunkZ()
            );
        }
        else if (packet instanceof SUpdateLightPacket) {
            return new ChunkPos(
                ((SUpdateLightPacket) packet).getChunkX(),
                ((SUpdateLightPacket) packet).getChunkZ()
            );
        }
        else if (packet instanceof SUnloadChunkPacket) {
            return new ChunkPos(
                ((SUnloadChunkPacket) packet).getX(),
                ((SUnloadChunkPacket) packet).getZ()
            );
        }
        throw new RuntimeException("oops");
    }
    
    private static int getInterval() {
        return 1;
    }
}
