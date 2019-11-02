package com.immersive_portals.network;

import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.SGlobal;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CtsTeleport {
    DimensionType dimensionBefore;
    Vec3d posBefore;
    int portalEntityId;
    
    public CtsTeleport(DimensionType dimensionBefore, Vec3d posBefore, int portalEntityId) {
        this.dimensionBefore = dimensionBefore;
        this.posBefore = posBefore;
        this.portalEntityId = portalEntityId;
    }
    
    public CtsTeleport(PacketBuffer buf) {
        dimensionBefore = DimensionType.getById(buf.readInt());
        posBefore = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        portalEntityId = buf.readInt();
    }
    
    public void encode(PacketBuffer buf) {
        buf.writeInt(dimensionBefore.getId());
        buf.writeDouble(posBefore.x);
        buf.writeDouble(posBefore.y);
        buf.writeDouble(posBefore.z);
        buf.writeInt(portalEntityId);
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        ModMain.serverTaskList.addTask(() -> {
            SGlobal.serverTeleportationManager.onPlayerTeleportedInClient(
                (ServerPlayerEntity) context.get().getSender(),
                dimensionBefore,
                posBefore,
                portalEntityId
            );
        
            return true;
        });
        context.get().setPacketHandled(true);
    }
}
