package com.qouteall.hiding_in_the_bushes.network;

import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.network.CommonNetworkClient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class StcSpawnEntity {
    String entityType;
    int entityId;
    RegistryKey<World> dimension;
    CompoundNBT tag;
    
    public StcSpawnEntity(
        String entityType,
        int entityId,
        RegistryKey<World> dimension,
        CompoundNBT tag
    ) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.dimension = dimension;
        this.tag = tag;
    }
    
    public StcSpawnEntity(PacketBuffer buf) {
        entityType = buf.readString();
        entityId = buf.readInt();
        dimension = DimId.readWorldId(buf, true);
        tag = buf.readCompoundTag();
    }
    
    public void encode(PacketBuffer buf) {
        buf.writeString(entityType);
        buf.writeInt(entityId);
        DimId.writeWorldId(buf, dimension, false);
        buf.writeCompoundTag(tag);
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(this::clientHandle);
        context.get().setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void clientHandle() {
        CommonNetworkClient.processEntitySpawn(
            entityType, this.entityId, dimension, tag
        );
    }
}
