package com.qouteall.immersive_portals;

//Client and server may have different integer ids for the same dimension
//So we need to sync them
//And the integer id is based on the registry sequence
//So removing or adding a dimension mod will change the ids
public class DimensionRegistryManager {

//    public static List<Pair<Integer, String>> getServerDimensionTypeId() {
//        return Registry.DIMENSION_TYPE.stream().map(
//            dimensionType -> new Pair<Integer, String>(
//                dimensionType.getId(),
//                dimensionType.getRegistryName().toString()
//            )
//        ).collect(Collectors.toList());
//    }
}
