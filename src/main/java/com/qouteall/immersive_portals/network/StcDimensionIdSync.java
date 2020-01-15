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
//            DimensionRegistryManager.acceptSync(this.data);
//        });
//    }
    
    
}
