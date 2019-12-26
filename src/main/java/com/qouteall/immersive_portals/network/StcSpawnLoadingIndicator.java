package com.qouteall.immersive_portals.network;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class StcSpawnLoadingIndicator {
    DimensionType dimensionType;
    Vec3d pos;
    
    public StcSpawnLoadingIndicator(DimensionType dimensionType, Vec3d pos) {
        this.dimensionType = dimensionType;
        this.pos = pos;
    }
    
    public StcSpawnLoadingIndicator(PacketBuffer buf) {
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
        clientOnlyHandle();
        
        context.get().setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void clientOnlyHandle() {
        Minecraft.getInstance().execute(() -> {
            ClientWorld world = CGlobal.clientWorldLoader.getWorld(dimensionType);
            if (world == null) {
                return;
            }
            
            LoadingIndicatorEntity indicator = LoadingIndicatorEntity.entityType.create(world);
            indicator.setPosition(pos.x, pos.y, pos.z);
            
            world.addEntity(233333333, indicator);
        });
    }
}
