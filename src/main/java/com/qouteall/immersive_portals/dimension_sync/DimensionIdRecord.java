package com.qouteall.immersive_portals.dimension_sync;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;

import java.util.stream.Collectors;

public class DimensionIdRecord {
    
    public static DimensionIdRecord clientRecord;
    
    public static DimensionIdRecord serverRecord;
    
    final BiMap<RegistryKey<World>, Integer> idMap;
    final BiMap<Integer, RegistryKey<World>> inverseMap;
    
    public DimensionIdRecord(BiMap<RegistryKey<World>, Integer> data) {
        idMap = data;
        inverseMap = data.inverse();
    }
    
    public RegistryKey<World> getDim(int integerId) {
        RegistryKey<World> result = inverseMap.get(integerId);
        if (result == null) {
            throw new RuntimeException(
                "Missing Dimension Integer Id " + integerId
            );
        }
        return result;
    }
    
    public int getIntId(RegistryKey<World> dim) {
        Integer result = idMap.get(dim);
        if (result == null) {
            throw new RuntimeException(
                "Missing DimensionId " + dim
            );
        }
        return result;
    }
    
    @Override
    public String toString() {
        return idMap.entrySet().stream().map(
            e -> e.getKey().func_240901_a_().toString() + " -> " + e.getValue()
        ).collect(Collectors.joining("\n"));
    }
    
    public static DimensionIdRecord tagToRecord(CompoundNBT tag) {
        CompoundNBT intids = tag.getCompound("intids");
        
        if (intids == null) {
            return null;
        }
        
        HashBiMap<RegistryKey<World>, Integer> bimap = HashBiMap.create();
        
        intids.keySet().forEach(dim -> {
            if (intids.contains(dim)) {
                int intid = intids.getInt(dim);
                bimap.put(DimId.idToKey(dim), intid);
            }
        });
        
        return new DimensionIdRecord(bimap);
    }
    
    public static CompoundNBT recordToTag(DimensionIdRecord record) {
        CompoundNBT intids = new CompoundNBT();
        record.idMap.forEach((key, intid) -> {
            intids.put(key.func_240901_a_().toString(), IntNBT.valueOf(intid));
        });
        
        CompoundNBT result = new CompoundNBT();
        result.put("intids", intids);
        return result;
    }
    
    
}
