package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class StcUpdateGlobalPortals {
    private CompoundNBT data;
    private DimensionType dimensionType;
    
    public StcUpdateGlobalPortals(
        CompoundNBT data,
        DimensionType dimensionType
    ) {
        this.data = data;
        this.dimensionType = dimensionType;
    }
    
    public StcUpdateGlobalPortals(PacketBuffer buf) {
        dimensionType = DimensionType.getById(buf.readInt());
        data = buf.readCompoundTag();
    }
    
    public void encode(PacketBuffer buf) {
        buf.writeInt(dimensionType.getId());
        buf.writeCompoundTag(data);
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        Minecraft.getInstance().execute(() -> {
            ClientWorld world =
                CGlobal.clientWorldLoader.getOrCreateFakedWorld(dimensionType);
            
            List<GlobalTrackedPortal> portals =
                GlobalPortalStorage.getPortalsFromTag(data, world);
            
            ((IEClientWorld) world).setGlobalPortals(portals);
        });
        context.get().setPacketHandled(true);
    }
}
