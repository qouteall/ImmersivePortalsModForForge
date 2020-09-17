package com.qouteall.hiding_in_the_bushes.network;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class StcDimensionConfirm {
    RegistryKey<World> dimensionType;
    Vector3d pos;
    
    public StcDimensionConfirm(RegistryKey<World> dimensionType, Vector3d pos) {
        this.dimensionType = dimensionType;
        this.pos = pos;
    }
    
    public StcDimensionConfirm(PacketBuffer buf) {
        dimensionType = DimId.readWorldId(buf,true);
        pos = new Vector3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
    }
    
    public void encode(PacketBuffer buf) {
        DimId.writeWorldId(buf,dimensionType,false);
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        clientHandle(context);
        context.get().setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void clientHandle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            CGlobal.clientTeleportationManager.acceptSynchronizationDataFromServer(
                dimensionType, pos,
                false
            );
        });
    }
}
