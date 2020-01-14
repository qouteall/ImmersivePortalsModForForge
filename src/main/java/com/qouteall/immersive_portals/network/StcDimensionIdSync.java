package com.qouteall.immersive_portals.network;

public class StcDimensionIdSync {
//    public List<Pair<Integer, String>> data;
//
//    public StcDimensionIdSync(List<Pair<Integer, String>> data) {
//        this.data = data;
//    }
//
//    public StcDimensionIdSync(PacketBuffer buf) {
//        data = new ArrayList<>();
//
//        int num = buf.readInt();
//        for (int i = 0; i < num; i++) {
//            int rawId = buf.readInt();
//            String stringId = buf.readString();
//            data.add(new Pair<>(rawId, stringId));
//        }
//    }
//
//    public void encode(PacketBuffer buf) {
//        buf.writeInt(data.size());
//
//        for (Pair<Integer, String> pair : data) {
//            buf.writeInt(pair.getFirst());
//            buf.writeString(pair.getSecond());
//        }
//    }
//
//    public static StcDimensionIdSync createPacket() {
//        List<Pair<Integer, String>> data = DimensionRegistryManager.getServerDimensionTypeId();
//        return new StcDimensionIdSync(data);
//    }
//
//    public void handle(Supplier<NetworkEvent.Context> context) {
//        clientOnlyHandle();
//        context.get().setPacketHandled(true);
//    }
//
//    @OnlyIn(Dist.CLIENT)
//    private void clientOnlyHandle() {
//        Minecraft.getInstance().execute(() -> {
//            data.forEach(pair -> {
//                Integer rawId = pair.getFirst();
//                String stringId = pair.getSecond();
//                DimensionType dimensionType = Registry.DIMENSION_TYPE.getValue(
//                    new ResourceLocation(stringId)
//                ).orElse(null);
//                if (dimensionType == null) {
//                    Helper.err("Dimension Type Not Existed In Client!!!");
//                    Helper.err(stringId + " " + rawId);
//                }
//                else {
//                    ((IEDimensionType) dimensionType).setRawId(rawId);
//                    Helper.log("Sync " + dimensionType +" "+ rawId + " " + stringId);
//                }
//            });
//        });
//    }
    
    
}
