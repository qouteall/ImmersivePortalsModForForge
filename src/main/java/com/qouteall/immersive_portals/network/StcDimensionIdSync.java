package com.qouteall.immersive_portals.network;

import com.mojang.datafixers.util.Pair;
import com.qouteall.immersive_portals.DimensionRegistryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class StcDimensionIdSync {
    public List<Pair<Integer, String>> data;
    
    public StcDimensionIdSync(List<Pair<Integer, String>> data) {
        this.data = data;
    }
    
    public StcDimensionIdSync(PacketBuffer buf) {
        data = new ArrayList<>();
        
        int num = buf.readInt();
        for (int i = 0; i < num; i++) {
            int rawId = buf.readInt();
            String stringId = buf.readString();
            data.add(new Pair<>(rawId, stringId));
        }
    }
    
    public void encode(PacketBuffer buf) {
        buf.writeInt(data.size());
        
        for (Pair<Integer, String> pair : data) {
            buf.writeInt(pair.getFirst());
            buf.writeString(pair.getSecond());
        }
    }
    
    public static StcDimensionIdSync createPacket() {
        List<Pair<Integer, String>> data = DimensionRegistryManager.getServerDimensionTypeId();
        return new StcDimensionIdSync(data);
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        clientOnlyHandle();
        context.get().setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void clientOnlyHandle() {
        Minecraft.getInstance().execute(() -> {
            List<Pair<Integer, String>> data = this.data;
            DimensionRegistryManager.acceptSync(data);
        });
    }
    
    
}
