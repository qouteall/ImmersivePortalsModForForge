package com.qouteall.immersive_portals.dimension_sync;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.server.IDynamicRegistries;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DimensionTypeSync {
    
    @OnlyIn(Dist.CLIENT)
    public static Map<RegistryKey<World>, RegistryKey<DimensionType>> clientTypeMap;
    
    @OnlyIn(Dist.CLIENT)
    private static IDynamicRegistries currentDimensionTypeTracker;
    
    @OnlyIn(Dist.CLIENT)
    public static void onGameJoinPacketReceived(IDynamicRegistries tracker) {
        currentDimensionTypeTracker = tracker;
    }
    
    @OnlyIn(Dist.CLIENT)
    private static Map<RegistryKey<World>, RegistryKey<DimensionType>> typeMapFromTag(CompoundNBT tag) {
        Map<RegistryKey<World>, RegistryKey<DimensionType>> result = new HashMap<>();
        tag.keySet().forEach(key -> {
            RegistryKey<World> worldKey = DimId.idToKey(key);
            
            String val = tag.getString(key);
            
            RegistryKey<DimensionType> typeKey =
                RegistryKey.func_240903_a_(Registry.field_239698_ad_, new ResourceLocation(val));
            
            result.put(worldKey, typeKey);
        });
        
        return result;
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void acceptTypeMapData(CompoundNBT tag) {
        clientTypeMap = typeMapFromTag(tag);
        
        Helper.log("Received Dimension Type Sync");
        Helper.log("\n" + Helper.myToString(
            clientTypeMap.entrySet().stream().map(
                e -> e.getKey().toString() + " -> " + e.getValue()
            )
        ));
    }
    
    public static CompoundNBT createTagFromServerWorldInfo() {
        return typeMapToTag(
            Streams.stream(McHelper.getServer().getWorlds()).collect(
                Collectors.toMap(World::func_234923_W_, World::func_234922_V_)
            )
        );
    }
    
    private static CompoundNBT typeMapToTag(Map<RegistryKey<World>, RegistryKey<DimensionType>> data) {
        CompoundNBT tag = new CompoundNBT();
        data.forEach((worldKey, typeKey) -> {
            tag.put(worldKey.func_240901_a_().toString(), StringNBT.valueOf(typeKey.func_240901_a_().toString()));
        });
        return tag;
    }
    
    @OnlyIn(Dist.CLIENT)
    public static RegistryKey<DimensionType> getDimensionTypeKey(RegistryKey<World> worldKey) {
        if (worldKey == World.field_234918_g_) {
            return DimensionType.field_235999_c_;
        }
        
        if (worldKey == World.field_234919_h_) {
            return DimensionType.field_236000_d_;
        }
        
        if (worldKey == World.field_234920_i_) {
            return DimensionType.field_236001_e_;
        }
        
        RegistryKey<DimensionType> obj = clientTypeMap.get(worldKey);
        
        if (obj == null) {
            Helper.err("Missing Dimension Type For " + worldKey);
            return DimensionType.field_235999_c_;
        }
        
        return obj;
    }
    
    @OnlyIn(Dist.CLIENT)
    public static DimensionType getDimensionType(RegistryKey<DimensionType> registryKey) {
        return currentDimensionTypeTracker.func_230520_a_().func_230516_a_(registryKey);
    }
    
}
