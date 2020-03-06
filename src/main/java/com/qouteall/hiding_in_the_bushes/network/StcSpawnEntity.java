package com.qouteall.hiding_in_the_bushes.network;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class StcSpawnEntity {
    String entityType;
    int entityId;
    DimensionType dimension;
    CompoundNBT tag;
    
    public StcSpawnEntity(
        String entityType,
        int entityId,
        DimensionType dimension,
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
        int dimensionIdInt = buf.readInt();
        dimension = DimensionType.getById(dimensionIdInt);
        tag = buf.readCompoundTag();
        
        if (dimension == null) {
            Helper.err("Invalid dimension id for entity spawning " + dimensionIdInt);
        }
    }
    
    public void encode(PacketBuffer buf) {
        buf.writeString(entityType);
        buf.writeInt(entityId);
        buf.writeInt(dimension.getId());
        buf.writeCompoundTag(tag);
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(this::clientHandle);
        context.get().setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    private void clientHandle() {
        String entityTypeString = entityType;
        int entityId = this.entityId;
        
        Optional<EntityType<?>> entityType = EntityType.byKey(entityTypeString);
        if (!entityType.isPresent()) {
            Helper.err("unknown entity type " + entityTypeString);
            return;
        }
        
        ClientWorld world = CGlobal.clientWorldLoader.getOrCreateFakedWorld(dimension);
        
        if (world.getEntityByID(entityId) != null) {
            Helper.err(String.format(
                "duplicate entity %s %s %s",
                ((Integer) entityId).toString(),
                entityType.get().getTranslationKey(),
                tag
            ));
            return;
        }
    
        Entity entity = entityType.get().create(world);
        entity.read(tag);
        entity.setEntityId(entityId);
        entity.setPacketCoordinates(entity.getPosX(), entity.getPosY(), entity.getPosZ());
        world.addEntity(entityId, entity);
    
        if (entity instanceof Portal) {
            //do not create client world while rendering or gl states will be disturbed
            CGlobal.clientWorldLoader.getOrCreateFakedWorld(((Portal) entity).dimensionTo);
        }
    }
}
