package com.qouteall.immersive_portals;

//Client and server may have different integer ids for the same dimension
//So we need to sync them
public class DimensionRegistryManager {


//    public static List<Pair<Integer, String>> getServerDimensionTypeId() {
//        return Registry.DIMENSION_TYPE.stream().map(
//            dimensionType -> new Pair<Integer, String>(
//                dimensionType.getId(),
//                dimensionType.getRegistryName().toString()
//            )
//        ).collect(Collectors.toList());
//    }
//
//    public static void onPlayerLoggedIn(ServerPlayerEntity playerIn) {
//        NetworkMain.sendToPlayer(playerIn, StcDimensionIdSync.createPacket());
//    }
//
//    private static class DimensionInfo {
//        public DimensionType inGameObject;
//        public int integerId;
//        public ResourceLocation nameId;
//
//        public DimensionInfo(
//            DimensionType inGameObject,
//            int integerId,
//            ResourceLocation nameId
//        ) {
//            this.inGameObject = inGameObject;
//            this.integerId = integerId;
//            this.nameId = nameId;
//        }
//
//        public boolean isConsistent(ClearableRegistry<DimensionType> registry) {
//            return DimensionType.getById(integerId) == inGameObject &&
//                inGameObject.getId() == integerId &&
//                inGameObject.getRegistryName().equals(nameId);
//        }
//    }
//
//    public static void acceptSync(List<Pair<Integer, String>> data) {
//        Helper.log("Received dimension id sync data\n" + Helper.myToString(data.stream()));
//
//        ClearableRegistry<DimensionType> registry = (ClearableRegistry<DimensionType>) Registry.DIMENSION_TYPE;
//
//        List<DimensionInfo> dimInfos = data.stream().map(pair -> {
//            Integer rawId = pair.getFirst();
//            String stringId = pair.getSecond();
//
//            DimensionType dimensionType = registry.getValue(
//                new ResourceLocation(stringId)
//            ).orElse(null);
//            if (dimensionType == null) {
//                throw new IllegalStateException(
//                    "Dimension Type Not Existed In Client!!! " + stringId + " " + rawId
//                );
//            }
//            return new DimensionInfo(
//                dimensionType,
//                rawId,
//                new ResourceLocation(stringId)
//            );
//        }).collect(Collectors.toList());
//
//        boolean isAllConsistent = dimInfos.stream().allMatch(
//            info -> info.isConsistent(registry)
//        );
//
//        if (isAllConsistent) {
//            Helper.log("All Dimension Registry Info is Consistent");
//            return;
//        }
//
//        Helper.log("Started re-registering dimension ids");
//
//        registry.clear();
//
//        dimInfos.forEach(info -> {
//            registry.register(
//                //dimension integer id = registry id - 1
//                info.integerId + 1,
//                info.nameId,
//                info.inGameObject
//            );
//        });
//
//        Helper.log("Re-registered dimension ids");
//    }
}
