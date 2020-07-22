package com.qouteall.hiding_in_the_bushes.network;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CtsTeleport {
    RegistryKey<World> dimensionBefore;
    Vector3d posBefore;
    UUID portalEntityId;
    
    public CtsTeleport(RegistryKey<World> dimensionBefore, Vector3d posBefore, UUID portalEntityId) {
        this.dimensionBefore = dimensionBefore;
        this.posBefore = posBefore;
        this.portalEntityId = portalEntityId;
    }
    
    public CtsTeleport(PacketBuffer buf) {
        dimensionBefore = DimId.readWorldId(buf, false);
        posBefore = new Vector3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        portalEntityId = buf.readUniqueId();
    }
    
    public void encode(PacketBuffer buf) {
        DimId.writeWorldId(buf, dimensionBefore, true);
        buf.writeDouble(posBefore.x);
        buf.writeDouble(posBefore.y);
        buf.writeDouble(posBefore.z);
        buf.writeUniqueId(portalEntityId);
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Global.serverTeleportationManager.onPlayerTeleportedInClient(
                (ServerPlayerEntity) context.get().getSender(),
                dimensionBefore,
                posBefore,
                portalEntityId
            );
        });
        context.get().setPacketHandled(true);
    }
}
