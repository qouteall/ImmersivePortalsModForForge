package com.qouteall.hiding_in_the_bushes.network;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class StcUpdateGlobalPortals {
    private CompoundNBT data;
    private RegistryKey<World> dimensionType;
    
    public StcUpdateGlobalPortals(
        CompoundNBT data,
        RegistryKey<World> dimensionType
    ) {
        this.data = data;
        this.dimensionType = dimensionType;
    }
    
    public StcUpdateGlobalPortals(PacketBuffer buf) {
        dimensionType = DimId.readWorldId(buf, true);
        data = buf.readCompoundTag();
    }
    
    public void encode(PacketBuffer buf) {
        DimId.writeWorldId(buf,dimensionType,false);
        buf.writeCompoundTag(data);
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(this::clientHandle);
        context.get().setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void clientHandle() {
        GlobalPortalStorage.receiveGlobalPortalSync(dimensionType, data);
    }
}
