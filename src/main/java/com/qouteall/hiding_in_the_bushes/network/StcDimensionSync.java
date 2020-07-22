package com.qouteall.hiding_in_the_bushes.network;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.dimension_sync.DimensionIdRecord;
import com.qouteall.immersive_portals.dimension_sync.DimensionTypeSync;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class StcDimensionSync {
    
    private CompoundNBT idInfo;
    private CompoundNBT typeInfo;
    
    public StcDimensionSync(CompoundNBT idInfo, CompoundNBT typeInfo) {
        this.idInfo = idInfo;
        this.typeInfo = typeInfo;
    }
    
    public StcDimensionSync(PacketBuffer buf) {
        idInfo = buf.readCompoundTag();
        typeInfo = buf.readCompoundTag();
    }
    
    public void encode(PacketBuffer buf) {
        buf.writeCompoundTag(idInfo);
        buf.writeCompoundTag(typeInfo);
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(this::clientHandle);
        context.get().setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void clientHandle() {
        DimensionIdRecord.clientRecord = DimensionIdRecord.tagToRecord(idInfo);
        
        DimensionTypeSync.acceptTypeMapData(typeInfo);
        
        Helper.log("Received Dimension Int Id Sync");
        Helper.log("\n" + DimensionIdRecord.clientRecord);
    }
}
