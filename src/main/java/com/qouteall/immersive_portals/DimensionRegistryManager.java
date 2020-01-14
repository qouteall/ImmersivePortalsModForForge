package com.qouteall.immersive_portals;

import com.mojang.datafixers.util.Pair;
import com.qouteall.immersive_portals.ducks.IEDimensionType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;

import java.util.List;
import java.util.stream.Collectors;

//Client and server may have different integer ids for the same dimension
//So we need to sync them
//And the integer id is based on the registry sequence
//So removing or adding a dimension mod will change the ids
public class DimensionRegistryManager {
    
    public static List<Pair<Integer, String>> getServerDimensionTypeId() {
        return Registry.DIMENSION_TYPE.stream().map(
            dimensionType -> new Pair<Integer, String>(
                dimensionType.getId(),
                dimensionType.getRegistryName().toString()
            )
        ).collect(Collectors.toList());
    }
    
    public static void acceptSync(List<Pair<Integer, String>> data) {
        data.forEach(pair -> {
            Integer rawId = pair.getFirst();
            String stringId = pair.getSecond();
            DimensionType dimensionType = Registry.DIMENSION_TYPE.getValue(
                new ResourceLocation(stringId)
            ).orElse(null);
            if (dimensionType == null) {
                Helper.err("Dimension Type Not Existed In Client!!!");
                Helper.err(stringId + " " + rawId);
            }
            else {
                ((IEDimensionType) dimensionType).setRawId(rawId);
                Helper.log("Sync " + dimensionType + " " + rawId + " " + stringId);
            }
        });
    }
}
