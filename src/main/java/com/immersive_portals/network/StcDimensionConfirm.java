package com.immersive_portals.network;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.SGlobal;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class StcDimensionConfirm {
    DimensionType dimensionType;
    Vec3d pos;
    
    public StcDimensionConfirm(DimensionType dimensionType, Vec3d pos) {
        this.dimensionType = dimensionType;
        this.pos = pos;
    }
    
    public StcDimensionConfirm(PacketBuffer buf) {
        dimensionType = DimensionType.getById(buf.readInt());
        pos = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
    }
    
    public void encode(PacketBuffer buf) {
        buf.writeInt(dimensionType.getId());
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        Minecraft.getInstance().execute(() -> {
            CGlobal.clientTeleportationManager.acceptSynchronizationDataFromServer(
                dimensionType, pos,
                false
            );
        });
    }
}
