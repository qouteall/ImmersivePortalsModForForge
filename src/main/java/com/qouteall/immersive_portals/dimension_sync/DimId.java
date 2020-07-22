package com.qouteall.immersive_portals.dimension_sync;

import com.qouteall.hiding_in_the_bushes.O_O;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class DimId {
    
    private static final boolean useIntegerId = true;
    
    public static void writeWorldId(
        PacketBuffer buf, RegistryKey<World> dimension, boolean isClient
    ) {
        if (useIntegerId) {
            DimensionIdRecord record = isClient ?
                DimensionIdRecord.clientRecord : DimensionIdRecord.serverRecord;
            int intId = record.getIntId(dimension);
            buf.writeInt(intId);
        }
        else {
            buf.writeResourceLocation(dimension.func_240901_a_());
        }
    }
    
    public static RegistryKey<World> readWorldId(PacketBuffer buf, boolean isClient) {
        if (isClient) {
            if (O_O.isDedicatedServer()) {
                throw new IllegalStateException("oops");
            }
        }
        
        if (useIntegerId) {
            DimensionIdRecord record = isClient ?
                DimensionIdRecord.clientRecord : DimensionIdRecord.serverRecord;
            int intId = buf.readInt();
            return record.getDim(intId);
        }
        else {
            ResourceLocation identifier = buf.readResourceLocation();
            return idToKey(identifier);
        }
    }
    
    public static RegistryKey<World> idToKey(ResourceLocation identifier) {
        return RegistryKey.func_240903_a_(Registry.field_239699_ae_, identifier);
    }
    
    public static RegistryKey<World> idToKey(String str) {
        return idToKey(new ResourceLocation(str));
    }
    
    public static void putWorldId(CompoundNBT tag, String tagName, RegistryKey<World> dim) {
        tag.putString(tagName, dim.func_240901_a_().toString());
    }
    
    public static RegistryKey<World> getWorldId(CompoundNBT tag, String tagName, boolean isClient) {
        INBT term = tag.get(tagName);
        if (term instanceof IntNBT) {
            int intId = ((IntNBT) term).getInt();
            DimensionIdRecord record = isClient ?
                DimensionIdRecord.clientRecord : DimensionIdRecord.serverRecord;
            return record.getDim(intId);
        }
        
        if (term instanceof StringNBT) {
            String id = ((StringNBT) term).getString();
            return idToKey(id);
        }
        
        throw new RuntimeException(
            "Invalid Dimension Record " + term
        );
    }
}
