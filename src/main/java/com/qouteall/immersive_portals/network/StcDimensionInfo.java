package com.qouteall.immersive_portals.network;

import com.qouteall.immersive_portals.DimensionSyncManager;
import com.qouteall.immersive_portals.Helper;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class StcDimensionInfo {
    
    public static class MyEntry {
        public ResourceLocation stringId;
        public boolean skylight;
        public int registryId;
        public ResourceLocation modDimensionName;
        public PacketBuffer extraData;
        
        public MyEntry(DimensionType type) {
            Validate.isTrue(!type.isVanilla());
            
            registryId = type.getId() + 1;
            stringId = type.getRegistryName();
            modDimensionName = type.getModType().getRegistryName();
            skylight = type.hasSkyLight();
            extraData = new PacketBuffer(Unpooled.buffer());
            type.getModType().write(extraData, true);
        }
        
        public MyEntry(PacketBuffer buffer) {
            registryId = buffer.readInt();
            stringId = buffer.readResourceLocation();
            modDimensionName = buffer.readResourceLocation();
            skylight = buffer.readBoolean();
            extraData = new PacketBuffer(Unpooled.wrappedBuffer(buffer.readByteArray()));
        }
        
        public void encode(PacketBuffer buffer) {
            buffer.writeInt(registryId);
            buffer.writeResourceLocation(stringId);
            buffer.writeResourceLocation(modDimensionName);
            buffer.writeBoolean(skylight);
            buffer.writeByteArray(extraData.array());
        }
        
        @Override
        public String toString() {
            return String.format(
                "(%s,%s,%s)",
                stringId, registryId, modDimensionName
            );
        }
    }
    
    public List<MyEntry> data;
    
    public StcDimensionInfo(List<MyEntry> data) {
        this.data = data;
    }
    
    public StcDimensionInfo(PacketBuffer buffer) {
        int num = buffer.readInt();
        data = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            data.add(new MyEntry(buffer));
        }
    }
    
    public void encode(PacketBuffer buffer) {
        buffer.writeInt(data.size());
        data.forEach(myEntry -> myEntry.encode(buffer));
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        Helper.log("Begin Handling");
        context.get().enqueueWork(() -> DimensionSyncManager.handleSyncData(data));
    }
    
    
}
